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

package com.example;

import rx.Observable;
import rx.redis.japi.Client;
import rx.redis.japi.RxRedis;

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
        .subscribe(r -> System.out.println("mixed = " + r));

    RxRedis.shutdown(client);
  }
}
