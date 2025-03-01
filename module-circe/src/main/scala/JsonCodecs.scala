package roach.circe

import io.circe.parser.*
import roach.codecs
import io.circe.Codec as CirceCodec
import io.circe.syntax.*
import io.circe.Json

def jsonOf[T: CirceCodec] = roach.Codec.stringLike("json")(
  raw => decode(raw).fold(throw _, identity),
  a => a.asJson.noSpaces
)

def jsonType[T: CirceCodec](name: String) = roach.Codec.stringLike(name)(
  raw => decode(raw).fold(throw _, identity),
  a => a.asJson.noSpaces
)

val json =
  roach.Codec.stringLike("json")(parse(_).fold(throw _, identity), _.noSpaces)

val jsonb =
  roach.Codec.stringLike("jsonb")(parse(_).fold(throw _, identity), _.noSpaces)
