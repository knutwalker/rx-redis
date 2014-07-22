package rx.redis.api

import rx.Observable

import rx.redis.commands.{HGet, HGetAll}
import rx.redis.serialization.{Bytes, Reads}


trait HashCommands { this: Client =>
  def hget[A: Bytes](key: String, field: String): Observable[Option[A]] =
    command(HGet(key, field)).flatMap(Reads.bytes.obsOptT[A])

  def hgetAll[A: Bytes](key: String): Observable[(String, A)] =
    command(HGetAll(key)).flatMap(Reads.unzip.obsAB[String, A])
}
