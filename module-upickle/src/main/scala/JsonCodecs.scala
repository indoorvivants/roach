package roach.upickle

import roach.codecs
// import io.circe.{Codec as CirceCodec}
// import io.circe.syntax.*
// import io.circe.Json

import upickle.default.ReadWriter

def jsonOf[T: ReadWriter] = roach.Codec.stringLike("json")(
  raw => upickle.default.read[T](raw),
  a => upickle.default.write[T](a)
)

def jsonType[T: ReadWriter](name: String) = roach.Codec.stringLike(name)(
  raw => upickle.default.read[T](raw),
  a => upickle.default.write[T](a)
)

val json = jsonOf[ujson.Value]
val jsonb = jsonType[ujson.Value]("jsonb")
