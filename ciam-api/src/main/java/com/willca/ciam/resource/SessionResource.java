
package com.willca.ciam.resource;

import com.willca.ciam.service.DataResponseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/ciam/session/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    private static final Logger LOG = Logger.getLogger(SessionResource.class);

    @Inject
    DataResponseService dataResponseService;

    @POST
    @Path("/register")
    @Consumes(MediaType.WILDCARD)
    public Response register(Map<String, Object> body, @Context HttpHeaders headers) {
        LOG.infof("Init Operation Register...");
        return dataResponseService.respond("register", body, headers);
    }

    @POST
    @Path("/delete")
    @Consumes(MediaType.WILDCARD)
    public Response delete(Map<String, Object> body, @Context HttpHeaders headers) {
        LOG.infof("Init Operation Delete...");
        return dataResponseService.respond("delete", body, headers);
    }


}