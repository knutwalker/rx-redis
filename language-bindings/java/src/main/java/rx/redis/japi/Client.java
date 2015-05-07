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

import io.netty.buffer.ByteBuf;
import rx.Observable;
import rx.functions.Func1;
import rx.redis.clients.GenericClient;
import rx.redis.japi.format.BytesFormat;
import rx.redis.japi.format.BytesReader;
import rx.redis.japi.format.BytesWriter;
import rx.redis.resp.RespType;
import rx.redis.serialization.Writes;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.concurrent.duration.Deadline;
import scala.concurrent.duration.FiniteDuration;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;


@SuppressWarnings("unused")
public final class Client {

  private final GenericClient underlying;

  Client(final GenericClient underlying) {
    this.underlying = underlying;
  }

  public static <T, U> Tuple2<T, U> pair(final T t, final U u) {
    return Tuple2.apply(t, u);
  }

  /**
   * Use {@link RxRedis#disconnect(Client)}.
   */
  @Deprecated
  public Observable<Void> shutdown() {
    return disconnect();
  }

  Observable<Void> disconnect() {
    return underlying.disconnect().map(x -> null);
  }

  public Observable<RespType> command(final ByteBuf cmd) {
    return underlying.sendCommand(cmd);
  }

  public <A> Observable<RespType> command(final A cmd, final Writes<A> A) {
    return underlying.command(cmd, A);
  }

  // ==============
  //  Key Commands
  // ==============


  public Observable<Long> del(final String... keys) {
    return underlying.del(tsToSeq(keys)).map(toJLong);
  }

  public Observable<Boolean> exists(final String key) {
    return underlying.exists(key).map(toJBool);
  }

  public Observable<Boolean> expire(final String key, final FiniteDuration expires) {
    return underlying.expire(key, expires).map(toJBool);
  }

  public Observable<Boolean> expireAt(final String key, final Deadline deadline) {
    return underlying.expireAt(key, deadline).map(toJBool);
  }

  public Observable<String> keys(final String pattern) {
    return underlying.keys(pattern);
  }

  public Observable<Optional<String>> randomKey() {
    return underlying.randomKey().map(optionFunc());
  }

  public Observable<Long> ttl(final String key) {
    return underlying.ttl(key).map(toJLong);
  }

  // ================
  // String Commands
  // ================

  public <T> Observable<Optional<T>> getAs(final String key, final BytesReader<T> bytesReader) {
    return underlying.get(key,
        Objects.requireNonNull(bytesReader, "bytesReader must not be null")
            .asScalaReader()).map(optionFunc());
  }

  public Observable<Optional<String>> get(final String key) {
    return getAs(key, DefaultBytes.FRAMELESS_STRING);
  }

  public Observable<Optional<byte[]>> getBytes(final String key) {
    return getAs(key, DefaultBytes.FRAMELESS_BYTES);
  }

  public <T> Observable<Boolean> setAs(final String key, final T value, final BytesWriter<T> bytesWriter) {
    return underlying.set(key, value,
        Objects.requireNonNull(bytesWriter, "bytesWriter must not be null")
            .asScalaWriter()).map(toJBool);
  }

  public Observable<Boolean> set(final String key, final String value) {
    return setAs(key, value, DefaultBytes.FRAMELESS_STRING);
  }

  public Observable<Boolean> set(final String key, final byte[] value) {
    return setAs(key, value, DefaultBytes.FRAMELESS_BYTES);
  }

  public <T> Observable<Boolean> setEx(final String key, final T value, final FiniteDuration expires, final BytesWriter<T> bytesWriter) {
    return underlying.setEx(key, value, expires,
        Objects.requireNonNull(bytesWriter, "bytesWriter must not be null")
            .asScalaWriter()).map(toJBool);
  }

  public <T> Observable<Boolean> setNx(final String key, final T value, final BytesWriter<T> bytesWriter) {
    return underlying.setNx(key, value,
        Objects.requireNonNull(bytesWriter, "bytesWriter must not be null")
            .asScalaWriter()).map(toJBool);
  }

  public Observable<Long> incr(final String key) {
    return underlying.incr(key).map(toJLong);
  }

  public Observable<Long> incrBy(final String key, final long amount) {
    return underlying.incrBy(key, amount).map(toJLong);
  }

  public Observable<Long> decr(final String key) {
    return underlying.decr(key).map(toJLong);
  }

  public Observable<Long> decrBy(final String key, final long amount) {
    return underlying.decrBy(key, amount).map(toJLong);
  }

  public <T> Observable<Optional<T>> mgetAs(final BytesReader<T> bytesReader, final String... keys) {
    return underlying.mget(tsToSeq(keys),
        Objects.requireNonNull(bytesReader, "bytesReader must not be null")
            .asScalaReader()).map(optionFunc());
  }

  public Observable<Optional<String>> mget(final String... keys) {
    return mgetAs(DefaultBytes.FRAMELESS_STRING, keys);
  }

  public Observable<Optional<byte[]>> mgetBytes(final String... keys) {
    return mgetAs(DefaultBytes.FRAMELESS_BYTES, keys);
  }

  @SafeVarargs
  public final <T> Observable<Boolean> msetAs(final BytesWriter<T> bytesWriter, final Map.Entry<String, T>... items) {
    return underlying.mset(tsMapToSeq(x -> Tuple2.apply(x.getKey(), x.getValue()), items),
        Objects.requireNonNull(bytesWriter, "bytesWriter must not be null")
            .asScalaWriter()).map(toJBool);
  }

