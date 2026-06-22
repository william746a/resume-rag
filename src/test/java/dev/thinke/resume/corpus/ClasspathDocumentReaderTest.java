package dev.thinke.resume.corpus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClasspathDocumentReaderTest {

  private final ClasspathDocumentReader reader = new ClasspathDocumentReader();

  @Test
  void readsClasspathResource() throws Exception {
    String content = reader.read("corpus-test/resume.md");
    assertTrue(content.contains("Test Resume"));
  }

  @Test
  void rejectsMissingResource() {
    assertThrows(IllegalStateException.class, () -> reader.read("corpus-test/missing.md"));
  }

  @Test
  void rejectsEmptyResource() {
    assertThrows(IllegalStateException.class, () -> reader.read("corpus-test/empty.md"));
  }

  @Test
  void stripsLeadingSlashFromPath() throws Exception {
    String content = reader.read("/corpus-test/resume.md");
    assertFalse(content.isBlank());
  }
}
