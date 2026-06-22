package dev.thinke.resume;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkingStrategyEmbeddingEffectivenessTest {

    private static final String QUERY = "What did the candidate build using Kafka and Kubernetes?";
    private static final int MAX_SEGMENT_SIZE = 60;
    private static final int OVERLAP_SIZE = 30;

    @Test
    void overlapChunkingImprovesRetrievalForCrossBoundaryFacts() {
        String documentText = findBoundarySensitiveDocument();
        Document document = Document.from(documentText);

        List<TextSegment> noOverlapSegments = split(document, MAX_SEGMENT_SIZE, 0);
        List<TextSegment> overlapSegments = split(document, MAX_SEGMENT_SIZE, OVERLAP_SIZE);

        assertFalse(anySegmentContainsKafkaAndKubernetes(noOverlapSegments));
        assertTrue(anySegmentContainsKafkaAndKubernetes(overlapSegments));

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        RetrievalResult noOverlapResult = retrieveTopResult(embeddingModel, noOverlapSegments, QUERY);
        RetrievalResult overlapResult = retrieveTopResult(embeddingModel, overlapSegments, QUERY);

        assertFalse(containsKafkaAndKubernetes(noOverlapResult.text));
        assertTrue(containsKafkaAndKubernetes(overlapResult.text));
    }

    private static List<TextSegment> split(Document document, int maxSegmentSize, int overlapSize) {
        DocumentSplitter splitter = DocumentSplitters.recursive(maxSegmentSize, overlapSize);
        return splitter.split(document);
    }

    private static RetrievalResult retrieveTopResult(
            EmbeddingModel embeddingModel, List<TextSegment> segments, String query) {
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        var embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        var queryEmbedding = embeddingModel.embed(query).content();
        var searchResult = embeddingStore.search(
                EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(1).build());
        var match = searchResult.matches().getFirst();
        return new RetrievalResult(match.score(), match.embedded().text());
    }

    private static boolean anySegmentContainsKafkaAndKubernetes(List<TextSegment> segments) {
        return segments.stream().anyMatch(segment -> containsKafkaAndKubernetes(segment.text()));
    }

    private static boolean containsKafkaAndKubernetes(String text) {
        String normalized = text.toLowerCase();
        return normalized.contains("kafka") && normalized.contains("kubernetes");
    }

    private static String findBoundarySensitiveDocument() {
        String suffix = " Additional notes: " + "y".repeat(180);

        for (int keywordGap = 8; keywordGap <= 40; keywordGap++) {
            String fact = " delivered platform work using Kafka "
                    + "z".repeat(keywordGap)
                    + " Kubernetes for production workloads.";

            for (int prefixLength = 0; prefixLength <= 350; prefixLength++) {
                String prefix = "Profile details: " + "x".repeat(prefixLength);
                String candidate = prefix + fact + suffix;
                Document document = Document.from(candidate);
                List<TextSegment> noOverlapSegments = split(document, MAX_SEGMENT_SIZE, 0);
                List<TextSegment> overlapSegments = split(document, MAX_SEGMENT_SIZE, OVERLAP_SIZE);
                boolean noOverlapHasBoth = anySegmentContainsKafkaAndKubernetes(noOverlapSegments);
                boolean overlapHasBoth = anySegmentContainsKafkaAndKubernetes(overlapSegments);
                if (!noOverlapHasBoth && overlapHasBoth) {
                    return candidate;
                }
            }
        }

        throw new IllegalStateException("Could not build a boundary-sensitive document for this splitter setup");
    }

    private record RetrievalResult(double score, String text) {}
}
