/*
 * Copyright 2014 – 2015 Paul Horn
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

import scala.annotation.tailrec

private abstract class ObjectPoolBase[A](settings: PoolSettings)(implicit A: PooledObject[A]) extends ObjectPool[A] {

  protected def pull(): Option[A]
  protected def push(obj: A): Boolean
  protected def poolSize: Int
  protected def decrementPoolSize(): Unit
  protected def incrementPoolSize(prev: Int): Boolean

  for (_ ← 1 to settings.min) {
    create() foreach push
  }

  @tailrec
  private def create(): Option[A] = {
    val current = poolSize
    if (current >= settings.max) {
      None
    } else {
      val obj = A.create()
      if (incrementPoolSize(current)) {
        Some(obj)
      } else {
        A.destroy(obj)
        create()
      }
    }
  }

  private[this] def activate(obj: A): A = A.activate(obj)

  private[this] def validate(obj: A): Boolean = A.validate(obj)

  private[this] def passivate(obj: A): A = A.passivate(obj)

  private[this] def destroy(obj: A): Unit = {
    decrementPoolSize()
    A.destroy(obj)
  }

  final def get: Option[A] =
    pull() orElse create() filter validate map activate

  final def recycle(obj: A): Unit =
    if (validate(obj)) {
      push(passivate(obj))
    } else {
      destroy(obj)
    }
}
