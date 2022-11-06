package roach

import libpq.functions.*
import libpq.types.*

import scala.scalanative.unsigned.*
import scala.scalanative.unsafe.*
import scala.scalanative.libc.*

class Prepared[T] private[roach] (
    db: Database,
    codec: Codec[T],
    statementName: String
):
  private val nParams = codec.length
  def execute(data: T)(using z: Zone): Validated[Result] =
    db.checkConnection()
    val paramValues = stackalloc[CString](nParams)
    val encoder = codec.encode(data)
    for i <- 0 until nParams do paramValues(i) = encoder(i)
    db.executePrepared(statementName, nParams, ParamValues(paramValues))
  end execute
end Prepared
