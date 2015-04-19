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

import rx.redis.japi.format.BytesFormat;
import rx.redis.serialization.ByteBufFormat$;

import static scala.compat.java8.JFunction.func;


@SuppressWarnings("UnusedDeclaration")
public final class DefaultBytes {

  private DefaultBytes() {}

  public final static BytesFormat<String> STRING =
      BytesFormat.fromScala(ByteBufFormat$.MODULE$.formatString());

  public final static BytesFormat<String> FRAMELESS_STRING =
      BytesFormat.fromScala(ByteBufFormat$.MODULE$.formatFramelessString());

  public final static BytesFormat<byte[]> BYTES =
      BytesFormat.fromScala(ByteBufFormat$.MODULE$.formatByteArray());

  public final static BytesFormat<byte[]> FRAMELESS_BYTES =
      BytesFormat.fromScala(ByteBufFormat$.MODULE$.formatFramelessByteArray());

  public final static BytesFormat<Integer> INT =
      BytesFormat.fromScala(ByteBufFormat$.MODULE$.formatInt()
          .xmap(func(x -> (Integer) x), func(x -> x)));

  public final static BytesFormat<Long> LONG =
      BytesFormat.fromScala(ByteBufFormat$.MODULE$.formatLong()
          .xmap(func(x -> (Long) x), func(x -> x)));
}
