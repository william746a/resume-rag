package dev.thinke.resume;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import org.junit.jupiter.api.Test;

class DocumentChunkingTest {

    @Test
    void recursiveSplitterProducesMultipleSegmentsForLongDoc() {
        String markdown =
                """
                ## Role A
                %s
                ## Role B
                %s
                """
                        .formatted("x".repeat(400), "y".repeat(400));

        var splitter = DocumentSplitters.recursive(500, 50);
        var segments = splitter.split(Document.from(markdown));

        assertFalse(segments.isEmpty());
        assertTrue(segments.size() >= 2);
    }
}
