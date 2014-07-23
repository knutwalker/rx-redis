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
