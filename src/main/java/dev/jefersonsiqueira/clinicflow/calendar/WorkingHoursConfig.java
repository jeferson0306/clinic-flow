package dev.jefersonsiqueira.clinicflow.calendar;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * One fixed working window for every doctor, not a per-doctor schedule table
 * — nothing in the roadmap has asked for individual hours yet, and a config
 * value that is trivial to promote to a database column later is a better
 * default than a schema built ahead of a real need for it.
 *
 * A {@code @ConfigMapping} interface rather than a class with fields and a
 * constructor: SmallRye Config generates the implementation, so there is
 * nothing here to get out of sync with application.properties by hand.
 */
@ConfigMapping(prefix = "clinic.calendar")
public interface WorkingHoursConfig {

  @WithDefault("08:00")
  LocalTime start();

  @WithDefault("18:00")
  LocalTime end();

  @WithDefault("America/Sao_Paulo")
  ZoneId zoneId();
}
