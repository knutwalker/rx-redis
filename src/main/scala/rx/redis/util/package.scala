package rx.redis

import rx.redis.resp._


// all temp in here
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

  val respContent: (RespType) => String = {
    case RespString(data) => data
    case RespError(reason) => reason
    case RespInteger(value: Long) => value.toString
    case RespArray(elements) => elements.map(respContent).mkString("[", ", ", "]")
    case NullString => "NULL"
    case NullArray => "NULL"
    case _ => "ERROR"
  }

  val preview = respContent andThen (_.replaceAllLiterally("\r\n", "\\r\\n").take(30))
}
