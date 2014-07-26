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
package rx.redis.client

import io.reactivex.netty.client.RxClient
import rx.functions.{ Func1, Func2 }
import rx.subjects.{ AsyncSubject, PublishSubject }
import rx.{ Observable, Observer }

import rx.redis.resp.{ DataType, RespType }

private[redis] object DefaultClient {
  private object JoinFun extends Func2[Observer[RespType], RespType, Unit] {
    def call(t1: Observer[RespType], t2: RespType): Unit = {
      t1.onNext(t2)
      t1.onCompleted()
    }
  }
  private object VoidToUnit extends Func1[Void, Unit] {
    def call(t1: Void): Unit = ()
  }

  private object DiscardingObserver {
    def apply[A](o: Observable[Unit]): Observable[Unit] = {
      val s = AsyncSubject.create[Unit]()
      o.subscribe(new DiscardingObserver(s))
      s
    }
  }

  final class DiscardingObserver(target: Observer[_]) extends Observer[Unit] {
    def onNext(t: Unit): Unit = ()
    def onError(error: Throwable): Unit = target.onError(error)
    def onCompleted(): Unit = target.onCompleted()
  }
}
private[redis] final class DefaultClient(client: RxClient[DataType, RespType])
    extends RawClient {
  import rx.redis.client.DefaultClient._

  private val connect = client.connect()
  // TODO: cache() and flatMap all the time?
  private val connection = connect.toBlocking.first()

  private val requestStream = PublishSubject.create[Observer[RespType]]()
  private val responseStream = connection.getInput
  private val requestResponseStream =
    requestStream.zip[RespType, Unit](responseStream, JoinFun)

  private def createResponse() = {
    val s = AsyncSubject.create[RespType]()
    requestStream.onNext(s)
    s
  }

  def command(cmd: DataType): Observable[RespType] = {
    connection.writeAndFlush(cmd)
    createResponse()
  }

  def shutdown(): Observable[Unit] = {
    requestStream.onCompleted()
    val closeObs = connection.close(true).map[Unit](VoidToUnit)
    closeObs.subscribe()
    client.shutdown()
    closeObs
  }

  val closedObservable: Observable[Unit] =
    DiscardingObserver(requestResponseStream)
}
