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

package rx.redis.commands.it

import rx.functions.Func2

class StringCommandsSpec extends CommandsSuite {


  test("GET and SET") {
    client.set("foo", "bar")
    val result = client.get[String]("foo").synch
    assert(result == Some("bar"))

    val noResult = client.get[String]("bar").synch
    assert(noResult == None)
  }

  test("SETEX") {
    import scala.concurrent.duration._
    client.setEx("foo", "bar", 2.seconds)
    Thread.sleep(1000)
    assert(client.exists("foo").synch)
    Thread.sleep(1001)
    assert(!client.exists("foo").synch)
  }

  test("SETNX") {
    assert(!client.exists("foo").synch)
    client.setNx("foo", "bar")
    assert(client.get[String]("foo").synch == Some("bar"))
    client.setNx("foo", "baz")
    assert(client.get[String]("foo").synch == Some("bar"))
  }

  test("INCR* and DECR*") {
    def get() = client.get[Long]("foo").synch.getOrElse(0L)

    assert(get() == 0)
    client.incr("foo")
    assert(get() == 1)
    client.incrBy("foo", 10)
    assert(get() == 11)
    client.decr("foo")
    assert(get() == 10)
    client.decrBy("foo", 5)
    assert(get() == 5)
  }

  test("MSET") {

    client.mset("foo" -> "bar", "bar" -> "baz", "baz" -> "qux").synch

    val results1 = client.mget[String]("foo", "bar", "baz").synchAll.flatten

    val m = Map("foo" -> "baz", "bar" -> "qux", "baz" -> "bar")
    client.mset(m.toSeq: _*).synch

    val results2 = client.mget[String]("foo", "bar", "baz").synchAll.flatten

    assert(results1 == List("bar", "baz", "qux"))
    assert(results2 == List("baz", "qux", "bar"))
  }

  test("MGET") {

    client.mset("foo" -> "bar", "bar" -> "baz", "baz" -> "qux")

    val gets = client.get[String]("foo").
      concatWith(client.get[String]("bar")).
      concatWith(client.get[String]("baz"))

    val mgets = client.mget[String]("foo", "bar", "baz")

    val combined = gets.zip[Option[String], Option[(String, String)]](mgets, zipper)

    val sc = combined.synchAll
    sc foreach {
      case Some((g, mg)) => assert(g == mg)
      case _ => fail("None")
    }
  }

  test("STRLEN") {
    client.set("foo", "barinessy")
    assert(client.strLen("foo").synch == 9)
    assert(client.strLen("bar").synch == 0)
  }


  test("custom types") {

    val knut = Person("knut", 27)
    client.set("knut", knut)

    val stringResult = client.get[String]("knut").synch
    assert(stringResult == Some(knut.redisString))

    val personResult = client.get[Person]("knut").synch
    assert(personResult == Some(knut))
  }


  private val zipper: Func2[_ >: Option[String], _ >: Option[String], _ <: Option[(String, String)]] =
    new Func2[Option[String], Option[String], Option[(String, String)]] {
      def call(t1: Option[String], t2: Option[String]): Option[(String, String)] = {
        t1.flatMap(r1 => t2.map(r2 => (r1, r2)))
      }
    }
}
