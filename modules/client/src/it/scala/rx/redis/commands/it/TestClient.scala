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

import org.scalatest.{BeforeAndAfter, Suite}
import rx.redis._
import rx.redis.client.RawClient


trait TestClient extends BeforeAndAfter { this: Suite =>

  private var _client: RawClient = _

  before {
    _client = RawClient("127.0.0.1", 6379, shareable = true)
    _client.command(cmd"FLUSHDB").toBlocking.first()
  }

  after {
    _client.shutdown()
    _client.closedObservable.toBlocking.lastOrDefault(())
  }

  def client = _client

}
