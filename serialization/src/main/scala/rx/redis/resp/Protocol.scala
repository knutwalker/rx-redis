package rx.redis.resp

private[redis] object Protocol {

  final val Plus = '+'.toByte
  final val Minus = '-'.toByte
  final val Colon = ':'.toByte
  final val Dollar = '$'.toByte
  final val Asterisk = '*'.toByte

  final val typeChars = List(Plus, Minus, Colon, Dollar, Asterisk)

  final val Cr = '\r'.toByte
  final val Lf = '\n'.toByte

  final val CrLf = Array(Cr, Lf)
  final val Nullary = Array(Minus, '1'.toByte)
}
