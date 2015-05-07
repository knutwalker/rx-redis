/*
 * Copyright 2014 – 2015 Paul Horn
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

package rx.redis.clients

import rx.redis.RedisCommand
import rx.redis.pipeline.OperatorDecode
import rx.redis.resp.RespType
import rx.redis.serialization._

import rx.Observable

import io.netty.buffer.{ ByteBufAllocator, ByteBuf }

import util.control.NonFatal

trait GenericClient extends KeyCommands
    with StringCommands
    with ConnectionCommands
    with HashCommands {

  def disconnect(): Observable[Unit]

  def reopen(): GenericClient

  def shallowClose(): Observable[Unit]

  def sendCommand(bb: ByteBuf): Observable[RespType]

  final def command(s: String with RedisCommand): Observable[RespType] =
    sendCommand(writeStringAsRedisCommand(s))

  final def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType] = {
    val buf = alloc.buffer()
    try {
      A.write(buf, cmd)
    } catch {
      case NonFatal(ex) ⇒
        buf.release()
        Observable.error(ex)
    }
    if (buf.isReadable) {
      sendCommand(buf)
    } else {
      buf.release()
      Observable.empty()
    }
  }

  protected final def run[A](cmd: A)(implicit A: Writes[A], R: Reads[A]): Observable[R.R] =
    command(cmd)(A).lift(new OperatorDecode[A, R.R](R))

  protected def alloc: ByteBufAllocator
}
