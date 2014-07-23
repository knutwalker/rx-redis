package rx.redis.japi;

import rx.Observable;
import rx.functions.Func1;
import rx.redis.client.RawClient;
import rx.redis.resp.DataType;
import rx.redis.resp.RespType;
import rx.redis.serialization.BytesFormat;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.concurrent.duration.FiniteDuration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;


@SuppressWarnings("UnusedDeclaration")
public final class Client {

  private final RawClient raw;

  public Client(final RawClient raw) {
    this.raw = raw;
  }

  public static <T, U> Tuple2<T, U> pair(final T t, final U u) {
    return Tuple2.apply(t, u);
  }

  public Observable<Void> shutdown() {
    return raw.shutdown().map(x -> null);
  }

  public Observable<Void> closedObservable() {
    return raw.closedObservable().map(x -> null);
  }

  public Observable<RespType> command(final DataType dataType) {
    return raw.command(dataType);
  }

  // ==============
  //  Key Commands
  // ==============


  public Observable<Long> del(final String... keys) {
    return raw.del(tsToSeq(keys)).map(toJLong);
  }

  // ================
  // String Commands
  // ================

  public <T> Observable<Optional<T>> getAs(final String key, final BytesFormat<T> bytesFormat) {
    return raw.get(key, bytesFormat).map(optionFunc());
  }

  public Observable<Optional<String>> get(final String key) {
    return getAs(key, DefaultBytes.STRING_BYTES_FORMAT);
  }

  public Observable<Optional<byte[]>> getBytes(final String key) {
    return getAs(key, DefaultBytes.BYTES_BYTES_FORMAT);
  }

  public <T> Observable<Boolean> setAs(final String key, final T value, final BytesFormat<T> bytesFormat) {
    return raw.set(key, value, bytesFormat).map(toJBool);
  }

  public Observable<Boolean> set(final String key, final String value) {
    return setAs(key, value, DefaultBytes.STRING_BYTES_FORMAT);
  }

  public Observable<Boolean> set(final String key, final byte[] value) {
    return setAs(key, value, DefaultBytes.BYTES_BYTES_FORMAT);
  }

  public <T> Observable<Boolean> setEx(final String key, final T value, final FiniteDuration expires, final BytesFormat<T> bytesFormat) {
    return raw.setEx(key, value, expires, bytesFormat).map(toJBool);
  }

  public <T> Observable<Boolean> setNx(final String key, final T value, final BytesFormat<T> bytesFormat) {
    return raw.setNx(key, value, bytesFormat).map(toJBool);
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

  public <T> Observable<Optional<T>> mgetAs(final BytesFormat<T> bytesFormat, final String... keys) {
    return raw.mget(tsToSeq(keys), bytesFormat).map(optionFunc());
  }

  public Observable<Optional<String>> mget(final String... keys) {
    return mgetAs(DefaultBytes.STRING_BYTES_FORMAT, keys);
  }

  public Observable<Optional<byte[]>> mgetBytes(final String... keys) {
    return mgetAs(DefaultBytes.BYTES_BYTES_FORMAT, keys);
  }

  @SafeVarargs
  public final <T> Observable<Boolean> msetAs(final BytesFormat<T> bytesFormat, final Map.Entry<String, T>... items) {
    return raw.mset(tsMapToSeq(x -> Tuple2.apply(x.getKey(), x.getValue()), items), bytesFormat).map(toJBool);
  }

  @SafeVarargs
  public final <T> Observable<Boolean> msetAs(final BytesFormat<T> bytesFormat, final Tuple2<String, T>... items) {
    return raw.mset(tsToSeq(items), bytesFormat).map(toJBool);
  }

  @SafeVarargs
  public final Observable<Boolean> mset(final Map.Entry<String, String>... items) {
    return msetAs(DefaultBytes.STRING_BYTES_FORMAT, items);
  }

  @SafeVarargs
  public final Observable<Boolean> mset(final Tuple2<String, String>... items) {
    return msetAs(DefaultBytes.STRING_BYTES_FORMAT, items);
  }

  @SafeVarargs
  public final Observable<Boolean> msetBytes(final Map.Entry<String, byte[]>... items) {
    return msetAs(DefaultBytes.BYTES_BYTES_FORMAT, items);
  }

  @SafeVarargs
  public final Observable<Boolean> msetBytes(final Tuple2<String, byte[]>... items) {
    return msetAs(DefaultBytes.BYTES_BYTES_FORMAT, items);
  }

  public Observable<Long> strLen(final String key) {
    return raw.strLen(key).map(toJLong);
  }

  // ===============
  //  Hash Commands
  // ===============



  // =====================
  //  Connection Commands
  // =====================

  public Observable<String> ping() {
    return raw.ping();
  }

  public <T> Observable<T> echo(final T message, final BytesFormat<T> bytesFormat) {
    return raw.echo(message, bytesFormat);
  }

  public Observable<String> echo(final String message) {
    return echo(message, DefaultBytes.STRING_BYTES_FORMAT);
  }

  public Observable<byte[]> echo(final byte[] message) {
    return echo(message, DefaultBytes.BYTES_BYTES_FORMAT);
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
}
