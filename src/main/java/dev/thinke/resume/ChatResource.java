package dev.thinke.resume;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/chat")
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private static final Logger LOG = Logger.getLogger(ChatResource.class);

    private static final int MAX_QUESTION_LENGTH = 500;

    @Inject
    ResumeAssistant resumeAssistant;

    @Inject
    GlobalDailyCap globalDailyCap;

    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @RateLimited(bucket = "chat", identityResolver = IpResolver.class)
    public Multi<String> chat(ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new BadRequestException("question is required");
        }
        if (request.question().length() > MAX_QUESTION_LENGTH) {
            throw new BadRequestException("question exceeds " + MAX_QUESTION_LENGTH + " characters");
        }
        if (!globalDailyCap.tryAcquire()) {
            throw new DailyCapExceededException(
                    "Daily question limit reached. Please ask again tomorrow.");
        }

        String question = request.question().trim();
        LOG.infof(
                "chat question_length=%d remaining_global_cap=%d question=%s",
                question.length(),
                globalDailyCap.remaining(),
                truncateForLog(question));

        return resumeAssistant.ask(question);
    }

    private static String truncateForLog(String question) {
        if (question.length() <= 200) {
            return question;
        }
        return question.substring(0, 200) + "…";
    }

    public record ChatRequest(String question) {}
}
