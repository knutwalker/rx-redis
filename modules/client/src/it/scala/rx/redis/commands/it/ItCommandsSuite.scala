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

import org.scalatest.FunSuiteLike
import rx.Observable

import scala.collection.convert.DecorateAsScala


trait ItCommandsSuite extends FunSuiteLike with TestClient with PersonType with DecorateAsScala {

  implicit class SynchableObservable[T](val underlying: Observable[T]) {
    def synch: T =
      underlying.toBlocking.single()

    def synchAll: List[T] =
      underlying.toList.toBlocking.single().asScala.toList
  }
}
