package roach

import scala.scalanative.unsafe.*
import scala.annotation.targetName
import scala.util.NotGiven
import scala.deriving.Mirror
import java.util.UUID
import libpq.types.Oid

trait Codec[T]:
  self =>
  def accepts(idx: Int): String
  def length: Int
  def decode(get: Int => CString)(using Zone): T
  def encode(value: T): Int => Zone ?=> CString
  def bimap[B](f: T => B, g: B => T): Codec[B] =
    new Codec[B]:
      def accepts(offset: Int) = self.accepts(offset)
      def length = self.length
      def decode(get: Int => CString)(using Zone) =
        f(self.decode(get))
      def encode(value: B) =
        self.encode(g(value))
end Codec

private[roach] class AppendCodec[A <: Tuple, B](a: Codec[A], b: Codec[B])
    extends Codec[Tuple.Concat[A, (B *: EmptyTuple)]]:
  type T = Tuple.Concat[A, (B *: EmptyTuple)]
  def accepts(offset: Int) =
    if (offset < a.length) then a.accepts(offset)
    else b.accepts(offset - a.length)

  def length = a.length + b.length

  def decode(get: Int => CString)(using Zone): T =
    val left = a.decode(get)
    val right = b.decode((i: Int) => get(i + a.length))
    left ++ (right *: EmptyTuple)

  def encode(value: T) =
    val (left, right) = value.splitAt(a.length).asInstanceOf[(A, Tuple1[B])]
    val leftEncode = a.encode(left)
    val rightEncode = b.encode(right._1)

    (offset: Int) =>
      if (offset + 1 > a.length) then rightEncode(offset - a.length)
      else leftEncode(offset)

  override def toString() =
    s"AppendCodec[$a, $b]"

end AppendCodec

private[roach] class CombineCodec[A, B](a: Codec[A], b: Codec[B])
    extends Codec[(A, B)]:
  type T = (A, B)
  def accepts(offset: Int) =
    if (offset < a.length) then a.accepts(offset)
    else b.accepts(offset - a.length)

  def length = a.length + b.length

  def decode(get: Int => CString)(using Zone): T =
    val left = a.decode(get)
    val right = b.decode((i: Int) => get(i + a.length))
    (left, right)

  def encode(value: T) =
    val leftEncode = a.encode(value._1)
    val rightEncode = b.encode(value._2)

    (offset: Int) =>
      if (offset + 1 > a.length) then rightEncode(offset - a.length)
      else leftEncode(offset)

  override def toString() =
    s"CombineCodec[$a, $b]"
end CombineCodec

object Codec:
  extension [A <: Tuple](d: Codec[A])
    inline def ~[B](
        other: Codec[B]
    ): Codec[Tuple.Concat[A, B *: EmptyTuple]] =
      AppendCodec(d, other)
  end extension

  extension [A](d: Codec[A])
    inline def as[T](using iso: Iso[A, T]) =
      new Codec[T]:
        def accepts(offset: Int) =
          d.accepts(offset)
        def length = d.length
        def decode(get: Int => CString)(using Zone) =
          iso.convert(d.decode(get))

        def encode(value: T) =
          d.encode(iso.invert(value))

  extension [A](d: Codec[A])
    inline def ~[B](
        other: Codec[B]
    )(using NotGiven[B <:< Tuple]): Codec[(A, B)] =
      CombineCodec(d, other)
  end extension

  def stringLike[A](accept: String)(f: String => A): Codec[A] =
    new Codec[A]:
      inline def length: Int = 1
      inline def accepts(offset: Int) = accept

      def decode(get: Int => CString)(using Zone) =
        f(fromCString(get(0)))

      def encode(value: A) =
        _ => toCString(value.toString)

      override def toString() = s"Decode[$accept]"

end Codec

trait Iso[A, B]:
  def convert(a: A): B
  def invert(b: B): A

object Iso:
  given [X <: Tuple, A](using
      mir: Mirror.ProductOf[A] { type MirroredElemTypes = X }
  ): Iso[X, A] with
    def convert(a: X) =
      mir.fromProduct(a)
    def invert(a: A) =
      Tuple.fromProduct(a.asInstanceOf[Product]).asInstanceOf[X]

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

  private def textual(nm: String) = stringLike[String](nm)(identity)
end codecs

