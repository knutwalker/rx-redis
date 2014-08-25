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

import rx.Observable
import rx.subjects.AsyncSubject

import rx.redis.pipeline.NettyClient
import rx.redis.resp.RespType

private[redis] final class DefaultClient(protected val netty: NettyClient) extends RawClient {

  def command(cmd: RespType): Observable[RespType] = {
    val s = AsyncSubject.create[RespType]()
    netty.send(cmd, s)
    s
  }

  protected def closeClient(): Observable[Unit] = {
    eagerObservable(netty.close())
  }

}
