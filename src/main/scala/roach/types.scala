package roach

import libpq.types.Oid
import scala.scalanative.unsigned.*

trait OidMapping:
  def map(c: String): Oid
  def rev(oid: Oid): String

object OidMapping extends OidMapping:
  private val mapping =
    Map(
      21 -> "int2",
      23 -> "int4",
      20 -> "int8",
      700 -> "float4",
      701 -> "float8",
      26 -> "oid",
      25 -> "text",
      1042 -> "bpchar",
      1043 -> "varchar",
      18 -> "char",
      2950 -> "uuid",
      19 -> "name",
      16 -> "bool"
    ).map((k, v) => Oid(k.toUInt) -> v)

  private val reverse = mapping.map(_.swap)

  inline override def map(c: String): Oid =
    reverse(c)

  inline override def rev(oid: Oid): String = mapping(oid)

end OidMapping

