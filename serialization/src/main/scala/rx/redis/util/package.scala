package rx.redis

import rx.redis.resp._
import rx.redis.serialization.Writes

import java.nio.charset.StandardCharsets

// some temp in here
package object util {

  val Utf8 = StandardCharsets.UTF_8

  implicit class CommandQuote(val ctx: StringContext) extends AnyVal {
    def resp(args: String*): DataType = {
      val strings = ctx.parts.iterator
      val expressions = args.iterator
      val result = strings.
        zipAll(expressions, "", "").
        map { case (a, b) => a + b }.
        foldLeft("")(_ + _).
        replaceAllLiterally("\\r\\n", "\r\n")

      Writes[String].write(result)
    }
  }

  val respContent: (RespType) => String = _.toString

  val preview = respContent andThen (_.replaceAllLiterally("\r\n", "\\r\\n").take(50))
}
