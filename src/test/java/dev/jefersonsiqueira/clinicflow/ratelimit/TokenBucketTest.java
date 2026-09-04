package dev.jefersonsiqueira.clinicflow.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Plain unit test — no Quarkus context, no timers to fake: a bucket this small is real-time-safe to test directly. */
class TokenBucketTest {

  @Test
  void allowsExactlyTheBurstAndThenRejects() {
    var bucket = new TokenBucket(3, /* refillPerSecond= */ 0);

    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse();
  }

  @Test
  void refillsOverTimeUpToCapacity() throws InterruptedException {
    var bucket = new TokenBucket(2, /* refillPerSecond= */ 1000);
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse();

    // At 1000 tokens/second, 5ms is more than enough for at least one to refill —
    // generous on purpose, since real elapsed time under a test runner is never exact.
    Thread.sleep(5);

    assertThat(bucket.tryConsume()).isTrue();
  }

  @Test
  void neverExceedsCapacityEvenAfterALongIdlePeriod() throws InterruptedException {
    var bucket = new TokenBucket(2, /* refillPerSecond= */ 1000);
    Thread.sleep(50); // would refill hundreds of tokens without the capacity cap

    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse();
  }
}
