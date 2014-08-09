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

package rx.redis.pipeline

import rx.Observer

import io.netty.util.Recycler
import io.netty.util.Recycler.Handle

import rx.redis.resp.{ RespType, DataType }

class AdapterAction private (private val handle: Handle) {
  private var cmd: DataType = _
  private var sender: Observer[RespType] = _
  private var action: ChannelAction = _
}

object AdapterAction {
  private final val InstanceRecycler = new Recycler[AdapterAction] {
    def newObject(handle: Handle): AdapterAction = new AdapterAction(handle)
  }

  def recycle(aa: AdapterAction): Unit = {
    InstanceRecycler.recycle(aa, aa.handle)
  }

  def apply(cmd: DataType, sender: Observer[RespType], action: ChannelAction): AdapterAction = {
    val adapterAction = InstanceRecycler.get()
    adapterAction.cmd = cmd
    adapterAction.sender = sender
    adapterAction.action = action
    adapterAction
  }

  def unapply(aa: AdapterAction): Option[(DataType, Observer[RespType], ChannelAction)] =
    Some((aa.cmd, aa.sender, aa.action))

  def write(cmd: DataType, sender: Observer[RespType]): AdapterAction =
    apply(cmd, sender, ChannelAction.Write)

  def writeAndFlush(cmd: DataType, sender: Observer[RespType]): AdapterAction =
    apply(cmd, sender, ChannelAction.WriteAndFlush)

  def flush(cmd: DataType, sender: Observer[RespType]): AdapterAction =
    apply(cmd, sender, ChannelAction.Flush)
}
