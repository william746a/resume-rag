package dev.thinke.resume.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorpusIngestionMetadataTest {

  @Test
  void splitSegmentsRetainDocumentMetadata() {
    LoadedDocument doc = new LoadedDocument(
        "writing-samples",
        DocumentRole.context,
        "classpath:context/writing-samples.md",
        "## Talk\n\nPresented RAG patterns on the JVM.\n");

    Metadata metadata = Metadata.from("document_id", doc.id())
        .put("document_role", doc.role().name())
        .put("source_uri", doc.sourceUri());
    Document document = Document.from(doc.markdown(), metadata);

    List<TextSegment> segments =
        DocumentSplitters.recursive(500, 50).split(document);

    assertEquals("writing-samples", segments.get(0).metadata().getString("document_id"));
    assertEquals("context", segments.get(0).metadata().getString("document_role"));
    assertEquals("classpath:context/writing-samples.md", segments.get(0).metadata().getString("source_uri"));
  }
}
