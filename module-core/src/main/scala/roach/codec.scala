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
  def decode(get: Int => CString, isNull: Int => Boolean)(using Zone): T
  def encode(value: T): Int => Zone ?=> CString

  def bimap[B](f: T => B, g: B => T): Codec[B] =
    new Codec[B]:
      def accepts(offset: Int) = self.accepts(offset)
      def length = self.length
      def decode(get: Int => CString, isNull: Int => Boolean)(using Zone) =
        f(self.decode(get, isNull))
      def encode(value: B) =
        self.encode(g(value))
end Codec

trait ValueCodec[T] extends Codec[T]:
  self =>
  def opt: Codec[Option[T]] = new Codec[Option[T]]:
    override def accepts(idx: Int): String = self.accepts(idx)
    override def length: Int = self.length
    def decode(get: Int => CString, isNull: Int => Boolean)(using Zone) =
      Option.when(!isNull(0))(self.decode(get, isNull))

    override def encode(value: Option[T]): Int => (Zone) ?=> CString =
      value match
        case None    => _ => null
        case Some(v) => self.encode(v)

end ValueCodec

private[roach] class AppendCodec[A <: Tuple, B](a: Codec[A], b: Codec[B])
    extends Codec[Tuple.Concat[A, (B *: EmptyTuple)]]:
  type T = Tuple.Concat[A, (B *: EmptyTuple)]
  def accepts(offset: Int) =
    if offset < a.length then a.accepts(offset)
    else b.accepts(offset - a.length)

  def length = a.length + b.length

  def decode(get: Int => CString, isNull: Int => Boolean)(using Zone): T =
    val left = a.decode(get, isNull)
    val right = b.decode(
      (i: Int) => get(i + a.length),
      (i: Int) => isNull(i + a.length)
    )
    left ++ (right *: EmptyTuple)

  def encode(value: T) =
    val (left, right) = value.splitAt(a.length).asInstanceOf[(A, Tuple1[B])]
    val leftEncode = a.encode(left)
    val rightEncode = b.encode(right._1)

    (offset: Int) =>
      if offset + 1 > a.length then rightEncode(offset - a.length)
      else leftEncode(offset)

  override def toString() =
    s"AppendCodec[$a, $b]"

end AppendCodec

private[roach] class CombineCodec[A, B](a: Codec[A], b: Codec[B])
    extends Codec[(A, B)]:
  type T = (A, B)
  def accepts(offset: Int) =
    if offset < a.length then a.accepts(offset)
    else b.accepts(offset - a.length)

  def length = a.length + b.length

  def decode(get: Int => CString, isNull: Int => Boolean)(using Zone): T =
    val left = a.decode(get, isNull)
    val right =
      b.decode((i: Int) => get(i + a.length), (i: Int) => isNull(i + a.length))
    (left, right)

  def encode(value: T) =
    val leftEncode = a.encode(value._1)
    val rightEncode = b.encode(value._2)

    (offset: Int) =>
      if offset + 1 > a.length then rightEncode(offset - a.length)
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
        def decode(get: Int => CString, isNull: Int => Boolean)(using Zone) =
          iso.convert(d.decode(get, isNull))

        def encode(value: T) =
          d.encode(iso.invert(value))

  extension [A](d: Codec[A])
    inline def ~[B](
        other: Codec[B]
    )(using NotGiven[B <:< Tuple]): Codec[(A, B)] =
      CombineCodec(d, other)
  end extension

  def stringLike[A](
      accept: String
  )(f: String => A, g: A => String = (_: A).toString): ValueCodec[A] =
    new ValueCodec[A]:
      inline def length: Int = 1
      inline def accepts(offset: Int) = accept

      def decode(get: Int => CString, isNull: Int => Boolean)(using Zone) =
        f(fromCString(get(0)))

      def encode(value: A) =
        _ => toCString(g(value))

      override def toString() = s"ValueCodec[$accept]"

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
