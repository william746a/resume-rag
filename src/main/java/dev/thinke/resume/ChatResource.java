package dev.thinke.resume;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/chat")
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private static final int MAX_QUESTION_LENGTH = 500;

    @Inject
    ResumeAssistant resumeAssistant;

    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> chat(ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new BadRequestException("question is required");
        }
        if (request.question().length() > MAX_QUESTION_LENGTH) {
            throw new BadRequestException("question exceeds " + MAX_QUESTION_LENGTH + " characters");
        }
        return resumeAssistant.ask(request.question());
    }

    public record ChatRequest(String question) {}
}
