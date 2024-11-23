package roach

import scala.scalanative.unsafe.CString

import scala.ContextFunction1

import scala.scalanative.unsafe.Zone

class LabelledCodec[L <: Singleton & String, T](lab: L, c: Codec[T]) extends Codec[(L, T)]:
  override def length: Int = c.length

  override def decode(get: Int => CString, isNull: Int => Boolean)(using
      Zone
  ): (L, T) =
    lab -> c.decode(get, isNull)

  override def encode(value: (L, T)): Int => (Zone) ?=> CString =
    c.encode(value._2)

  export c.accepts

  override def toString(): String = s"LabelledCodec[$lab, $c]"
end LabelledCodec

extension [T](c: Codec[T])
  inline def label[L <: Singleton & String]: LabelledCodec[L, T] =
    LabelledCodec(compiletime.constValue[L], c)
