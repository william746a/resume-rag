package dev.thinke.resume;

import io.quarkiverse.bucket4j.runtime.RateLimitException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RateLimitExceptionMapper implements ExceptionMapper<RateLimitException> {

    @Override
    public Response toResponse(RateLimitException exception) {
        return Response.status(429)
                .entity("Too many requests. Please try again later.")
                .type("text/plain")
                .build();
    }
}
