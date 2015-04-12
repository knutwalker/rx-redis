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
import rx.redis.clients.RawClient;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;


public final class Client {

  private final RawClient raw;

  Client(final RawClient raw) {
    this.raw = raw;
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
    return raw.disconnect().map(x -> null);
  }

  public Observable<RespType> command(final ByteBuf cmd) {
    return raw.command(cmd);
  }

  public <A> Observable<RespType> command(final A cmd, final Writes<A> A) {
    return raw.command(cmd, A);
  }

  // ==============
  //  Key Commands
  // ==============


  public Observable<Long> del(final String... keys) {
    return raw.del(tsToSeq(keys)).map(toJLong);
  }

  public Observable<Boolean> exists(final String key) {
    return raw.exists(key).map(toJBool);
  }

  public Observable<Boolean> expire(final String key, final FiniteDuration expires) {
    return raw.expire(key, expires).map(toJBool);
  }

  public Observable<Boolean> expireAt(final String key, final Deadline deadline) {
    return raw.expireAt(key, deadline).map(toJBool);
  }

  public Observable<String> keys(final String pattern) {
    return raw.keys(pattern);
  }

  public Observable<Optional<String>> randomKey() {
    return raw.randomKey().map(optionFunc());
  }

  public Observable<Long> ttl(final String key) {
    return raw.ttl(key).map(toJLong);
  }

  // ================
  // String Commands
  // ================

  public <T> Observable<Optional<T>> getAs(final String key, final BytesReader<T> byteBufReader) {
    return raw.get(key, byteBufReader.asScalaReader()).map(optionFunc());
  }

  public Observable<Optional<String>> get(final String key) {
    return getAs(key, DefaultBytes.STRING);
  }

  public Observable<Optional<byte[]>> getBytes(final String key) {
    return getAs(key, DefaultBytes.BYTES);
  }

  public <T> Observable<Boolean> setAs(final String key, final T value, final BytesWriter<T> byteBufWriter) {
    return raw.set(key, value, byteBufWriter.asScalaWriter()).map(toJBool);
  }

  public Observable<Boolean> set(final String key, final String value) {
    return setAs(key, value, DefaultBytes.STRING);
  }

  public Observable<Boolean> set(final String key, final byte[] value) {
    return setAs(key, value, DefaultBytes.BYTES);
  }

  public <T> Observable<Boolean> setEx(final String key, final T value, final FiniteDuration expires, final BytesWriter<T> byteBufWriter) {
    return raw.setEx(key, value, expires, byteBufWriter.asScalaWriter()).map(toJBool);
  }

  public <T> Observable<Boolean> setNx(final String key, final T value, final BytesWriter<T> byteBufWriter) {
    return raw.setNx(key, value, byteBufWriter.asScalaWriter()).map(toJBool);
  }

  public Observable<Long> incr(final String key) {
    return raw.incr(key).map(toJLong);
  }

  public Observable<Long> incrBy(final String key, final long amount) {
    return raw.incrBy(key, amount).map(toJLong);
  }

  public Observable<Long> decr(final String key) {
    return raw.decr(key).map(toJLong);
  }

  public Observable<Long> decrBy(final String key, final long amount) {
    return raw.decrBy(key, amount).map(toJLong);
  }

  public <T> Observable<Optional<T>> mgetAs(final BytesReader<T> byteBufReader, final String... keys) {
    return raw.mget(tsToSeq(keys), byteBufReader.asScalaReader()).map(optionFunc());
  }

  public Observable<Optional<String>> mget(final String... keys) {
    return mgetAs(DefaultBytes.STRING, keys);
  }

  public Observable<Optional<byte[]>> mgetBytes(final String... keys) {
    return mgetAs(DefaultBytes.BYTES, keys);
  }

