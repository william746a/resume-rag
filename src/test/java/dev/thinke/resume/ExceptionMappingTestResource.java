package dev.thinke.resume;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/test")
public class ExceptionMappingTestResource {

    @GET
    @Path("/throw")
    @Produces(MediaType.TEXT_PLAIN)
    public String throwUnhandled() {
        throw new RuntimeException("Deliberate unhandled exception for testing");
    }
}
