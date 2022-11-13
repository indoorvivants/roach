package roach

opaque type Validated[+A] = Either[String, A]
object Validated:
  inline def apply[A](inline value: A): Validated[A] =
    Right(value)

  inline def error[A](inline msg: String): Validated[Nothing] =
    Left(msg)

  extension [A](v: Validated[A])
    inline def getOrThrow: A =
      v match
        case Left(err) => throw new RoachFatalException(err, None)
        case Right(r)  => r
    inline def either: Either[String, A] = v
    inline def map[B](inline f: A => B): Validated[B] =
      v.map(f)
end Validated
