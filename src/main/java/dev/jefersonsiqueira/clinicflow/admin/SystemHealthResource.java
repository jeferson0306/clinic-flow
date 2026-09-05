package dev.jefersonsiqueira.clinicflow.admin;

import dev.jefersonsiqueira.clinicflow.common.RecentErrorsLog;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * ADMIN-only, deliberately separate from {@code /q/health}: that endpoint is
 * probed unauthenticated by Render itself and answers "is this instance
 * alive", while this answers "what has gone wrong recently" — a question
 * only an operator should get to ask, and one {@code /q/health} was never
 * designed to answer.
 */
@Path("/v1/admin/recent-errors")
@Tag(name = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@RunOnVirtualThread
public class SystemHealthResource {

  @Inject RecentErrorsLog recentErrorsLog;

  @GET
  @Operation(
      summary = "Recent error outcomes, newest first",
      description =
          "An in-memory tail (last 200), not a real log store — lost on every restart or "
              + "redeploy. For anything older, or for the full request log, see Render's own "
              + "dashboard for this service.")
  public List<RecentErrorsLog.Entry> recentErrors() {
    return recentErrorsLog.recent();
  }
}
