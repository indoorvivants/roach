package roach

enum RoachError(val message: String, val cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull):
  case ConnectionIsDown(msg: String)
      extends RoachError(s"Postgres connection is down: $msg")

  case MigrationAbortedWithReason(applied: Vector[String], reason: Throwable)
      extends RoachError(
        s"Migration was aborted due to a failure, " + s"the following migrations will be rolled back: `${applied
            .mkString(", ")}`",
        Some(reason)
      )
  case PoolConsistencyError(msg: String) extends RoachError(msg)

  case QueryExecutionFailed(msg: String, result: Option[SQLSTATE])
      extends RoachError(s"Query failed: $msg, SQLSTATE = ${result}")

  case CodecQueryLengthMismatch(codecLength: Int, nFields: Int)
      extends RoachError(
        s"Provided codec is for $codecLength fields, while the result has $nFields fields"
      )

  case CodecFieldTypeMismatch(
      offset: Int,
      fieldName: String,
      expectedType: String,
      accepts: String
  ) extends RoachError(
        s"$offset: Field $fieldName is of type '$expectedType', " +
          s"but the decoder only accepts '$accepts'"
      )

  case MigrationStateInconsistent(msg: String, state: String)
      extends RoachError(msg + "\n" + state)

  inline def raise: Nothing = throw this
end RoachError

object RoachError:
  object MigrationStateInconsistent:
    def databaseIsAhead(state: String) = RoachError.MigrationStateInconsistent(
      s"Migration state is inconsistent (database is AHEAD and contains migrations NOT passed to Migrate)",
      state
    )

    def wrongPrefix(state: String) =
      apply(s"Migration state is inconsistent!", state)
end RoachError
