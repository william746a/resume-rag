package dev.thinke.resume;

import dev.thinke.resume.security.QuestionSanitizer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/chat")
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatResource {

    private static final Logger LOG = Logger.getLogger(ChatResource.class);

    @Inject
    ResumeAssistant resumeAssistant;

    @Inject
    GlobalDailyCap globalDailyCap;

    @Inject
    QuestionSanitizer questionSanitizer;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "resume.api.max-question-length")
    int maxQuestionLength;

    private Counter requestCounter;
    private Counter errorCounter;
    private Timer chatTimer;

    @PostConstruct
    void initMetrics() {
        requestCounter = meterRegistry.counter("resume.chat.requests");
        errorCounter = meterRegistry.counter("resume.chat.errors");
        chatTimer = meterRegistry.timer("resume.chat.duration");
    }

    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @RateLimited(bucket = "chat", identityResolver = IpResolver.class)
    public Multi<String> chat(ChatRequest request) {
        requestCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            if (request.question() == null || request.question().isBlank()) {
                throw new BadRequestException("question is required");
            }
            if (request.question().length() > maxQuestionLength) {
                throw new BadRequestException(
                        "question exceeds " + maxQuestionLength + " characters");
            }

            String question = request.question().trim();
            questionSanitizer.check(question);

            if (!globalDailyCap.tryAcquire()) {
                throw new DailyCapExceededException(
                        "Daily question limit reached. Please ask again tomorrow.");
            }

            LOG.infof(
                    "chat question_length=%d remaining_global_cap=%d question=%s",
                    question.length(),
                    globalDailyCap.remaining(),
                    truncateForLog(question));

            Multi<String> answer = resumeAssistant.ask(question);
            return answer.onTermination().invoke(() -> sample.stop(chatTimer));
        } catch (RuntimeException e) {
            errorCounter.increment();
            sample.stop(chatTimer);
            throw e;
        }
    }

    private static String truncateForLog(String question) {
        if (question.length() <= 200) {
            return question;
        }
        return question.substring(0, 200) + "…";
    }

    public record ChatRequest(String question) {}
}
