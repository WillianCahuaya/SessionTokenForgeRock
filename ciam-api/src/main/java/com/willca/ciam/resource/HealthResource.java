package com.willca.ciam.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final Logger LOG =
            Logger.getLogger(HealthResource.class);

    @GET
    public Response health() {
        LOG.info("Health endpoint called");

        return Response.ok(Map.of(
                "service", "ciam-api",
                "status", "UP",
                "timestamp", Instant.now().toString()
        )).build();
    }
}

