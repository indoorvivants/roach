package roach

import scala.scalanative.unsafe.*
import scala.annotation.targetName
import scala.util.NotGiven
import scala.deriving.Mirror
import java.util.UUID
import libpq.types.Oid

object codecs:
  import Codec.*
  import scala.scalanative.unsigned.*

  val int2 = stringLike[Short]("int2")(_.toShort)
  val int4 = stringLike[Int]("int4")(_.toInt)
  val int8 = stringLike[Long]("int8")(_.toLong)
  val float4 = stringLike[Float]("float4")(_.toFloat)
  val float8 = stringLike[Double]("float8")(_.toDouble)
  val uuid = stringLike[UUID]("uuid")(UUID.fromString(_))
  val bool = stringLike[Boolean]("bool")(_ == "t")
  val char = stringLike[Char]("char")(_(0).toChar)
  val name = textual("name")
  val varchar = textual("varchar")
  val bpchar = textual("bpchar")
  val text = textual("text")
  val oid =
    int4.bimap[Oid](i => Oid(i.toUInt), _.asInstanceOf[CUnsignedInt].toInt)

  inline def bpchar(n: Int)          = textual(s"bpchar($n)")
  inline def varchar(n: Int)         = textual(s"varchar($n)")

  private def textual(nm: String) = stringLike[String](nm)(identity)
end codecs
