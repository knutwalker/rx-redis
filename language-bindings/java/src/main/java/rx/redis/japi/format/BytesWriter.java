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
import rx.redis.serialization.ByteBufWriter;
import rx.redis.serialization.ByteBufWriter$;
import scala.Tuple2;
import scala.util.Either;

import java.util.function.Function;
import java.util.function.Supplier;

import static scala.collection.JavaConverters.iterableAsScalaIterableConverter;
import static scala.compat.java8.JFunction.func;

@FunctionalInterface
public interface BytesWriter<T> {

  ByteBuf toByteBuf(ByteBuf bb, T value);

  default <U> BytesWriter<U> contramap(final Function<U, T> f) {
    return fromScala(this.asScalaWriter().contramap(func(f::apply)));
  }

  default <U> BytesWriter<U> contraflatMap(final Function<U, Iterable<T>> f) {
    return fromScala(this.asScalaWriter().contraflatMap(func(b ->
        iterableAsScalaIterableConverter(f.apply(b)).asScala())));
  }

  default <U> BytesPairWriter<T, U> andWrite(final BytesWriter<U> f) {
    final ByteBufWriter<Tuple2<T, U>> delegate =
        this.asScalaWriter().andWrite(f.asScalaWriter());
    return BytesPairWriter.fromScala(delegate);
  }

  // TODO: EitherWriter
  default <U> BytesWriter<Either<T, U>> orWrite(final Supplier<BytesWriter<U>> f) {
    return fromScala(this.asScalaWriter().orWrite(func(() -> f.get().asScalaWriter())));
  }

  default ByteBufWriter<T> asScalaWriter() {
    return ByteBufWriter$.MODULE$.apply(func(this::toByteBuf));
  }

  static <T> BytesWriter<T> fromScala(final ByteBufWriter<T> delegate) {
    return new FromScalaWriter<>(delegate);
  }

  final class FromScalaWriter<T> implements BytesWriter<T> {
    private final ByteBufWriter<T> delegate;

    FromScalaWriter(final ByteBufWriter<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public ByteBuf toByteBuf(final ByteBuf bb, final T value) {
      return delegate.toByteBuf(bb, value);
    }

    @Override
    public ByteBufWriter<T> asScalaWriter() {
      return delegate;
    }
  }
}
