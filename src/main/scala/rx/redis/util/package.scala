package rx.redis

import rx.redis.resp._

// some temp in here
package object util {

  implicit class CommandQuote(val ctx: StringContext) extends AnyVal {
    def resp(args: Any*): String = {
      val strings = ctx.parts.iterator
      val expressions = args.iterator
      val result = strings.
        zipAll(expressions.map(_.toString), "", "").
        map { case (a, b) => a + b }.
        foldLeft("")(_ + _)

      val items = result.split(' ')
      val buf = new StringBuffer("*").append(items.size).append("\r\n")
      for (item <- items) {
        buf.append("$").append(item.size).append("\r\n").append(item).append("\r\n")
      }
      buf.toString
    }
  }

  val respContent: (RespType) => String = _.toString

  val preview = respContent andThen (_.replaceAllLiterally("\r\n", "\\r\\n").take(30))
}
