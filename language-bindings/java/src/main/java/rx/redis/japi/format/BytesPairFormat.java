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
import rx.redis.serialization.ByteBufReader;
import rx.redis.serialization.ByteBufWriter;
import scala.Tuple2;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface BytesPairFormat<T, U> extends BytesFormat<Tuple2<T, U>> {

  ByteBuf toByteBuf(ByteBuf bb, T value1, U value2);

  Map.Entry<T, U> entryFromByteBuf(ByteBuf bb);

  @Override
  default Tuple2<T, U> fromByteBuf(final ByteBuf bb) {
    final Map.Entry<T, U> entry = entryFromByteBuf(bb);
    return Tuple2.apply(entry.getKey(), entry.getValue());
  }

  @Override
  default ByteBuf toByteBuf(final ByteBuf bb, final Tuple2<T, U> value) {
    return toByteBuf(bb, value._1(), value._2());
  }

  default <V> BytesFormat<V> xmap(final BiFunction<T, U, V> f, final Function<V, T> g, final Function<V, U> h) {
    return xmap(tpl -> f.apply(tpl._1(), tpl._2()), v -> Tuple2.apply(g.apply(v), h.apply(v)));
  }

  static <T, U> BytesPairFormat<T, U> fromScala(final ByteBufFormat<Tuple2<T, U>> delegate) {
    return new FromScalaPairFormat<>(delegate);
  }

  final class FromScalaPairFormat<T, U> implements BytesPairFormat<T, U> {
    private final ByteBufFormat<Tuple2<T, U>> delegate;

    FromScalaPairFormat(final ByteBufFormat<Tuple2<T, U>> delegate) {
      this.delegate = delegate;
    }

    @Override
    public ByteBuf toByteBuf(final ByteBuf bb, final T value1, final U value2) {
      return delegate.toByteBuf(bb, Tuple2.apply(value1, value2));
    }

    @Override
    public Map.Entry<T, U> entryFromByteBuf(final ByteBuf bb) {
      final Tuple2<T, U> tuple = delegate.fromByteBuf(bb);
      return new AbstractMap.SimpleImmutableEntry<>(tuple._1(), tuple._2());
    }

    @Override
    public Tuple2<T, U> fromByteBuf(final ByteBuf bb) {
      return delegate.fromByteBuf(bb);
    }

    @Override
    public ByteBuf toByteBuf(final ByteBuf bb, final Tuple2<T, U> value) {
      return delegate.toByteBuf(bb, value);
    }

    @Override
    public ByteBufFormat<Tuple2<T, U>> asScala() {
      return delegate;
    }

    @Override
    public ByteBufReader<Tuple2<T, U>> asScalaReader() {
      return delegate;
    }

    @Override
    public ByteBufWriter<Tuple2<T, U>> asScalaWriter() {
      return delegate;
    }
  }
}