  @SafeVarargs
  public final <T> Observable<Boolean> msetAs(final BytesWriter<T> bytesWriter, final Tuple2<String, T>... items) {
    return underlying.mset(tsToSeq(items),
        Objects.requireNonNull(bytesWriter, "bytesWriter must not be null")
            .asScalaWriter()).map(toJBool);
  }

  @SuppressWarnings("unchecked")
  public final <T> Observable<Boolean> msetAs(final BytesWriter<T> bytesWriter, final Map<String, T> items) {
    final Map.Entry<String, T>[] entries = (Map.Entry<String, T>[]) items.entrySet().toArray();
    return msetAs(bytesWriter, entries);
  }

  @SafeVarargs
  public final Observable<Boolean> mset(final Map.Entry<String, String>... items) {
    return msetAs(DefaultBytes.FRAMELESS_STRING, items);
  }

  @SafeVarargs
  public final Observable<Boolean> mset(final Tuple2<String, String>... items) {
    return msetAs(DefaultBytes.FRAMELESS_STRING, items);
  }

  @SuppressWarnings("unchecked")
  public final Observable<Boolean> mset(final Map<String, String> items) {
    final Map.Entry<String, String>[] entries = (Map.Entry<String, String>[]) items.entrySet().toArray();
    return msetAs(DefaultBytes.FRAMELESS_STRING, entries);
  }

  @SafeVarargs
  public final Observable<Boolean> msetBytes(final Map.Entry<String, byte[]>... items) {
    return msetAs(DefaultBytes.FRAMELESS_BYTES, items);
  }

  @SafeVarargs
  public final Observable<Boolean> msetBytes(final Tuple2<String, byte[]>... items) {
    return msetAs(DefaultBytes.FRAMELESS_BYTES, items);
  }

  @SuppressWarnings("unchecked")
  public final Observable<Boolean> msetBytes(final Map<String, byte[]> items) {
    final Map.Entry<String, byte[]>[] entries = (Map.Entry<String, byte[]>[]) items.entrySet().toArray();
    return msetAs(DefaultBytes.FRAMELESS_BYTES, entries);
  }

  public Observable<Long> strLen(final String key) {
    return underlying.strLen(key).map(toJLong);
  }

  // ===============
  //  Hash Commands
  // ===============

  public <A> Observable<Optional<A>> hgetAs(final String key, final String field, final BytesReader<A> bytesReader) {
    return underlying.hget(key, field,
        Objects.requireNonNull(bytesReader, "bytesReader must not be null")
            .asScalaReader()).map(optionFunc());
  }

  public Observable<Optional<String>> hget(final String key, final String field) {
    return hgetAs(key, field, DefaultBytes.FRAMELESS_STRING);
  }

  public Observable<Optional<byte[]>> hgetBytes(final String key, final String field) {
    return hgetAs(key, field, DefaultBytes.FRAMELESS_BYTES);
  }

  public <A> Observable<Tuple2<String, A>> hgetAllAs(final String key, final BytesReader<A> bytesReader) {
    return underlying.hgetAll(key,
        Objects.requireNonNull(bytesReader, "bytesReader must not be null")
            .asScalaReader());
  }

  public Observable<Map.Entry<String, String>> hgetAll(final String key) {
    return underlying.hgetAll(key, DefaultBytes.FRAMELESS_STRING.asScalaReader()).map(entryFunc());
  }

  public Observable<Map.Entry<String, byte[]>> hgetAllBytes(final String key) {
    return underlying.hgetAll(key, DefaultBytes.FRAMELESS_BYTES.asScala()).map(entryFunc());
  }


  // =====================
  //  Connection Commands
  // =====================

  public Observable<String> ping() {
    return underlying.ping();
  }

  public <T> Observable<T> echo(final T message, final BytesFormat<T> bytesFormat) {
    return underlying.echo(message, Objects.requireNonNull(bytesFormat, "bytesFormat must not be null").asScala());
  }

  public Observable<String> echo(final String message) {
    return echo(message, DefaultBytes.FRAMELESS_STRING);
  }

  public Observable<byte[]> echo(final byte[] message) {
    return echo(message, DefaultBytes.FRAMELESS_BYTES);
  }

  // =================

  @SafeVarargs
  private static <T> Seq<T> tsToSeq(final T... ts) {
    return JavaConverters.asScalaIteratorConverter(Stream.of(ts).iterator()).asScala().toVector();
  }

  @SafeVarargs
  private static <T, R> Seq<R> tsMapToSeq(final Function<? super T, ? extends R> mapper, final T... ts) {
    return JavaConverters.asScalaIteratorConverter(Stream.of(ts).map(mapper).iterator()).asScala().toVector();
  }

  private static final Func1<Object, Boolean> toJBool = b -> (Boolean) b;
  private static final Func1<Object, Long> toJLong = b -> (Long) b;

  private static <T> Func1<Option<T>, Optional<T>> optionFunc() {
    return tOption -> {
      if (tOption.isDefined()) {
        return Optional.of(tOption.get());
      } else {
        return Optional.empty();
      }
    };
  }

  private static <T, U> Func1<Tuple2<T, U>, Map.Entry<T, U>> entryFunc() {
    return tuple -> new AbstractMap.SimpleImmutableEntry<>(tuple._1(), tuple._2());
  }
}
