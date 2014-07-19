package com.example;

import rx.Observer;
import rx.redis.RxRedis;
import rx.redis.api.Client;
import rx.redis.japi.DefaultWrites;
import rx.redis.resp.RespType;
import rx.redis.serialization.Writes;

import java.util.Random;

public final class JExample {

  static final Random random = new Random();

  static String msg(final String s) {
    return String.format("Hello from %s, it is %d and %s", s, System.currentTimeMillis(), String.valueOf(random.nextInt(60)));
  }

  public static void main(final String[] args) {

    final Client client = RxRedis.connect("localhost", 6379);
    final Writes<String> stringWrites = DefaultWrites.String();

    client.set("foo1", msg("foo1"), stringWrites);
    client.set("foo2", msg("foo2"), stringWrites);
    client.set("foo3", msg("foo3"), stringWrites);

    client.ping()
        .concatWith(client.get("foo1"))
        .concatWith(client.get("foo2"))
        .concatWith(client.get("foo3"))
        .subscribe(new Observer<RespType>() {
          @Override
          public void onCompleted() {
            client.shutdown();
          }

          @Override
          public void onError(final Throwable e) {
            e.printStackTrace();
          }

          @Override
          public void onNext(final RespType respType) {
            System.out.println("response = " + respType);
          }
        });

    RxRedis.await(client);
  }
}
