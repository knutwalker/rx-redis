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

import java.nio.charset.Charset

import io.netty.buffer.{ ByteBuf, ByteBufAllocator }
import rx.redis.resp._
import rx.redis.serialization.Deserializer
import rx.redis.util.Utf8

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object ByteBufDeserializer {

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

  def foreach(bb: ByteBuf)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    @tailrec def loop(): Option[NotEnoughData.type] =
      if (!bb.isReadable) None
      else INSTANCE(bb) match {
        case NotEnoughData ⇒ Some(NotEnoughData)
        case e: ErrorType ⇒ {
          f(e)
          None
        }
        case x ⇒ {
          f(x)
          loop()
        }
      }
    loop()
  }
  def foreach(bytes: Array[Byte], alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    releaseAfterUse(bb)(foreach(bb)(f))
  }
  def foreach(string: String, charset: Charset, alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    foreach(string.getBytes(charset), alloc)(f)
  }
  def foreach(string: String, alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    foreach(string, Utf8, alloc)(f)
  }

  def parseAll(bb: ByteBuf): immutable.Seq[RespType] = {
    val lb = new ListBuffer[RespType]()
    foreach(bb)(lb += _) foreach (lb += _)
    lb.result()
  }
  def parseAll(bytes: Array[Byte], alloc: ByteBufAllocator): immutable.Seq[RespType] = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    releaseAfterUse(bb)(parseAll(bb))
  }
  def parseAll(string: String, charset: Charset, alloc: ByteBufAllocator): immutable.Seq[RespType] = {
    parseAll(string.getBytes(charset), alloc)
  }
  def parseAll(string: String, alloc: ByteBufAllocator): immutable.Seq[RespType] = {
    parseAll(string, Utf8, alloc)
  }
}
