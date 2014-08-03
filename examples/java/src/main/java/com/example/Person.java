/*
 * Copyright 2014 Paul Horn
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

package com.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import rx.redis.serialization.BytesFormat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Person {

  private final String name;
  private final int age;

  public Person(final String name, final int age) {
    this.name = name;
    this.age = age;
  }

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Person person = (Person) o;
    return age == person.age && name.equals(person.name);
  }

  @Override
  public int hashCode() {
    return 31 * name.hashCode() + age;
  }

  @Override
  public String toString() {
    return String.format("Person(%s, %d)", name, age);
  }

  public static final BytesFormat<Person> BYTES_FORMAT = PersonBytesFormat.INSTANCE;

  private static enum PersonBytesFormat implements BytesFormat<Person> {
    INSTANCE;

    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;
    private static final Charset charset = StandardCharsets.UTF_8;

    @Override
    public byte[] bytes(final Person value) {
      final byte[] nameBytes = value.getName().getBytes(charset);
      final int capacity = nameBytes.length + 2 * Integer.BYTES;
      final ByteBuf buffer = ALLOC.heapBuffer(capacity, capacity);
      buffer.writeInt(nameBytes.length);
      buffer.writeBytes(nameBytes);
      buffer.writeInt(value.getAge());
      final byte[] bytes = buffer.array();
      buffer.release();
      return bytes;
    }

    @Override
    public Person value(final byte[] bytes) {
      final ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
      final int length = buffer.readInt();
      final byte[] nameBytes = new byte[length];
      buffer.readBytes(nameBytes);
      final int age = buffer.readInt();
      buffer.release();
      return new Person(new String(nameBytes, charset), age);
    }
  }
}
