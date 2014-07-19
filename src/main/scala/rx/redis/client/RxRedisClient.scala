package rx.redis.client

import io.netty.buffer.ByteBuf
import io.reactivex.netty.client.RxClient
import rx.functions.{Func1, Func2}
import rx.subjects.{AsyncSubject, PublishSubject}
import rx.{Observable, Observer}

import rx.redis.api
import rx.redis.resp.RespType
import rx.redis.serialization.Writes


private[redis] object RxRedisClient {
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
private[redis] final class RxRedisClient (client: RxClient[ByteBuf, RespType])
  extends api.Client
  with Commands {
  import rx.redis.client.RxRedisClient._

  private val connect = client.connect()
  // TODO: cache() and flatMap all the time?
  private val connection = connect.toBlocking.first()

  private val requestStream = PublishSubject.create[Observer[RespType]]()
  private val responseStream =  connection.getInput
  private val requestResponseStream =
    requestStream.zip[RespType, Unit](responseStream, JoinFun)

  protected def allocator = connection.getAllocator

  private def createResponse() = {
    val s = AsyncSubject.create[RespType]()
    requestStream.onNext(s)
    s
  }

  def command(cmd: ByteBuf): Observable[RespType] = synchronized {
    connection.writeAndFlush(cmd)
    createResponse()
  }

  def command[A](cmd: A)(implicit B: Writes[A]): Observable[RespType] = synchronized {
    connection.writeAndFlush(cmd, B.contentTransformer)
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
