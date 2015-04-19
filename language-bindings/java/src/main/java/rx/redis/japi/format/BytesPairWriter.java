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
import scala.Tuple2;

import java.util.function.Function;

@FunctionalInterface
public interface BytesPairWriter<T, U> extends BytesWriter<Tuple2<T, U>> {

  ByteBuf toByteBuf(ByteBuf bb, T value1, U value2);

  @Override
  default ByteBuf toByteBuf(final ByteBuf bb, final Tuple2<T, U> value) {
    return toByteBuf(bb, value._1(), value._2());
  }

  default <V> BytesWriter<V> contramap(final Function<V, T> f, final Function<V, U> g) {
    return contramap(x -> Tuple2.apply(f.apply(x), g.apply(x)));
  }

  static <T, U> BytesPairWriter<T, U> fromScala(final ByteBufWriter<Tuple2<T, U>> delegate) {
    return new FromScalaPairWriter<>(delegate);
  }

  final class FromScalaPairWriter<T, U> implements BytesPairWriter<T, U> {
    private final ByteBufWriter<Tuple2<T, U>> delegate;

    FromScalaPairWriter(final ByteBufWriter<Tuple2<T, U>> delegate) {
      this.delegate = delegate;
    }

    @Override
    public ByteBuf toByteBuf(final ByteBuf bb, final T value1, final U value2) {
      return delegate.toByteBuf(bb, Tuple2.apply(value1, value2));
    }

    @Override
    public ByteBuf toByteBuf(final ByteBuf bb, final Tuple2<T, U> value) {
      return delegate.toByteBuf(bb, value);
    }

    @Override
    public ByteBufWriter<Tuple2<T, U>> asScalaWriter() {
      return delegate;
    }
  }
}
