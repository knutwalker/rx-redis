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

package rx.redis.japi.format;

import io.netty.buffer.ByteBuf;
import rx.redis.serialization.ByteBufFormat;
import rx.redis.serialization.ByteBufFormat$;
import rx.redis.serialization.ByteBufReader;
import rx.redis.serialization.ByteBufWriter;
import scala.util.Either;

import java.util.Objects;
import java.util.function.Function;

import static scala.collection.JavaConverters.iterableAsScalaIterableConverter;
import static scala.compat.java8.JFunction.func;

public interface BytesFormat<T> extends BytesReader<T>, BytesWriter<T> {

  default <U> BytesFormat<U> xmap(final Function<T, U> f, final Function<U, T> g) {
    return fromScala(this.asScala().xmap(func(f::apply), func(g::apply)));
  }

  default <U> BytesFormat<U> xflatMap(final Function<T, U> f, final Function<U, Iterable<T>> g) {
    final ByteBufFormat<U> xByteBufFormat = this.asScala().xflatMap(
        func(f::apply),
        func(x -> iterableAsScalaIterableConverter(g.apply(x)).asScala()));
    return fromScala(xByteBufFormat);
  }

  default <U> BytesPairFormat<T, U> and(final BytesFormat<U> f) {
    return BytesPairFormat.fromScala(this.asScala().and(f.asScala()));
  }

  // TODO: EitherFormat
  default <U> BytesFormat<Either<T, U>> or(final BytesFormat<U> f) {
    return fromScala(this.asScala().or(f.asScala()));
  }

  default ByteBufFormat<T> asScala() {
    return ByteBufFormat$.MODULE$.from(asScalaReader(), asScalaWriter());
  }

  static <A> BytesFormat<A> fromScala(final ByteBufFormat<A> delegate) {
    return new FromScalaFormat<>(delegate);
  }

  static <A> BytesFormat<A> fromReaderAndWriter(final BytesReader<A> reader, final BytesWriter<A> writer) {
    return new BytesFormat<A>() {
      @Override
      public A fromByteBuf(final ByteBuf bb) {
        return reader.fromByteBuf(bb);
      }

      @Override
      public ByteBuf toByteBuf(final ByteBuf bb, final A value) {
        return writer.toByteBuf(bb, value);
      }
    };
  }

  final class FromScalaFormat<T> implements BytesFormat<T> {
    private final ByteBufFormat<T> delegate;

    FromScalaFormat(final ByteBufFormat<T> delegate) {
      this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public T fromByteBuf(final ByteBuf bb) {
      return delegate.fromByteBuf(bb);
    }

    @Override
    public ByteBuf toByteBuf(final ByteBuf bb, final T value) {
      return delegate.toByteBuf(bb, value);
    }

    @Override
    public ByteBufFormat<T> asScala() {
      return delegate;
    }

    @Override
    public ByteBufReader<T> asScalaReader() {
      return delegate;
    }

    @Override
    public ByteBufWriter<T> asScalaWriter() {
      return delegate;
    }
  }
}
