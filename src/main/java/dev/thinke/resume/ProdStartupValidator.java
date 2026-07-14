package dev.thinke.resume;

import dev.thinke.resume.corpus.DocumentCorpusConfig;
import dev.thinke.resume.corpus.DocumentSourceType;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProdStartupValidator {

    private static final Logger LOG = Logger.getLogger(ProdStartupValidator.class);

    @ConfigProperty(name = "quarkus.langchain4j.openai.api-key")
    String apiKey;

    @Inject
    DocumentCorpusConfig documentCorpusConfig;

    void onStart(@Observes StartupEvent event) {
        if (!ConfigUtils.isProfileActive("prod")) {
            return;
        }

        if (apiKey == null
                || apiKey.isBlank()
                || "local-dev-replace-me".equals(apiKey)) {
            throw new IllegalStateException(
                    "OPENROUTER_API_KEY must be set to a real key in production");
        }

        boolean hasGcsSource = documentCorpusConfig.sources().stream()
                .anyMatch(s -> s.source() == DocumentSourceType.gcs);
        boolean prefixEnabled = documentCorpusConfig.gcs().contextPrefixEnabled();
        if (hasGcsSource || prefixEnabled) {
            boolean defaultBucket =
                    documentCorpusConfig.gcs().bucket().filter(b -> !b.isBlank()).isPresent();
            boolean anySourceHasBucket = documentCorpusConfig.sources().stream()
                    .anyMatch(s -> s.gcs().bucket().filter(b -> !b.isBlank()).isPresent());
            if (!defaultBucket && !anySourceHasBucket) {
                throw new IllegalStateException(
                        "DOCUMENTS_GCS_BUCKET (or per-source gcs.bucket) must be set when using GCS in production");
            }
        }

        LOG.info("Production startup checks passed");
    }
}
