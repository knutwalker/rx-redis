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

import org.scalatest.{BeforeAndAfter, FunSuite}
import rx.redis.client.RawClient
import rx.redis.serialization.BytesFormat
import rx.redis.util.Utf8

class CommandsSpec extends FunSuite with BeforeAndAfter {

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
    case class Person(name: String, age: Int)
    implicit val PersonFormat = new BytesFormat[Person] {
      def bytes(value: Person): Array[Byte] = {
        val s = s"${value.age}|${value.name}"
        s.getBytes(Utf8)
      }

      def value(bytes: Array[Byte]): Person = {
        val s = new String(bytes, Utf8)
        val Array(age, name) = s.split("\\|", 2)
        Person(name, age.toInt)
      }
    }

    val knut = Person("knut", 27)

    client.set[Person]("knut", knut)

    val stringResult = client.get[String]("knut").toBlocking.last()
    assert(stringResult == Some("27|knut"))

    val personResult = client.get[Person]("knut").toBlocking.last()
    assert(personResult == Some(knut))

  }
}
