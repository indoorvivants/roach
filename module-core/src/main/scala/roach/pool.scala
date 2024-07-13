package roach

import scalanative.unsafe.*
import libpq.all.*
import scala.util.Using.Releasable

private[roach] enum Slot:
  case Available(db: Database)
  case Empty, Busy

trait Pool:
  def lease[A](f: Database => A): A
  def withLease[A](f: Database ?=> A): A = lease(db => f(using db))
  def shutdown(): Unit

object Pool:
  def single[A](connString: String, noticeProcessor: String => Unit = _ => ())(
      f: Pool => A
  )(using Zone): A =
    val pool = Single(toCString(connString), noticeProcessor)
    try
      f(pool)
    finally
      pool.shutdown()

  given Releasable[Pool] = _.shutdown()

end Pool

private[roach] class Single private (
    var slot: Slot,
    connString: CString,
    noticeProcessor: String => Unit
) extends Pool:

  var isShutdown = false

  val (captured, memory) = Captured.unsafe(noticeProcessor)

  def shutdown(): Unit =
    if isShutdown then
      RoachError
        .PoolConsistencyError(
          "Attempting to shutdown an already terminated pool!"
        )
        .raise
    memory.deallocate()
    synchronized { isShutdown = true }
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

  def lease[A](f: Database => A): A =
    if scalanative.meta.LinktimeInfo.isMultithreadingEnabled then
      slot.synchronized:
        singleThreadedLogic(f) // YOLO
    else singleThreadedLogic(f)
  end lease

  private inline def singleThreadedLogic[A](f: Database => A) =
    slot match
      case Slot.Available(db) =>
        var result: A | Null = null
        if db.connectionIsOkay then
          try
            slot = Slot.Busy
            result = f(db)
          catch
            case rfe: RoachError.ConnectionIsDown =>
              slot = Slot.Empty
              throw rfe
          finally if slot != Slot.Empty then slot = Slot.Available(db)
          result.nn
        else
          slot = Slot.Available(reconnect().getOrThrow)
          lease(f)
        end if

      case Slot.Empty =>
        slot = Slot.Available(reconnect().getOrThrow)
        lease(f)

      case Slot.Busy =>
        RoachError
          .PoolConsistencyError(
            "Single slot cannot be busy in a single threaded Scala Native environment." +
              " This suggests the library implementation is wrong"
          )
          .raise
    end match
  end singleThreadedLogic

end Single

object Single:
  def apply(
      connString: CString,
      noticeProcessor: String => Unit = _ => ()
  ): Pool = new Single(Slot.Empty, connString, noticeProcessor)