  @SafeVarargs
  public final <T> Observable<Boolean> msetAs(final BytesWriter<T> byteBufWriter, final Map.Entry<String, T>... items) {
    return raw.mset(tsMapToSeq(x -> Tuple2.apply(x.getKey(), x.getValue()), items), byteBufWriter.asScalaWriter()).map(toJBool);
  }

  @SafeVarargs
  public final <T> Observable<Boolean> msetAs(final BytesWriter<T> byteBufWriter, final Tuple2<String, T>... items) {
    return raw.mset(tsToSeq(items), byteBufWriter.asScalaWriter()).map(toJBool);
  }

  @SuppressWarnings("unchecked")
  public final <T> Observable<Boolean> msetAs(final BytesWriter<T> byteBufWriter, final Map<String, T> items) {
    final Map.Entry<String, T>[] entries = (Map.Entry<String, T>[]) items.entrySet().toArray();
    return msetAs(byteBufWriter, entries);
  }

  @SafeVarargs
  public final Observable<Boolean> mset(final Map.Entry<String, String>... items) {
    return msetAs(DefaultBytes.STRING, items);
  }

  @SafeVarargs
  public final Observable<Boolean> mset(final Tuple2<String, String>... items) {
    return msetAs(DefaultBytes.STRING, items);
  }

  @SuppressWarnings("unchecked")
  public final Observable<Boolean> mset(final Map<String, String> items) {
    final Map.Entry<String, String>[] entries = (Map.Entry<String, String>[]) items.entrySet().toArray();
    return msetAs(DefaultBytes.STRING, entries);
  }

  @SafeVarargs
  public final Observable<Boolean> msetBytes(final Map.Entry<String, byte[]>... items) {
    return msetAs(DefaultBytes.BYTES, items);
  }

  @SafeVarargs
  public final Observable<Boolean> msetBytes(final Tuple2<String, byte[]>... items) {
    return msetAs(DefaultBytes.BYTES, items);
  }

  @SuppressWarnings("unchecked")
  public final Observable<Boolean> msetBytes(final Map<String, byte[]> items) {
    final Map.Entry<String, byte[]>[] entries = (Map.Entry<String, byte[]>[]) items.entrySet().toArray();
    return msetAs(DefaultBytes.BYTES, entries);
  }

  public Observable<Long> strLen(final String key) {
    return raw.strLen(key).map(toJLong);
  }

  // ===============
  //  Hash Commands
  // ===============

  public <A> Observable<Optional<A>> hgetAs(final String key, final String field, final BytesReader<A> byteBufReader) {
    return raw.hget(key, field, byteBufReader.asScalaReader()).map(optionFunc());
  }

  public Observable<Optional<String>> hget(final String key, final String field) {
    return hgetAs(key, field, DefaultBytes.STRING);
  }

  public Observable<Optional<byte[]>> hgetBytes(final String key, final String field) {
    return hgetAs(key, field, DefaultBytes.BYTES);
  }

  public <A> Observable<Tuple2<String, A>> hgetAllAs(final String key, final BytesReader<A> byteBufReader) {
    return raw.hgetAll(key, byteBufReader.asScalaReader());
  }

  public Observable<Map.Entry<String, String>> hgetAll(final String key) {
    return raw.hgetAll(key, DefaultBytes.STRING.asScalaReader()).map(entryFunc());
  }

  public Observable<Map.Entry<String, byte[]>> hgetAllBytes(final String key) {
    return raw.hgetAll(key, DefaultBytes.BYTES.asScala()).map(entryFunc());
  }


  // =====================
  //  Connection Commands
  // =====================

  public Observable<String> ping() {
    return raw.ping();
  }

  public <T> Observable<T> echo(final T message, final BytesFormat<T> byteBufFormat) {
    return raw.echo(message, byteBufFormat.asScala());
  }

  public Observable<String> echo(final String message) {
    return echo(message, DefaultBytes.STRING);
  }

  public Observable<byte[]> echo(final byte[] message) {
    return echo(message, DefaultBytes.BYTES);
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
