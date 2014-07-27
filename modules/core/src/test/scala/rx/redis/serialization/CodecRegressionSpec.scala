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

package rx.redis.serialization

import io.netty.buffer.{ ByteBuf, Unpooled }
import org.scalacheck.Gen
import org.scalatest.FunSuite
import org.scalatest.prop.PropertyChecks
import rx.redis.resp._

class CodecRegressionSpec extends FunSuite with PropertyChecks with ByteBufAccess {
  import org.scalacheck.Arbitrary.arbitrary

  val genString = Gen.alphaStr suchThat (!_.contains("\r\n")) map RespString
  val genError = Gen.alphaStr suchThat (!_.contains("\r\n")) map RespError
  val genBytes = arbitrary[Array[Byte]] map (RespBytes(_))
  val genInteger = arbitrary[Long] map RespInteger
  val genAnyPrimitive = Gen.oneOf(genString, genError, genBytes, genInteger, Gen.const(NullString))
  val genArray = Gen.containerOf[Array, DataType](genAnyPrimitive) map RespArray
  val genAnyData = Gen.oneOf(genString, genError, genBytes, genInteger, genArray, Gen.const(NullArray))

  test("serialze <> deserialze must be the same") {

    val serializer = new Serializer[ByteBuf]
    val deserializer = new Deserializer[ByteBuf]

    forAll(genAnyData -> "dataType", minSuccessful(500)) { dataType ⇒
      val buf = Unpooled.buffer()
      serializer(dataType, buf)
      val respType = deserializer(buf)
      assert(respType == dataType)
    }
  }

  test("serialze <> deserialize many must the same") {

    val serializer = new Serializer[ByteBuf]
    val deserializer = new Deserializer[ByteBuf]

    forAll(Gen.listOf(genAnyData) -> "dataTypes", minSuccessful(500)) { dataTypes ⇒
      val buf = Unpooled.buffer()
      val dts = dataTypes.iterator
      while (dts.hasNext) {
        serializer(dts.next(), buf)
      }

      val respTypes = dataTypes map (_ ⇒ deserializer(buf))

      dataTypes.zipAll(respTypes, null, null) foreach {
        case (expected, actual) ⇒
          assert(expected == actual)
      }
    }
  }
}
