package dev.thinke.resume.corpus;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MultiDocCorpusIT.MultiDocProfile.class)
class MultiDocCorpusIT {

  @Inject
  DocumentCorpusLoader documentCorpusLoader;

  @Test
  void loadsResumeAndContextDocumentsFromClasspath() throws Exception {
    var documents = documentCorpusLoader.load();

    assertTrue(documents.size() >= 2);
    assertTrue(documents.stream().anyMatch(d -> d.id().equals("resume")));
    assertTrue(documents.stream().anyMatch(d -> d.id().equals("context-extra")));
  }

  public static class MultiDocProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.ofEntries(
          Map.entry("documents.sources[0].id", "resume"),
          Map.entry("documents.sources[0].role", "resume"),
          Map.entry("documents.sources[0].source", "classpath"),
          Map.entry("documents.sources[0].classpath.path", "corpus-test/resume.md"),
          Map.entry("documents.sources[1].id", "context-extra"),
          Map.entry("documents.sources[1].role", "context"),
          Map.entry("documents.sources[1].source", "classpath"),
          Map.entry("documents.sources[1].classpath.path", "corpus-test/context-extra.md"),
          Map.entry("documents.gcs.context-prefix-enabled", "false"));
    }
  }
}
