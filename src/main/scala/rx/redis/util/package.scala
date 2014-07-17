package rx.redis

package object util {
  val command = (s: String) => {
    val sb = new StringBuilder

    val items = s.split(' ')

    sb += '*'
    sb ++= items.size.toString
    sb ++= "\r\n"

    for (item <- items) {
    sb += '$'
    sb ++= item.length.toString
    sb ++= "\r\n"
    sb ++= item
    sb ++= "\r\n"
    }

    sb.result()

  }
}
