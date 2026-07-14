package dev.thinke.resume;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DailyCapExceptionMapper implements ExceptionMapper<DailyCapExceededException> {

    @Override
    public Response toResponse(DailyCapExceededException exception) {
        return Response.status(429)
                .entity(exception.getMessage())
                .type("text/plain")
                .build();
    }
}
