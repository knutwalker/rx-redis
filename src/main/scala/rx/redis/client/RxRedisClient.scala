package rx.redis.client

import io.netty.buffer.ByteBuf
import io.reactivex.netty.client.RxClient
import rx.functions.Func2
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.Observable
import rx.subjects.{AsyncSubject, PublishSubject}

import rx.redis.api
import rx.redis.api.Writes
import rx.redis.resp.RespType
import rx.redis.util.observers.DiscardingObserver

import java.util.concurrent.locks.ReentrantLock


private[redis] object RxRedisClient {
  final private class JoinFun[Req <: rx.Observer[Res], Res] extends Func2[Req, Res, Unit] {
    def call(t1: Req, t2: Res): Unit = {
      t1.onNext(t2)
      t1.onCompleted()
    }
  }
  private[redis] trait Locking {
    private val _lock = new ReentrantLock(false)
    protected def locked[A](f: => A): A = {
      val lock = _lock
      lock.lockInterruptibly()
      try {
        f
      }
      finally {
        lock.unlock()
      }
    }
  }
}
private[redis] final class RxRedisClient (client: RxClient[ByteBuf, RespType])
  extends api.Client
  with StringCommands
  with RxRedisClient.Locking {

  type Req = rx.Observer[RespType]
  type Res = RespType

  private val connect = client.connect()
  // TODO: .cache() and flatMap all the time?
  private val connection = connect.toBlocking.first()

  private val requestStream = PublishSubject.create[Req]()
  private val responseStream =  connection.getInput
  private val requestResponseStream =
    requestStream.zip[Res, Unit](responseStream, new RxRedisClient.JoinFun[Req, Res])
  requestResponseStream.subscribe()

  protected def allocator = connection.getAllocator

  private def createResponse() = {
    val s = AsyncSubject.create[RespType]()
    requestStream.onNext(s)
    s
  }

  def command(cmd: ByteBuf): Observable[RespType] = locked {
    connection.writeAndFlush(cmd)
    createResponse()
  }

  def command[B](cmd: B)(implicit B: Writes[B]): Observable[RespType] = locked {
    connection.writeAndFlush(cmd, B.contentTransformer)
    createResponse()
  }

  def shutdown(): Observable[Unit] = {
    requestStream.onCompleted()
    val closeObs = toScalaObservable(connection.close(true)).map(_ => ())
    closeObs.subscribe()
    client.shutdown()
    closeObs
  }

  lazy val closedObservable: Observable[Unit] =
    DiscardingObserver(requestResponseStream)
}
