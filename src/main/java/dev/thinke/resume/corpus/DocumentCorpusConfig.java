package dev.thinke.resume.corpus;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "documents")
public interface DocumentCorpusConfig {

    GcsConfig gcs();

    List<DocumentSourceConfig> sources();

    interface GcsConfig {
        Optional<String> bucket();

        @WithDefault("context/")
        String contextPrefix();

        @WithDefault("false")
        boolean contextPrefixEnabled();
    }

    interface DocumentSourceConfig {
        String id();

        DocumentRole role();

        DocumentSourceType source();

        ClasspathConfig classpath();

        GcsSourceConfig gcs();
    }

    interface ClasspathConfig {
        Optional<String> path();
    }

    interface GcsSourceConfig {
        Optional<String> bucket();

        Optional<String> object();
    }
}
