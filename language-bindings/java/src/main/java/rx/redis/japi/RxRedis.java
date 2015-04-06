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

package rx.redis.japi;

import rx.Observable;
import rx.redis.clients.RawClient;
import rx.redis.resp.RespType;
import rx.redis.serialization.Writes;

@SuppressWarnings("UnusedDeclaration")
public final class RxRedis {

  private RxRedis() {}

  public static Client connect(final String host, final int port) {
    return new Client(RawClient.apply(host, port));
  }

  public static Client connect(final String host) {
    return connect(host, 6379);
  }

  public static Client connect(final int port) {
    return connect("127.0.0.1", port);
  }

  public static Client connect() {
    return connect(6379);
  }

  public static RespType command(final String cmd) {
    return Writes.DirectStringWrites$.MODULE$.write(cmd);
  }

  public static Observable<Void> disconnect(final Client client) {
    return client.disconnect();
  }

  public static void shutdown(final Client client) {
    disconnect(client).toBlocking().lastOrDefault(null);
  }

  /**
   * Use {@link #shutdown(Client)}
   */
  @Deprecated
  public static void await(final Client client) {
    shutdown(client);
  }
}
