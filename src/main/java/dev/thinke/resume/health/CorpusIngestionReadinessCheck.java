package dev.thinke.resume.health;

import dev.thinke.resume.RagBootstrap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class CorpusIngestionReadinessCheck implements HealthCheck {

    @Inject
    RagBootstrap ragBootstrap;

    @Override
    public HealthCheckResponse call() {
        if (ragBootstrap.isCorpusReady()) {
            return HealthCheckResponse.up("corpus-ingestion");
        }
        return HealthCheckResponse.down("corpus-ingestion");
    }
}
