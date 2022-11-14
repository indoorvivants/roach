package roach

import scala.quoted.*

extension (inline ctx: StringContext)
  transparent inline def sql(inline args: Any*): Any =
    ${ MacroImpl.sql('ctx, 'args) }

private[roach] object MacroImpl:
  def sql(strCtxExpr: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using
      Quotes
  ): Expr[Any] =
    val args: List[Expr[Any]] =
      val Varargs(args) = argsExpr
      args.toList

    val codecsBuilder = List.newBuilder[Expr[Any]]
    val segmentBuilders = List.newBuilder[Expr[Either[Int, String]]]

    args.foreach {
      case '{ $e: Codec[t] } =>
        codecsBuilder.addOne(e)
        segmentBuilders.addOne('{
          Left($e.length)
        })
      case '{ $other: String } =>
        segmentBuilders.addOne('{
          Right($other)
        })
      case o =>
        quotes.reflect.report.errorAndAbort(
          "Interpolators can only be codecs or strings"
        )
    }

    val codecs = codecsBuilder.result()
    val insertions = Expr.ofList(segmentBuilders.result())

    val query = '{
      val parts = $strCtxExpr.parts.map(Option.apply)
      val ins = $insertions.map(Option.apply)

      val sb = new StringBuilder
      var i = 1

      parts.zipAll(ins, None, None).foreach { case (str, insertion) =>
        str.foreach { s => sb.append(s) }

        insertion.foreach {
          case Left(l) =>
            sb.append(
              (0 until l)
                .map(i + _)
                .map(num => "$" + num.toString)
                .mkString(",")
            )
            i += l
          case Right(const) => sb.append(const)
        }
      }

      sb.result
    }

    codecs match
      case Nil => '{ Query($query) }
      case '{ $e: Codec[t] } :: Nil =>
        '{ Query[t]($query, $e) }
      case h :: t :: _ =>
        val listOf = codecs.reduceLeft[Expr[Any]] {
          case ('{ $e: Codec[t] }, '{ $e1: Codec[t1] }) =>
            '{ $e ~ $e1 }
        }

        listOf match
          case '{ $e: Codec[t] } =>
            '{
              Query[t]($query, $e)
            }

    end match
  end sql
end MacroImpl
