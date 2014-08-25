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
import rx.redis.resp.RespType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class AdvancedExample {

  public static void main(final String[] args) {

    final Client client = RxRedis.connect("localhost", 6379);

    final RespType serverInfoCommand = RxRedis.command("INFO SERVER");
    final RespType clientListCommand = RxRedis.command("CLIENT LIST");

    final List<String> results =
        client.command(serverInfoCommand)
            .mergeWith(client.command(clientListCommand))
            .map(Object::toString)
            .toList().toBlocking().first();

    System.out.println("results = " + results);

    final Map<String, String> map = new HashMap<>();

    client.hgetAll("qux")
        .forEach(e -> map.put(e.getKey(), e.getValue()));

    System.out.println("map = " + map);

    RxRedis.shutdown(client);
  }
}
