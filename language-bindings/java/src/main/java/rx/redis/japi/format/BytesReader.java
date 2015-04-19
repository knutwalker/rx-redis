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
import rx.redis.serialization.ByteBufReader;
import rx.redis.serialization.ByteBufReader$;
import scala.util.Either;

import java.util.function.Function;
import java.util.function.Supplier;

import static scala.compat.java8.JFunction.func;

@FunctionalInterface
public interface BytesReader<T> {

  T fromByteBuf(ByteBuf bb);

  default <U> BytesReader<U> map(final Function<T, U> f) {
    return fromScala(this.asScalaReader().map(func(f::apply)));
  }

  default <U> BytesPairReader<T, U> andRead(final Supplier<BytesReader<U>> f) {
    return BytesPairReader.fromScala(this.asScalaReader().andRead(func(() -> f.get().asScalaReader())));
  }

  // TODO: EitherReader
  default <U> BytesReader<Either<T, U>> orRead(final Supplier<BytesReader<U>> f) {
    return fromScala(this.asScalaReader().orRead(func(() -> f.get().asScalaReader())));
  }

  default ByteBufReader<T> asScalaReader() {
    return ByteBufReader$.MODULE$.apply(func(this::fromByteBuf));
  }

  static <T> BytesReader<T> fromScala(final ByteBufReader<T> delegate) {
    return new FromScalaReader<>(delegate);
  }

  final class FromScalaReader<T> implements BytesReader<T> {
    private final ByteBufReader<T> delegate;

    FromScalaReader(final ByteBufReader<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T fromByteBuf(final ByteBuf bb) {
      return delegate.fromByteBuf(bb);
    }

    @Override
    public ByteBufReader<T> asScalaReader() {
      return delegate;
    }
  }
}
