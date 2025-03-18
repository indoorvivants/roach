package roach

import scalanative.unsafe.*
import libpq.all.*
import scala.util.Using.Releasable
import java.util.concurrent.Semaphore

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

    val pool =
      if scalanative.meta.LinktimeInfo.isMultithreadingEnabled then
        new MultiThreadedSlotPool(
          Slot.Empty,
          toCString(connString),
          noticeProcessor
        )
      else
        new SingleThreadedSlotPool(
          Slot.Empty,
          toCString(connString),
          noticeProcessor
        )

    try
      f(pool)
    finally
      pool.shutdown()
  end single

  given Releasable[Pool] = _.shutdown()

end Pool
