package roach

opaque type Validated[+A] = Either[RoachError, A]
object Validated:
  inline def apply[A](inline value: A): Validated[A] =
    Right(value)

  inline def error[A](inline err: RoachError): Validated[Nothing] =
    Left(err)

  extension [A](inline v: Validated[A])
    inline def getOrThrow: A =
      v match
        case Left(err) => throw err
        case Right(r)  => r
    inline def either: Either[RoachError, A] = v
    inline def map[B](inline f: A => B): Validated[B] =
      v.map(f)
    inline def tap[B](inline f: A => B): Validated[A] =
      v match
        case Left(_)  => v
        case Right(a) => f(a); v
  end extension

end Validated
