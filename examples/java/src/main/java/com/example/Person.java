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

package com.example;

import rx.redis.japi.BytesFormat;
import rx.redis.japi.DefaultBytes;

public final class Person {

  private final String name;
  private final int age;

  public Person(final String name, final int age) {
    this.name = name;
    this.age = age;
  }

  public static Person of(final int age, final String name) {
    return new Person(name, age);
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

  public static final BytesFormat<Person> BYTES_FORMAT =
      DefaultBytes.INT.and(DefaultBytes.STRING)
          .xmap(Person::of, Person::getAge, Person::getName);
}
