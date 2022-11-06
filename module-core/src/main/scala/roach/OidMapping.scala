package roach

import libpq.types.Oid
import scala.scalanative.unsigned.*
import scala.collection.immutable.IntMap
import java.{util as ju}

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
  private val mapping =
    IntMap(
      1033 -> "aclitem",
      13427 -> "administrable_role_authorizations",
      2276 -> "any",
      2277 -> "anyarray",
      5077 -> "anycompatible",
      5078 -> "anycompatiblearray",
      4538 -> "anycompatiblemultirange",
      5079 -> "anycompatiblenonarray",
      5080 -> "anycompatiblerange",
      2283 -> "anyelement",
      3500 -> "anyenum",
      4537 -> "anymultirange",
      2776 -> "anynonarray",
      3831 -> "anyrange",
      13422 -> "applicable_roles",
      13431 -> "attributes",
      1560 -> "bit",
      16 -> "bool",
      603 -> "box",
      1042 -> "bpchar",
      17 -> "bytea",
      13405 -> "cardinal_number",
      18 -> "char",
      13408 -> "character_data",
      13436 -> "character_sets",
      13441 -> "check_constraint_routine_usage",
      13446 -> "check_constraints",
      29 -> "cid",
      650 -> "cidr",
      718 -> "circle",
      13456 -> "collation_character_set_applicability",
      13451 -> "collations",
      13461 -> "column_column_usage",
      13466 -> "column_domain_usage",
      13689 -> "column_options",
      13471 -> "column_privileges",
      13476 -> "column_udt_usage",
      13481 -> "columns",
      13486 -> "constraint_column_usage",
      13491 -> "constraint_table_usage",
      2275 -> "cstring",
      13674 -> "data_type_privileges",
      1082 -> "date",
      4535 -> "datemultirange",
      3912 -> "daterange",
      13496 -> "domain_constraints",
      13501 -> "domain_udt_usage",
      13506 -> "domains",
      13679 -> "element_types",
      13511 -> "enabled_roles",
      3838 -> "event_trigger",
      3115 -> "fdw_handler",
      700 -> "float4",
      701 -> "float8",
      13697 -> "foreign_data_wrapper_options",
      13701 -> "foreign_data_wrappers",
      13710 -> "foreign_server_options",
      13714 -> "foreign_servers",
      13723 -> "foreign_table_options",
      13727 -> "foreign_tables",
      3642 -> "gtsvector",
      325 -> "index_am_handler",
      869 -> "inet",
      13413 -> "information_schema_catalog_name",
      21 -> "int2",
      22 -> "int2vector",
      23 -> "int4",
      4451 -> "int4multirange",
      3904 -> "int4range",
      20 -> "int8",
      4536 -> "int8multirange",
      3926 -> "int8range",
      2281 -> "internal",
      1186 -> "interval",
      114 -> "json",
      3802 -> "jsonb",
      4072 -> "jsonpath",
      13515 -> "key_column_usage",
      2280 -> "language_handler",
      628 -> "line",
      601 -> "lseg",
      829 -> "macaddr",
      774 -> "macaddr8",
      790 -> "money",
      19 -> "name",
      1700 -> "numeric",
      4532 -> "nummultirange",
      3906 -> "numrange",
      26 -> "oid",
      30 -> "oidvector",
      13520 -> "parameters",
      602 -> "path",
      600 -> "point",
      604 -> "polygon",
      2249 -> "record",
      1790 -> "refcursor",
      13525 -> "referential_constraints",
      2205 -> "regclass",
      4191 -> "regcollation",
      3734 -> "regconfig",
      3769 -> "regdictionary",
      4089 -> "regnamespace",
      2203 -> "regoper",
      2204 -> "regoperator",
      24 -> "regproc",
      2202 -> "regprocedure",
      4096 -> "regrole",
      2206 -> "regtype",
      16387 -> "roach_test_circe_2282029329189676863",
      13530 -> "role_column_grants",
      13544 -> "role_routine_grants",
      13607 -> "role_table_grants",
      13636 -> "role_udt_grants",
      13645 -> "role_usage_grants",
      13534 -> "routine_column_usage",
      13539 -> "routine_privileges",
      13548 -> "routine_routine_usage",
      13553 -> "routine_sequence_usage",
      13558 -> "routine_table_usage",
      13563 -> "routines",
      13568 -> "schemata",
      13572 -> "sequences",
      13577 -> "sql_features",
      13410 -> "sql_identifier",
      13582 -> "sql_implementation_info",
      13587 -> "sql_parts",
      13592 -> "sql_sizing",
      269 -> "table_am_handler",
      13597 -> "table_constraints",
      13602 -> "table_privileges",
      13611 -> "tables",
      25 -> "text",
      27 -> "tid",
      1083 -> "time",
      13416 -> "time_stamp",
      1114 -> "timestamp",
      1184 -> "timestamptz",
      1266 -> "timetz",
      13616 -> "transforms",
      2279 -> "trigger",
      13621 -> "triggered_update_columns",
      13626 -> "triggers",
      3310 -> "tsm_handler",
      4533 -> "tsmultirange",
      3615 -> "tsquery",
      3908 -> "tsrange",
      4534 -> "tstzmultirange",
      3910 -> "tstzrange",
      3614 -> "tsvector",
      2970 -> "txid_snapshot",
      13631 -> "udt_privileges",
      705 -> "unknown",
      13640 -> "usage_privileges",
      13649 -> "user_defined_types",
      13736 -> "user_mapping_options",
      13741 -> "user_mappings",
      2950 -> "uuid",
      1562 -> "varbit",
      1043 -> "varchar",
      13654 -> "view_column_usage",
      13659 -> "view_routine_usage",
      13664 -> "view_table_usage",
      13669 -> "views",
      2278 -> "void",
      28 -> "xid",
      5069 -> "xid8",
      142 -> "xml",
      13418 -> "yes_or_no"
    ).map((k, v) => Oid(k.toUInt) -> v)

  private val reverse = mapping.map(_.swap)

  inline override def map(c: String): Oid =
    reverse(c)

  inline override def rev(oid: Oid): String = mapping(oid)

end OidMapping