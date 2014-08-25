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

package rx.redis.serialization

import io.netty.buffer.{ ByteBuf, ByteBufAllocator }

import rx.redis.resp.RespType
import rx.redis.serialization.Deserializer.NotEnoughData
import rx.redis.util.Utf8

import java.nio.charset.Charset
import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object ByteBufDeserializer {

  case class ParseAllResult(data: immutable.Seq[RespType], hasRemainder: Boolean)

  private final val INSTANCE = new Deserializer[ByteBuf]()(ByteBufAccess)

  private def releaseAfterUse[A](bb: ByteBuf)(f: ⇒ A): A =
    try f finally bb.release()

  def apply(bb: ByteBuf): RespType = INSTANCE(bb)

  def apply(bytes: Array[Byte], alloc: ByteBufAllocator): RespType = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    INSTANCE(bb)
  }
  def apply(string: String, charset: Charset, alloc: ByteBufAllocator): RespType = {
    apply(string.getBytes(charset), alloc)
  }
  def apply(string: String, alloc: ByteBufAllocator): RespType = {
    apply(string, Utf8, alloc)
  }

  def foreach(bb: ByteBuf)(f: RespType ⇒ Unit): Boolean = {
    @tailrec def loop(): Boolean =
      if (!bb.isReadable) false
      else {
        f(INSTANCE(bb))
        loop()
      }
    try {
      loop()
    } catch {
      case NotEnoughData ⇒ true
    }
  }
  def foreach(bytes: Array[Byte], alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Boolean = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    releaseAfterUse(bb)(foreach(bb)(f))
  }
  def foreach(string: String, charset: Charset, alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Boolean = {
    foreach(string.getBytes(charset), alloc)(f)
  }
  def foreach(string: String, alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Boolean = {
    foreach(string, Utf8, alloc)(f)
  }

  def parseAll(bb: ByteBuf): ParseAllResult = {
    val lb = new ListBuffer[RespType]()
    val hasRemainder = foreach(bb)(lb += _)
    ParseAllResult(lb.result(), hasRemainder)
  }
  def parseAll(bytes: Array[Byte], alloc: ByteBufAllocator): ParseAllResult = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    releaseAfterUse(bb)(parseAll(bb))
  }
  def parseAll(string: String, charset: Charset, alloc: ByteBufAllocator): ParseAllResult = {
    parseAll(string.getBytes(charset), alloc)
  }
  def parseAll(string: String, alloc: ByteBufAllocator): ParseAllResult = {
    parseAll(string, Utf8, alloc)
  }
}
