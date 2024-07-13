package roach

import scalanative.unsafe.*
import libpq.all.*
import scala.util.Using.Releasable
import java.util.concurrent.Semaphore

private[roach] class MultiThreadedSlotPool(
    var slot: Slot,
    connString: CString,
    noticeProcessor: String => Unit
) extends Pool:

  var isShutdown = false

  val sem = new Semaphore(1)

  val (captured, memory) = Captured.unsafe(noticeProcessor)

  def shutdown(): Unit =
    try
      sem.acquire()
      if isShutdown then
        RoachError
          .PoolConsistencyError(
            "Attempting to shutdown an already terminated pool!"
          )
          .raise
      else
        memory.deallocate()
        isShutdown = true

    finally sem.release()
  end shutdown

  def reconnect(): Validated[Database] =
    Database.apply(connString).map { db =>
      db.unsafely { conn =>
        val proc = PQsetNoticeProcessor(
          conn,
          PQnoticeProcessor { (arg: Ptr[Byte], msg: CString) =>
            val handler = arg.asInstanceOf[Ptr[String => Unit]]
            Zone(
              (!handler).apply(fromCString(msg))
            )
          },
          captured.asInstanceOf[Ptr[Byte]]
        )
      }

      db
    }
  end reconnect

  override def lease[A](f: Database => A): A =

    var released: Boolean = false

    inline def retry() =
      released = true
      sem.release()
      lease(f)

    try
      sem.acquire()

      slot match
        case Slot.Available(db) =>
          if !db.connectionIsOkay then
            slot = Slot.Empty
            retry()
          else
            try f(db)
            catch
              case rfe: RoachError.ConnectionIsDown =>
                slot = Slot.Empty
                retry()

          end if

        case Slot.Empty =>
          slot = Slot.Available(reconnect().getOrThrow)
          retry()

        case Slot.Busy => ???
      end match
    finally if !released then sem.release()
    end try

  end lease
end MultiThreadedSlotPool
