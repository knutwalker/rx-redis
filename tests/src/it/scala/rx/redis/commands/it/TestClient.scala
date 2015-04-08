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

import rx.redis._
import rx.redis.clients.RawClient
import rx.redis.util.{DefaultRedisHost, DefaultRedisPort}

import org.scalatest.{BeforeAndAfter, Suite}


trait TestClient extends BeforeAndAfter { this: Suite =>

  private[this] var _client: RawClient = _

  before {
    _client = RawClient(DefaultRedisHost, DefaultRedisPort)
    _client.command(cmd"FLUSHDB").toBlocking.first()
  }

  after {
    _client.disconnect().toBlocking.lastOrDefault(())
  }

  protected def client = _client

}
