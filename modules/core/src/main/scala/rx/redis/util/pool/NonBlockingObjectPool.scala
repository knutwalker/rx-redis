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

import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }
import scala.annotation.tailrec
import scala.collection.immutable.Queue

private class NonBlockingObjectPool[A: PooledObject](settings: PoolSettings) extends ObjectPoolBase[A](settings) {
  private final val queue = new AtomicReference[Queue[A]](Queue.empty[A])
  private final val currentSize = new AtomicInteger(0)

  @tailrec
  protected final def pull(): Option[A] = {
    val current = queue.get()
    if (current.isEmpty) None
    else {
      val (head, tail) = current.dequeue
      if (queue.compareAndSet(current, tail)) Some(head)
      else pull()
    }
  }

  @tailrec
  protected final def push(obj: A): Boolean = {
    val current = queue.get()
    val updated = current enqueue obj
    queue.compareAndSet(current, updated) || push(obj)
  }

  protected def poolSize: Int =
    currentSize.get()
  protected def incrementPoolSize(prev: Int): Boolean =
    currentSize.compareAndSet(prev, prev + 1)
  protected def decrementPoolSize(): Unit =
    currentSize.decrementAndGet()
}
