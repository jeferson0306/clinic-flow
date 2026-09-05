package dev.jefersonsiqueira.clinicflow.common;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A bounded, in-memory tail of recent error outcomes, for the admin
 * system-health page. Render's free tier is a single instance with no log
 * aggregation behind it — without this, "what just went wrong" means
 * finding and reading Render's own dashboard log stream by hand. Deliberately
 * thin, the same restraint {@link GlobalExceptionMapper}'s own log line
 * applies: status, exception type, path, trace id, never the value that
 * failed. Lost on every restart or redeploy — an acceptable tradeoff for a
 * demo-scale service, not a substitute for real log retention.
 */
@ApplicationScoped
public class RecentErrorsLog {

  private static final int CAPACITY = 200;

  public record Entry(Instant timestamp, int status, String exceptionType, String path, String traceId) {}

  private final Deque<Entry> entries = new ArrayDeque<>();

  public synchronized void record(Entry entry) {
    entries.addFirst(entry);
    while (entries.size() > CAPACITY) {
      entries.removeLast();
    }
  }

  /** Newest first. */
  public synchronized List<Entry> recent() {
    return List.copyOf(entries);
  }
}
