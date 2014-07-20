package rx.redis.resp

import io.netty.buffer.UnpooledByteBufAllocator

import org.scalatest.{FunSuite, Inside}

import rx.redis.commands.Ping
import rx.redis.serialization.Writes
import rx.redis.util.Utf8

class SerialzerSpec extends FunSuite with Inside {

  val alloc = UnpooledByteBufAllocator.DEFAULT

  test("Ping") {

    val dt1 = RespArray(List(RespBytes("PING")))

    val result = Serializer(dt1, Utf8, alloc)
    println("result = " + result)

    val dt2 = implicitly[Writes[Ping.type]].write(Ping)

    println("dt1 = " + dt1)
    println("dt2 = " + dt2)

    val result2 = Serializer(dt2, Utf8, alloc)

    println("result2 = " + result2)

  }
}
