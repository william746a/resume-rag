package dev.thinke.resume;

import dev.thinke.resume.corpus.DocumentCorpusLoader;
import dev.thinke.resume.corpus.LoadedDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RagBootstrap {

    private static final Logger LOG = Logger.getLogger(RagBootstrap.class);

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    DocumentCorpusLoader documentCorpusLoader;

    @ConfigProperty(name = "resume.rag.chunk-size")
    int chunkSize;

    @ConfigProperty(name = "resume.rag.chunk-overlap")
    int chunkOverlap;

    private final AtomicBoolean corpusReady = new AtomicBoolean(false);

    public boolean isCorpusReady() {
        return corpusReady.get();
    }

    void onStart(@Observes StartupEvent event) throws IOException {
        List<LoadedDocument> documents = documentCorpusLoader.load();
        var splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);

        List<TextSegment> allSegments = new ArrayList<>();
        List<String> perDocumentSummary = new ArrayList<>();

        for (LoadedDocument doc : documents) {
            Metadata metadata = Metadata.from("document_id", doc.id())
                    .put("document_role", doc.role().name())
                    .put("source_uri", doc.sourceUri());
            Document document = Document.from(doc.markdown(), metadata);
            List<TextSegment> segments = splitter.split(document);
            perDocumentSummary.add(doc.id() + " (" + doc.sourceUri() + ", " + segments.size() + " segments)");
            allSegments.addAll(segments);
        }

        if (allSegments.isEmpty()) {
            throw new IllegalStateException("Document corpus produced no segments");
        }

        var embeddings = embeddingModel.embedAll(allSegments).content();
        if (embeddings.size() != allSegments.size()) {
            throw new IllegalStateException("Embedding count does not match segment count");
        }
        for (int i = 0; i < allSegments.size(); i++) {
            embeddingStore.add(embeddings.get(i), allSegments.get(i));
        }

        LOG.infof(
                "Ingested document corpus (%d documents, %d segments): %s",
                documents.size(),
                allSegments.size(),
                String.join(", ", perDocumentSummary));

        corpusReady.set(true);
        LOG.info("Corpus ingestion complete; readiness check is UP");
    }
}
