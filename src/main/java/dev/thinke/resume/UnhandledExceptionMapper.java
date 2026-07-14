package dev.thinke.resume;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Catch-all exception mapper to prevent stack traces and internal details from leaking in HTTP
 * responses. The original exception is still server-side logged for debugging.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnhandledExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Unhandled exception", exception);
        return Response.status(500)
                .entity("An unexpected error occurred. Please try again later.")
                .type("text/plain")
                .build();
    }
}
