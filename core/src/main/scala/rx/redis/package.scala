package rx

import rx.redis.resp.DataType
import rx.redis.serialization.Writes

package object redis {

  implicit class CommandQuote(val ctx: StringContext) extends AnyVal {
    def cmd(args: String*): DataType = {
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
}
