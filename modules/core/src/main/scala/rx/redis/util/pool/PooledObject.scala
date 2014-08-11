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

package rx.redis.util.pool

trait PooledObject[A] {
  def create(): A
  def activate(obj: A): A
  def passivate(obj: A): A
  def destroy(obj: A): Unit
  def validate(obj: A): Boolean
}
object PooledObject {
  @inline def apply[A](implicit A: PooledObject[A]): PooledObject[A] = A

  abstract class PooledObjectBase[A] extends PooledObject[A] {
    def activate(obj: A): A = obj
    def passivate(obj: A): A = obj
    def destroy(obj: A): Unit = ()
    def validate(obj: A): Boolean = true
  }

  def of[A](make: ⇒ A): PooledObject[A] = new PooledObjectBase[A] {
    def create(): A = make
  }
}
