package roach

import scala.quoted.* // imports Quotes, Expr
import java.nio.file.Paths

opaque type ResourceFile = String
object ResourceFile:
  inline def apply(name: String): ResourceFile =
    ${ applyImpl('name) }

  inline def applyUnsafe(name: String): ResourceFile = name

  private def applyImpl(x: Expr[String])(using Quotes): Expr[ResourceFile] =
    import quotes.reflect.report
    if getClass().getResourceAsStream(x.valueOrAbort) != null then x
    else report.errorAndAbort(s"${x.valueOrAbort} doesn't exist as a resource")

  extension (a: ResourceFile)
    inline def value: String = a
    inline def filename: String = Paths.get(a).getFileName().toString
end ResourceFile
