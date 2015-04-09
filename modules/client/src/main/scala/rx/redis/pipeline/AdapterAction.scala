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

package rx.redis.pipeline

import rx.Observer
import io.netty.util.Recycler
import io.netty.util.Recycler.Handle

import rx.redis.resp.RespType

class AdapterAction private (private val handle: Handle) {
  private[this] var _cmd: RespType = _
  private[this] var _sender: Observer[RespType] = _

  def cmd = _cmd
  def sender = _sender

  def update(cmd: RespType, sender: Observer[RespType]): Unit = {
    _cmd = cmd
    _sender = sender
  }

  def recycle(): Unit = {
    _cmd = null
    _sender = null
    AdapterAction.InstanceRecycler.recycle(this, handle)
  }
}

object AdapterAction {
  private final val InstanceRecycler = new Recycler[AdapterAction] {
    def newObject(handle: Handle): AdapterAction = new AdapterAction(handle)
  }

  def apply(cmd: RespType, sender: Observer[RespType]): AdapterAction = {
    val adapterAction = InstanceRecycler.get()
    adapterAction.update(cmd, sender)
    adapterAction
  }
}
