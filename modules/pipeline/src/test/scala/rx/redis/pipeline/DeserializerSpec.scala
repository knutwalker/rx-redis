/*
 * Copyright 2014 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.redis.pipeline

import io.netty.buffer.UnpooledByteBufAllocator

import rx.redis.resp.{NotEnoughData, RespBytes}
import rx.redis.util.Utf8

import org.scalatest.{FunSuite, Inside}

class DeserializerSpec extends FunSuite with Inside {

  val alloc = UnpooledByteBufAllocator.DEFAULT

  test("apply should parse only one item") {
    val resp = "$3\r\nfoo\r\n$3\r\nbar\r\n"
    val result = ByteBufDeserializer(resp, Utf8, alloc)
    assert(result == RespBytes("foo"))
  }

  test("foreach should execute an callback for each item") {
    val resp = "$3\r\nfoo\r\n$3\r\nbar\r\n"
    val expected = Iterator(RespBytes("foo"), RespBytes("bar"))
    ByteBufDeserializer.foreach(resp, Utf8, alloc) {
      actual ⇒ assert(actual == expected.next())
    }
  }

  test("foreach should return whether there is data left") {
    val resp = "$3\r\nfoo\r\n$3\r\nbar\r\n$2\r"
    val remainder = ByteBufDeserializer.foreach(resp, Utf8, alloc)(_ ⇒ ())
    assert(remainder == Some(NotEnoughData))

    val resp2 = "$3\r\nfoo\r\n$3\r\nbar\r\n"
    val remainder2 = ByteBufDeserializer.foreach(resp2, Utf8, alloc)(_ ⇒ ())
    assert(remainder2 == None)
  }

  test("parseAll should parse all items") {
    val resp = "$3\r\nfoo\r\n$3\r\nbar\r\n"
    val result = ByteBufDeserializer.parseAll(resp, Utf8, alloc)
    assert(result == List(RespBytes("foo"), RespBytes("bar")))
  }

  test("parseAll should also include whether there is data left") {
    val resp = "$3\r\nfoo\r\n$3\r\nbar\r\n$2\r"
    val result = ByteBufDeserializer.parseAll(resp, Utf8, alloc)
    assert(result == List(RespBytes("foo"), RespBytes("bar"), NotEnoughData))
  }
}
