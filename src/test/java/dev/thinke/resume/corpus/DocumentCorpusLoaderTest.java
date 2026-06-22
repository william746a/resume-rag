package dev.thinke.resume.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.thinke.resume.corpus.DocumentCorpusConfig.ClasspathConfig;
import dev.thinke.resume.corpus.DocumentCorpusConfig.DocumentSourceConfig;
import dev.thinke.resume.corpus.DocumentCorpusConfig.GcsConfig;
import dev.thinke.resume.corpus.DocumentCorpusConfig.GcsSourceConfig;
import dev.thinke.resume.corpus.GcsDocumentReader.GcsObjectRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentCorpusLoaderTest {

  private DocumentCorpusConfig config;
  private ClasspathDocumentReader classpathReader;
  private GcsDocumentReader gcsReader;
  private DocumentCorpusLoader loader;

  @BeforeEach
  void setUp() {
    config = mock(DocumentCorpusConfig.class);
    classpathReader = new ClasspathDocumentReader();
    gcsReader = mock(GcsDocumentReader.class);
    loader = new DocumentCorpusLoader(config, classpathReader, gcsReader);
  }

  @Test
  void loadsMultipleClasspathDocuments() throws Exception {
    GcsConfig gcsConfig = mock(GcsConfig.class);
    when(config.gcs()).thenReturn(gcsConfig);
    when(gcsConfig.contextPrefixEnabled()).thenReturn(false);
    List<DocumentSourceConfig> sources = List.of(
        classpathSource("resume", DocumentRole.resume, "corpus-test/resume.md"),
        classpathSource("context-extra", DocumentRole.context, "corpus-test/context-extra.md"));
    when(config.sources()).thenReturn(sources);

    List<LoadedDocument> documents = loader.load();

    assertEquals(2, documents.size());
    assertTrue(documents.stream().anyMatch(d -> d.id().equals("resume")));
    assertTrue(documents.stream().anyMatch(d -> d.id().equals("context-extra")));
  }

  @Test
  void requiresExactlyOneResumeDocument() {
    GcsConfig gcsConfig = mock(GcsConfig.class);
    when(config.gcs()).thenReturn(gcsConfig);
    when(gcsConfig.contextPrefixEnabled()).thenReturn(false);
    DocumentSourceConfig contextOnly =
        classpathSource("notes", DocumentRole.context, "corpus-test/context-extra.md");
    when(config.sources()).thenReturn(List.of(contextOnly));

    assertThrows(IllegalStateException.class, loader::load);
  }

  @Test
  void rejectsDuplicateDocumentIds() {
    GcsConfig gcsConfig = mock(GcsConfig.class);
    when(config.gcs()).thenReturn(gcsConfig);
    when(gcsConfig.contextPrefixEnabled()).thenReturn(false);
    List<DocumentSourceConfig> sources = List.of(
        classpathSource("resume", DocumentRole.resume, "corpus-test/resume.md"),
        classpathSource("resume", DocumentRole.context, "corpus-test/context-extra.md"));
    when(config.sources()).thenReturn(sources);

    assertThrows(IllegalStateException.class, loader::load);
  }

  @Test
  void expandsGcsContextPrefixSkippingExplicitObjects() throws Exception {
    GcsConfig gcsConfig = mock(GcsConfig.class);
    when(config.gcs()).thenReturn(gcsConfig);
    when(gcsConfig.contextPrefixEnabled()).thenReturn(true);
    when(gcsConfig.contextPrefix()).thenReturn("context/");
    when(gcsConfig.bucket()).thenReturn(Optional.of("test-bucket"));

    DocumentSourceConfig resumeSource = gcsSource("resume", DocumentRole.resume, "test-bucket", "resume.md");
    when(config.sources()).thenReturn(List.of(resumeSource));
    when(gcsReader.readObject("test-bucket", "resume.md")).thenReturn("# Resume");
    when(gcsReader.listMarkdownObjects("test-bucket", "context/"))
        .thenReturn(List.of(
            new GcsObjectRef("test-bucket", "resume.md"),
            new GcsObjectRef("test-bucket", "context/faq.md")));
    when(gcsReader.readObject("test-bucket", "context/faq.md")).thenReturn("# FAQ");

    List<LoadedDocument> documents = loader.load();

    assertEquals(2, documents.size());
    assertTrue(documents.stream().anyMatch(d -> d.id().equals("faq")));
  }

  @Test
  void derivesIdFromGcsObjectBasename() {
    assertEquals("writing-samples", DocumentCorpusLoader.idFromObjectName("context/writing-samples.md"));
  }

  private static DocumentSourceConfig classpathSource(String id, DocumentRole role, String path) {
    DocumentSourceConfig source = mock(DocumentSourceConfig.class);
    ClasspathConfig classpath = mock(ClasspathConfig.class);
    when(source.id()).thenReturn(id);
    when(source.role()).thenReturn(role);
    when(source.source()).thenReturn(DocumentSourceType.classpath);
    when(source.classpath()).thenReturn(classpath);
    when(classpath.path()).thenReturn(Optional.of(path));
    return source;
  }

  private static DocumentSourceConfig gcsSource(String id, DocumentRole role, String bucket, String object) {
    DocumentSourceConfig source = mock(DocumentSourceConfig.class);
    GcsSourceConfig gcs = mock(GcsSourceConfig.class);
    when(source.id()).thenReturn(id);
    when(source.role()).thenReturn(role);
    when(source.source()).thenReturn(DocumentSourceType.gcs);
    when(source.gcs()).thenReturn(gcs);
    when(gcs.bucket()).thenReturn(Optional.of(bucket));
    when(gcs.object()).thenReturn(Optional.of(object));
    return source;
  }
}
