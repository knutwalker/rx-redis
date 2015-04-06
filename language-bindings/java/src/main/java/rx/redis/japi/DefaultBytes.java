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

import rx.redis.serialization.BytesFormat;
import rx.redis.serialization.BytesFormat$;
import scala.concurrent.duration.Deadline;
import scala.concurrent.duration.FiniteDuration;


@SuppressWarnings("UnusedDeclaration")
public final class DefaultBytes {

  private DefaultBytes() {}

  public final static BytesFormat<String> STRING_BYTES_FORMAT = BytesFormat$.MODULE$.StringBytes();

  public final static BytesFormat<byte[]> BYTES_BYTES_FORMAT = BytesFormat$.MODULE$.ByteArrayBytes();

  public final static BytesFormat<Long> LONG_BYTES_FORMAT = BytesFormat$.MODULE$.JLongBytes();

  public final static BytesFormat<FiniteDuration> DURATION_BYTES_FORMAT = BytesFormat$.MODULE$.DurationBytes();

  public final static BytesFormat<Deadline> DEADLINE_BYTES_FORMAT = BytesFormat$.MODULE$.DeadlineBytes();
}
