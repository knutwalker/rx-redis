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

package rx.redis.clients

import java.util.concurrent.atomic.AtomicBoolean

import rx.Observable
import rx.redis.pipeline.NettyClient
import rx.redis.resp.{ DataType, RespType }
import rx.subjects.AsyncSubject

private[redis] final class DefaultClient(netty: NettyClient[DataType, RespType]) extends RawClient {

  private val isClosed = new AtomicBoolean(false)
  private val alreadyClosed: Observable[Unit] =
    Observable.error(new IllegalStateException("Client has already shutdown."))

  def command(cmd: DataType): Observable[RespType] = {
    val s = AsyncSubject.create[RespType]()
    netty.send(cmd, s)
    s
  }

  def shutdown(): Observable[Unit] = {
    if (isClosed.compareAndSet(false, true)) {
      close()
    } else {
      alreadyClosed
    }
  }

  private def close(): Observable[Unit] = {
    netty.close()
  }

  val closedObservable: Observable[Unit] =
    netty.closed
}
