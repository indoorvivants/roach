package roach

import scala.quoted.*

extension (inline ctx: StringContext)
  transparent inline def sql(inline args: Any*): Any =
    ${ MacroImpl.sql('ctx, 'args) }

  // transparent inline def fr(inline args: Any*): Any =
  //   ${ MacroImpl.sql('ctx, 'args) }

trait Fragment:
  def sql: String
  def applied[T](codec: Codec[T]): AppliedFragment[T]

object Fragment:
  private class Impl(val sql: String) extends Fragment:
    def applied[T](codec: Codec[T]): AppliedFragment[T] =
      AppliedFragment(sql, codec)
  def apply(s: String): Fragment = Impl(s)

trait AppliedFragment[T]:
  def sql: Fragment
  def codec: Codec[T]

object AppliedFragment:
  private class Impl[T](val sql: Fragment, val codec: Codec[T])
      extends AppliedFragment[T]
  def apply[T](sql: String, codec: Codec[T]): AppliedFragment[T] =
    Impl(Fragment(sql), codec)

private[roach] object MacroImpl:
  def sql(strCtxExpr: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using
      Quotes
  ): Expr[Any] =
    val args: List[Expr[Any]] =
      val Varargs(args) = argsExpr
      args.toList

    val codecsBuilder = List.newBuilder[Expr[Any]]
    val segmentBuilders = List.newBuilder[Expr[Either[Int, String]]]

    import quotes.reflect.*

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
      case '{ $e: Fragment } =>
        segmentBuilders.addOne('{
          Right($e.sql)
        })
      case '{ $e: AppliedFragment[t] } =>
        codecsBuilder.addOne('{ $e.codec })
        segmentBuilders.addOne('{
          Left($e.codec.length)
        })
      case o =>
        quotes.reflect.report.errorAndAbort(
          "Interpolators can only be codecs or strings"
        )
    }

    val codecs = codecsBuilder.result()
    val insertions = Expr.ofList(segmentBuilders.result())

    val queryString = '{
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
      case Nil => '{ Query($queryString) }
      case '{ $e: Codec[t] } :: Nil =>
        '{ Query[t]($queryString, $e) }
      case codecs @ (h :: rest) =>
        var seenAt = collection.mutable.Map.empty[String, Int]
        val mappingB = List.newBuilder[Int]

        val enc = List.newBuilder[String]

        val originalCodecs = codecs.toArray

        var drift = 0
        var idx = 0
        codecs.foreach: expr =>
          val trueIndex = idx - drift

          enc += seenAt.toString() + " -- " + mappingB
            .result()
            .toString + "--" + expr.show + "\n"
          expr match
            case '{ $e: LabelledCodec[l, t] } =>
              val label = TypeRepr.of[l].show

              seenAt.get(label) match
                case None =>
                  mappingB += trueIndex
                  seenAt.update(label, trueIndex)
                case Some(value) =>
                  mappingB += value
                  drift += 1
            case '{ $e: Codec[t] } =>
              mappingB += trueIndex
          end match

          idx += 1

        val mapping = mappingB.result()
        val compressedCodecs = Array.ofDim[Expr[Any]](mapping.max + 1)

        originalCodecs.zipWithIndex.foreach: (codec, idx) =>
          val mapTo = mapping(idx)
          compressedCodecs(mapTo) = codec

        val remap = Expr(
          mapping.zipWithIndex
            .map(_.swap)
            .groupMapReduce(_._2)(s => List(s._1))(_ ++ _)
        )
        val map = Expr(mapping)
        def combineCodecs(codecs: List[Expr[Any]]): Expr[Any] =
          codecs match
            case head :: Nil =>
              head
            case '{ $h: Codec[t] } :: '{ $r: Codec[t1] } :: rest =>
              rest.foldLeft[Expr[Any]]('{ TupleCodec($h, $r) }):
                case ('{ $acc: TupleCodec[a, z] }, '{ $next: Codec[t] }) =>
                  '{ AppendCodec($acc, $next) }
                case ('{ $acc: AppendCodec[a, z] }, '{ $next: Codec[t] }) =>
                  '{ AppendCodec($acc, $next) }
                case (other, next) =>
                  quotes.reflect.report.errorAndAbort(other.show)

        end combineCodecs

        val original = combineCodecs(codecs)

        val userSupplied = combineCodecs(compressedCodecs.toList)

        (userSupplied, original) match
          case ('{ $e: Codec[userSuppliedT] }, '{ $e1: Codec[positionalT] }) =>
            '{
              val transform: userSuppliedT => positionalT = us =>
                val usTuple = us.asInstanceOf[Tuple].toArray
                val positional = Array.ofDim[Any]($map.length)
                var start = 0
                while start < usTuple.length do
                  val mapTo = $remap(start)
                  mapTo.foreach: idx =>
                    positional(idx) = usTuple(start)
                  start += 1

                Tuple.fromArray(positional).asInstanceOf[positionalT]
              end transform

              Query.applyTransformed[positionalT, userSuppliedT](
                $queryString,
                $e1,
                transform
              )
            }
        end match

    end match
  end sql
end MacroImpl
