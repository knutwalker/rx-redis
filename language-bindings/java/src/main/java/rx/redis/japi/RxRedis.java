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

package rx.redis.japi;

import rx.redis.client.RawClient;

@SuppressWarnings("UnusedDeclaration")
public final class RxRedis {

  private RxRedis() {}

  public static Client connect(final String host, final int port, final boolean shareable) {
    return new Client(RawClient.apply(host, port, shareable));
  }

  public static Client connect(final String host, final int port) {
    return connect(host, port, true);
  }

  public static Client connect(final String host) {
    return connect(host, 6379);
  }

  public static Client connect(final int port) {
    return connect("127.0.0.1", port);
  }

  public static void await(final Client client) {
    client.closedObservable().toBlocking().lastOrDefault(null);
  }
}
