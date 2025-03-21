package roach

import libpq.types.Oid
import scala.scalanative.unsigned.*
import scala.collection.immutable.IntMap
import java.util as ju

trait OidMapping:
  self =>
  def map(c: String): Oid
  def rev(oid: Oid): String

  def extend(other: OidMapping) =
    new OidMapping:
      override def map(c: String): Oid =
        try other.map(c)
        catch case _: ju.NoSuchElementException => self.map(c)

      override def rev(oid: Oid): String =
        try other.rev(oid)
        catch case _: ju.NoSuchElementException => self.rev(oid)
end OidMapping

object OidMapping extends OidMapping:
  private val reverseMapping = Map(
    "_aclitem" -> 1034,
    "_bit" -> 1561,
    "_bool" -> 1000,
    "_box" -> 1020,
    "_bpchar" -> 1014,
    "_bytea" -> 1001,
    "_char" -> 1002,
    "_cid" -> 1012,
    "_cidr" -> 651,
    "_circle" -> 719,
    "_cstring" -> 1263,
    "_date" -> 1182,
    "_daterange" -> 3913,
    "_float4" -> 1021,
    "_float8" -> 1022,
    "_gtsvector" -> 3644,
    "_inet" -> 1041,
    "_int2" -> 1005,
    "_int2vector" -> 1006,
    "_int4" -> 1007,
    "_int4range" -> 3905,
    "_int8" -> 1016,
    "_int8range" -> 3927,
    "_interval" -> 1187,
    "_json" -> 199,
    "_jsonb" -> 3807,
    "_jsonpath" -> 4073,
    "_line" -> 629,
    "_lseg" -> 1018,
    "_macaddr" -> 1040,
    "_macaddr8" -> 775,
    "_money" -> 791,
    "_name" -> 1003,
    "_numeric" -> 1231,
    "_numrange" -> 3907,
    "_oid" -> 1028,
    "_oidvector" -> 1013,
    "_path" -> 1019,
    "_pg_attribute" -> 270,
    "_pg_class" -> 273,
    "_pg_lsn" -> 3221,
    "_pg_proc" -> 272,
    "_pg_type" -> 210,
    "_point" -> 1017,
    "_polygon" -> 1027,
    "_record" -> 2287,
    "_refcursor" -> 2201,
    "_regclass" -> 2210,
    "_regcollation" -> 4192,
    "_regconfig" -> 3735,
    "_regdictionary" -> 3770,
    "_regnamespace" -> 4090,
    "_regoper" -> 2208,
    "_regoperator" -> 2209,
    "_regproc" -> 1008,
    "_regprocedure" -> 2207,
    "_regrole" -> 4097,
    "_regtype" -> 2211,
    "_text" -> 1009,
    "_tid" -> 1010,
    "_time" -> 1183,
    "_timestamp" -> 1115,
    "_timestamptz" -> 1185,
    "_timetz" -> 1270,
    "_tsquery" -> 3645,
    "_tsrange" -> 3909,
    "_tstzrange" -> 3911,
    "_tsvector" -> 3643,
    "_txid_snapshot" -> 2949,
    "_uuid" -> 2951,
    "_varbit" -> 1563,
    "_varchar" -> 1015,
    "_xid" -> 1011,
    "_xid8" -> 271,
    "_xml" -> 143,
    "aclitem" -> 1033,
    "any" -> 2276,
    "anyarray" -> 2277,
    "anycompatiblemultirange" -> 4538,
    "anyelement" -> 2283,
    "anyenum" -> 3500,
    "anymultirange" -> 4537,
    "anynonarray" -> 2776,
    "anyrange" -> 3831,
    "bit" -> 1560,
    "bool" -> 16,
    "box" -> 603,
    "bpchar" -> 1042,
    "bytea" -> 17,
    "char" -> 18,
    "cid" -> 29,
    "cidr" -> 650,
    "circle" -> 718,
    "cstring" -> 2275,
    "date" -> 1082,
    "datemultirange" -> 4535,
    "daterange" -> 3912,
    "event_trigger" -> 3838,
    "fdw_handler" -> 3115,
    "float4" -> 700,
    "float8" -> 701,
    "gtsvector" -> 3642,
    "index_am_handler" -> 325,
    "inet" -> 869,
    "int2" -> 21,
    "int2vector" -> 22,
    "int4" -> 23,
    "int4multirange" -> 4451,
    "int4range" -> 3904,
    "int8" -> 20,
    "int8multirange" -> 4536,
    "int8range" -> 3926,
    "internal" -> 2281,
    "interval" -> 1186,
    "json" -> 114,
    "jsonb" -> 3802,
    "jsonpath" -> 4072,
    "language_handler" -> 2280,
    "line" -> 628,
    "lseg" -> 601,
    "macaddr" -> 829,
    "macaddr8" -> 774,
    "money" -> 790,
    "name" -> 19,
    "numeric" -> 1700,
    "nummultirange" -> 4532,
    "numrange" -> 3906,
    "oid" -> 26,
    "oidvector" -> 30,
    "path" -> 602,
    "point" -> 600,
    "polygon" -> 604,
    "record" -> 2249,
    "refcursor" -> 1790,
    "regclass" -> 2205,
    "regcollation" -> 4191,
    "regconfig" -> 3734,
    "regdictionary" -> 3769,
    "regnamespace" -> 4089,
    "regoper" -> 2203,
    "regoperator" -> 2204,
    "regproc" -> 24,
    "regprocedure" -> 2202,
    "regrole" -> 4096,
    "regtype" -> 2206,
    "table_am_handler" -> 269,
    "text" -> 25,
    "tid" -> 27,
    "time" -> 1083,
    "timestamp" -> 1114,
    "timestamptz" -> 1184,
    "timetz" -> 1266,
    "trigger" -> 2279,
    "tsm_handler" -> 3310,
    "tsmultirange" -> 4533,
    "tsquery" -> 3615,
    "tsrange" -> 3908,
    "tstzmultirange" -> 4534,
    "tstzrange" -> 3910,
    "tsvector" -> 3614,
    "txid_snapshot" -> 2970,
    "unknown" -> 705,
    "uuid" -> 2950,
    "varbit" -> 1562,
    "varchar" -> 1043,
    "void" -> 2278,
    "xid" -> 28,
    "xml" -> 142
  )

  private val mapping =
    IntMap.from(reverseMapping.map(_.swap))

  inline override def map(c: String): Oid =
    Oid(reverseMapping(c).toUInt)

  inline override def rev(oid: Oid): String = mapping(oid.value.toInt)

end OidMapping
