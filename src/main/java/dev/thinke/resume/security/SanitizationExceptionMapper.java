package dev.thinke.resume.security;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SanitizationExceptionMapper implements ExceptionMapper<SanitizationException> {

    @Override
    public Response toResponse(SanitizationException exception) {
        return Response.status(400)
                .entity(exception.getMessage())
                .type("text/plain")
                .build();
    }
}
