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
