package dev.thinke.resume;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Mapper for {@link IllegalStateException}, typically startup/config failures. Keeps internal
 * details out of the response body while preserving server-side logs.
 */
@Provider
public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {

    private static final Logger LOG = Logger.getLogger(IllegalStateExceptionMapper.class);

    @Override
    public Response toResponse(IllegalStateException exception) {
        LOG.error("Illegal state / configuration error", exception);
        return Response.status(500)
                .entity("Service misconfiguration. Please contact the operator.")
                .type("text/plain")
                .build();
    }
}
