package com.example;

import rx.Observable;
import rx.redis.japi.RxRedis;
import rx.redis.japi.Client;

import java.util.Optional;


public final class JavaExample {

  public static void main(final String[] args) {

    final Client client = RxRedis.connect("localhost", 6379);

    client.del("foo", "foo1", "foo2", "foo3", "foo4", "foo5", "what?")
        .toBlocking().forEach(System.out::println);

    client.get("key");
    client.mset(
        Client.pair("foo1", "foo1"),
        Client.pair("foo2", "foo2"),
        Client.pair("foo3", "foo3"));

    final Observable<Optional<String>> gets =
        client.get("foo").mergeWith(client.get("foo1"));

    final Observable<Optional<String>> mget =
        client.mget("foo1", "foo2", "foo4", "foo3", "foo5");

    gets.mergeWith(mget)
        .forEach(response -> System.out.println("GET or MGET = " + response));

    client.ping()
        .concatWith(client.echo("42"))
        .concatWith(client.incr("what?").map(String::valueOf))
        .doOnCompleted(client::shutdown)
        .subscribe(r -> System.out.println("mixed = " + r));

    RxRedis.await(client);
  }
}
