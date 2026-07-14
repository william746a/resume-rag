package dev.thinke.resume.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Lightweight prompt-injection / abuse guard for chat questions.
 *
 * <p>Uses a configurable regex blocklist. Matches are deterministic, case-insensitive, and logged
 * only as a SHA-256 hash so rejected question text is not retained in logs.
 */
@ApplicationScoped
public class QuestionSanitizer {

    private static final Logger LOG = Logger.getLogger(QuestionSanitizer.class);

    /** Reasonable default blocklist tuned for common resume-probe injections. */
    public static final String DEFAULT_BLOCKED_PATTERNS =
            "ignore (all )?(previous|prior) instructions,"
                    + "ignore (your|the) instructions,"
                    + "ignore (what )?(is|was) (said|written) above,"
                    + "disregard (all )?(previous|prior|the) instructions,"
                    + "system prompt,"
                    + "you are now,"
                    + "you are (an? )?(ai )?assistant,"
                    + "do anything now,"
                    + "developer mode,"
                    + "jailbreak,"
                    + "pretend (to be|you are),"
                    + "new instructions,"
                    + "reveal.*system,"
                    + "reveal.*prompt,"
                    + "show.*system prompt,"
                    + "show.*raw resume,"
                    + "output.*raw,"
                    + "forget (everything|your instructions),"
                    + "DAN";

    private final List<Pattern> blockedPatterns;

    @Inject
    public QuestionSanitizer(
            @ConfigProperty(name = "resume.sanitizer.blocked-patterns", defaultValue = DEFAULT_BLOCKED_PATTERNS)
            String blockedPatternsConfig) {
        this.blockedPatterns = Arrays.stream(blockedPatternsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    /**
     * Throws {@link SanitizationException} if the question matches a blocked pattern.
     *
     * @param question question text that has already passed length validation
     */
    public void check(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(question).find()) {
                LOG.warnf("Question rejected by sanitizer pattern=%s question_hash=%s", pattern.pattern(), hash(question));
                throw new SanitizationException("Question contains disallowed content.");
            }
        }
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
