package roach

import scalanative.unsafe.*

private[roach] enum Slot:
  case Available(db: Database)
  case Empty, Busy

trait Pool:
  def lease[A](f: Database => A): A

  def withLease[A](f: Database ?=> A): A = lease(db => f(using db))

object Pool:
  def single[A](connString: String)(f: Pool => A)(using Zone) =
    val pool = Single(toCString(connString))
    f(pool)

private[roach] class Single private (var slot: Slot, connString: CString)
    extends Pool:

  def reconnect(): Validated[Database] =
    Database(connString)

  def lease[A](f: Database => A): A =
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
            "Single slot cannot be busy in a single threaded Scala Native environment." + " This suggests the library implementation is wrong"
          )
          .raise

end Single

object Single:
  def apply(connString: CString): Pool = new Single(Slot.Empty, connString)
