package com.example;

import rx.Observable;
import rx.redis.RxRedis;
import rx.redis.api.Client;
import rx.redis.japi.DefaultBytes;
import rx.redis.serialization.Bytes;
import scala.Option;

public final class JExample {

  public static void main(final String[] args) {

    final Bytes<String> stringBytes = DefaultBytes.String();
    final Client client = RxRedis.connect("localhost", 6379, false);

    client.del(new String[]{"foo", "foo1", "foo2", "foo3", "foo4", "foo5", "what?"})
        .toBlocking().forEach(System.out::println);

    client.set("foo1", "foo1", stringBytes);
    client.set("foo2", "foo2", stringBytes);
    client.set("foo3", "foo3", stringBytes);

    final Observable<Option<String>> gets =
        client.get("foo", stringBytes)
            .mergeWith(client.get("foo1", stringBytes));

    final Observable<Option<String>> mget =
        client.mget(new String[]{"foo1", "foo2", "foo4", "foo3", "foo5"}, stringBytes);

    gets.mergeWith(mget)
        .forEach(response -> System.out.println("GET or MGET = " + response));

    client.ping()
        .concatWith(client.echo("42", stringBytes))
        .concatWith(client.incr("what?").map(String::valueOf))
        .subscribe(
            r -> System.out.println("mixed = " + r),
            Throwable::printStackTrace,
            client::shutdown);

    RxRedis.await(client);
  }
}
