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
import rx.redis.serialization.ByteBufReader;
import scala.Tuple2;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiFunction;

public interface BytesPairReader<T, U> extends BytesReader<Tuple2<T, U>> {

  Map.Entry<T, U> entryFromByteBuf(ByteBuf bb);

  @Override
  default Tuple2<T, U> fromByteBuf(final ByteBuf bb) {
    final Map.Entry<T, U> entry = entryFromByteBuf(bb);
    return Tuple2.apply(entry.getKey(), entry.getValue());
  }

  default <V> BytesReader<V> map(final BiFunction<T, U, V> f) {
    return map(tpl -> f.apply(tpl._1(), tpl._2()));
  }

  static <T, U> BytesPairReader<T, U> fromScala(final ByteBufReader<Tuple2<T, U>> delegate) {
    return new FromScalaPairReader<>(delegate);
  }

  final class FromScalaPairReader<T, U> implements BytesPairReader<T, U> {
    private final ByteBufReader<Tuple2<T, U>> delegate;

    FromScalaPairReader(final ByteBufReader<Tuple2<T, U>> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Map.Entry<T, U> entryFromByteBuf(final ByteBuf bb) {
      final Tuple2<T, U> tuple = delegate.fromByteBuf(bb).right().get();
      return new AbstractMap.SimpleImmutableEntry<>(tuple._1(), tuple._2());
    }

    @Override
    public Tuple2<T, U> fromByteBuf(final ByteBuf bb) {
      return delegate.fromByteBuf(bb).right().get();
    }

    @Override
    public ByteBufReader<Tuple2<T, U>> asScalaReader() {
      return delegate;
    }
  }
}
