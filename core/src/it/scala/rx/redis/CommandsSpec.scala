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

package rx.redis

import collection.JavaConverters._

import org.scalatest.{BeforeAndAfter, FunSuite}
import rx.functions.Func2
import rx.redis.client.RawClient
import rx.redis.serialization.BytesFormat
import rx.redis.util.Utf8


class CommandsSpec extends FunSuite with BeforeAndAfter {

  case class Person(name: String, age: Int) {
    def redisString = s"${age}|${name}"
  }
  object Person {
    def apply(redisString: String): Person = {
      val Array(age, name) = redisString.split("\\|", 2)
      apply(name, age.toInt)
    }
  }
  implicit val PersonFormat = new BytesFormat[Person] {
    def bytes(value: Person): Array[Byte] =
      value.redisString.getBytes(Utf8)

    def value(bytes: Array[Byte]): Person =
      Person(new String(bytes, Utf8))
  }

  private var client: RawClient = _

  before {
    client = RawClient("127.0.0.1", 6379, shareable = true)
    client.command(cmd"FLUSHDB").toBlocking.first()
  }

  after {
    client.shutdown()
    client.closedObservable.toBlocking.lastOrDefault(())
  }

  test("GET and SET") {
    client.set[String]("foo", "bar")
    val result = client.get[String]("foo").toBlocking.last()

    assert(result == Some("bar"))
  }

  test("custom types in GET and SET") {

    val knut = Person("knut", 27)

    client.set[Person]("knut", knut)

    val stringResult = client.get[String]("knut").toBlocking.last()
    assert(stringResult == Some(knut.redisString))

    val personResult = client.get[Person]("knut").toBlocking.last()
    assert(personResult == Some(knut))
  }

  test("GET returns None when not found") {
    val result = client.get[String]("foo").toBlocking.last()
    assert(result == None)
  }

  test("MGET und multiple GETs behave the same") {

    val m = Map("foo" -> "bar", "bar" -> "baz", "baz" -> "qux")

    client.mset(m.toSeq: _*).toBlocking.last()

    val gets = client.get[String]("foo").
      concatWith(client.get[String]("bar")).
      concatWith(client.get[String]("baz"))

    val mgets = client.mget[String]("foo", "bar", "baz")

    val combined = gets.zip[Option[String], Option[(String, String)]](mgets, zipper)

    val sc = combined.toBlocking.toIterable.asScala
    sc foreach {
      case Some((g, mg)) => assert(g == mg)
      case _ => fail("None")
    }
  }


  private val zipper: Func2[_ >: Option[String], _ >: Option[String], _ <: Option[(String, String)]] =
    new Func2[Option[String], Option[String], Option[(String, String)]] {
      def call(t1: Option[String], t2: Option[String]): Option[(String, String)] = {
        t1.flatMap(r1 => t2.map(r2 => (r1, r2)))
      }
    }
}
