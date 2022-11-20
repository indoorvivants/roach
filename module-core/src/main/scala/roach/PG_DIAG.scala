package roach

object PG_DIAG:
  inline def PG_DIAG_SEVERITY = 'S'.toInt
  inline def PG_DIAG_SEVERITY_NONLOCALIZED = 'V'.toInt
  inline def PG_DIAG_SQLSTATE = 'C'.toInt
  inline def PG_DIAG_MESSAGE_PRIMARY = 'M'.toInt
  inline def PG_DIAG_MESSAGE_DETAIL = 'D'.toInt
  inline def PG_DIAG_MESSAGE_HINT = 'H'.toInt
  inline def PG_DIAG_STATEMENT_POSITION = 'P'.toInt
  inline def PG_DIAG_INTERNAL_POSITION = 'p'.toInt
  inline def PG_DIAG_INTERNAL_QUERY = 'q'.toInt
  inline def PG_DIAG_CONTEXT = 'W'.toInt
  inline def PG_DIAG_SCHEMA_NAME = 's'.toInt
  inline def PG_DIAG_TABLE_NAME = 't'.toInt
  inline def PG_DIAG_COLUMN_NAME = 'c'.toInt
  inline def PG_DIAG_DATATYPE_NAME = 'd'.toInt
  inline def PG_DIAG_CONSTRAINT_NAME = 'n'.toInt
  inline def PG_DIAG_SOURCE_FILE = 'F'.toInt
  inline def PG_DIAG_SOURCE_LINE = 'L'.toInt
  inline def PG_DIAG_SOURCE_FUNCTION = 'R'.toInt
end PG_DIAG
