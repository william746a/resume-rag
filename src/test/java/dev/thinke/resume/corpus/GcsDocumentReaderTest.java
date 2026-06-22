package dev.thinke.resume.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GcsDocumentReaderTest {

  private Storage storage;
  private GcsDocumentReader reader;

  @BeforeEach
  void setUp() {
    storage = mock(Storage.class);
    reader = new GcsDocumentReader(storage);
  }

  @Test
  void readsGcsObject() {
    Blob blob = mock(Blob.class);
    when(storage.get("bucket", "resume.md")).thenReturn(blob);
    when(blob.exists()).thenReturn(true);
    when(blob.getContent()).thenReturn("# Resume".getBytes(StandardCharsets.UTF_8));

    String content = reader.readObject("bucket", "resume.md");

    assertEquals("# Resume", content);
  }

  @Test
  void rejectsMissingGcsObject() {
    when(storage.get("bucket", "missing.md")).thenReturn(null);

    assertThrows(IllegalStateException.class, () -> reader.readObject("bucket", "missing.md"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void listsMarkdownObjectsUnderPrefix() {
    Blob markdown = mock(Blob.class);
    Blob directory = mock(Blob.class);
    when(markdown.isDirectory()).thenReturn(false);
    when(markdown.getName()).thenReturn("context/faq.md");
    when(directory.isDirectory()).thenReturn(true);

    Page<Blob> page = mock(Page.class);
    when(page.iterateAll()).thenReturn(List.of(directory, markdown));
    when(storage.list("bucket", BlobListOption.prefix("context/"))).thenReturn(page);

    var refs = reader.listMarkdownObjects("bucket", "context/");

    assertEquals(1, refs.size());
    assertEquals("context/faq.md", refs.get(0).object());
  }
}
