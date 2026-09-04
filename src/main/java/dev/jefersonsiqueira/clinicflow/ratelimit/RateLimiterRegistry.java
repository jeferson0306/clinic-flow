package dev.jefersonsiqueira.clinicflow.ratelimit;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One bucket per client address, in memory — not Redis. The work being
 * protected (a JPA insert, a call to brdoc) is already inexpensive relative
 * to a network round trip; adding one to guard it would cost more than the
 * thing it protects, the same reasoning brdoc's own README gives for not
 * reaching for a cache.
 *
 * Buckets for addresses that stop sending requests are swept periodically —
 * without that, the map grows for the lifetime of the process.
 */
@ApplicationScoped
public class RateLimiterRegistry {

  private record Entry(TokenBucket bucket, AtomicLong lastSeenMillis) {}

  @Inject RateLimitConfig config;

  private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
  private ScheduledExecutorService sweeper;

  @PostConstruct
  void start() {
    sweeper = Executors.newSingleThreadScheduledExecutor(RateLimiterRegistry::daemonThread);
    sweeper.scheduleAtFixedRate(this::sweep, 1, 1, TimeUnit.MINUTES);
  }

  @PreDestroy
  void stop() {
    sweeper.shutdownNow();
  }

  public boolean allow(String address) {
    Entry entry =
        buckets.computeIfAbsent(
            address,
            ignored -> new Entry(new TokenBucket(config.burst(), config.requestsPerSecond()), new AtomicLong()));
    entry.lastSeenMillis().set(System.currentTimeMillis());
    synchronized (entry.bucket()) {
      return entry.bucket().tryConsume();
    }
  }

  private void sweep() {
    long cutoff = System.currentTimeMillis() - config.idleBefore().toMillis();
    int before = buckets.size();
    buckets.entrySet().removeIf(entry -> entry.getValue().lastSeenMillis().get() < cutoff);
    int evicted = before - buckets.size();
    if (evicted > 0) {
      Log.debugf("rate limiter: evicted %d idle client(s), %d remaining", evicted, buckets.size());
    }
  }

  private static Thread daemonThread(Runnable task) {
    Thread thread = new Thread(task, "rate-limiter-sweeper");
    thread.setDaemon(true);
    return thread;
  }
}
