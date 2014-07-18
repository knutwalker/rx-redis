package rx.redis.client

import io.netty.buffer.ByteBuf
import io.reactivex.netty.client.RxClient
import rx.{Observable, Observer}
import rx.functions.{Func1, Func2}
import rx.subjects.{AsyncSubject, PublishSubject}

import rx.redis.api
import rx.redis.api.Writes
import rx.redis.resp.RespType
import rx.redis.util.observers.DiscardingObserver


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
}
private[redis] final class RxRedisClient (client: RxClient[ByteBuf, RespType])
  extends api.Client
  with StringCommands {
  import RxRedisClient._

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

  def command[B](cmd: B)(implicit B: Writes[B]): Observable[RespType] = synchronized {
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
