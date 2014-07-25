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

import rx.redis.japi.Client;
import rx.redis.japi.RxRedis;

import java.util.Arrays;

public final class CustomFormatExample {

  public static void main(final String[] args) {

    final Client client = RxRedis.connect("localhost", 6379);

    final Person knut = new Person("Knütsen \uD83D\uDCA9", 27);
    client.setAs("person:knut", knut, Person.BYTES_FORMAT);

    client.get("person:knut").forEach(s -> System.out.println("Person as string: " + s.orElse("")));
    client.getBytes("person:knut").forEach(b -> System.out.println("Person as bytes: " + b.map(Arrays::toString).orElse("")));
    client.getAs("person:knut", Person.BYTES_FORMAT)
        .doOnCompleted(client::shutdown)
        .forEach(p -> System.out.println("Person as Person: " + p.orElse(null)));

    RxRedis.await(client);
  }
}
