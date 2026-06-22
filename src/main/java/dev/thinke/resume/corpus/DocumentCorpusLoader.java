package dev.thinke.resume.corpus;

import dev.thinke.resume.corpus.DocumentCorpusConfig.DocumentSourceConfig;
import dev.thinke.resume.corpus.GcsDocumentReader.GcsObjectRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class DocumentCorpusLoader {

    private final DocumentCorpusConfig config;
    private final ClasspathDocumentReader classpathReader;
    private final GcsDocumentReader gcsReader;

    @Inject
    public DocumentCorpusLoader(
            DocumentCorpusConfig config,
            ClasspathDocumentReader classpathReader,
            GcsDocumentReader gcsReader) {
        this.config = config;
        this.classpathReader = classpathReader;
        this.gcsReader = gcsReader;
    }

    public List<LoadedDocument> load() throws IOException {
        List<LoadedDocument> documents = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> gcsObjects = new HashSet<>();

        for (DocumentSourceConfig source : config.sources()) {
            LoadedDocument doc = loadSource(source);
            if (!ids.add(doc.id())) {
                throw new IllegalStateException("Duplicate document id: " + doc.id());
            }
            if (doc.sourceUri().startsWith("gcs://")) {
                gcsObjects.add(gcsKeyFromUri(doc.sourceUri()));
            }
            documents.add(doc);
        }

        if (config.gcs().contextPrefixEnabled()) {
            String bucket = resolveDefaultBucket();
            for (GcsObjectRef ref : gcsReader.listMarkdownObjects(bucket, config.gcs().contextPrefix())) {
                if (gcsObjects.contains(ref.bucket() + "/" + ref.object())) {
                    continue;
                }
                String id = idFromObjectName(ref.object());
                if (!ids.add(id)) {
                    throw new IllegalStateException("Duplicate document id from GCS prefix: " + id);
                }
                String markdown = gcsReader.readObject(ref.bucket(), ref.object());
                documents.add(new LoadedDocument(id, DocumentRole.context, ref.sourceUri(), markdown));
                gcsObjects.add(ref.bucket() + "/" + ref.object());
            }
        }

        long resumeCount = documents.stream().filter(d -> d.role() == DocumentRole.resume).count();
        if (resumeCount != 1) {
            throw new IllegalStateException(
                    "Document corpus must contain exactly one resume document, found: " + resumeCount);
        }

        return List.copyOf(documents);
    }

    private LoadedDocument loadSource(DocumentSourceConfig source) throws IOException {
        String id = source.id();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Document id is required");
        }

        return switch (source.source()) {
            case classpath -> loadClasspath(source);
            case gcs -> loadGcs(source);
        };
    }

    private LoadedDocument loadClasspath(DocumentSourceConfig source) throws IOException {
        String path = source.classpath()
                .path()
                .orElseThrow(() -> new IllegalStateException(
                        "documents.sources[].classpath.path is required for classpath source: " + source.id()));
        String markdown = classpathReader.read(path);
        String sourceUri = "classpath:" + path;
        return new LoadedDocument(source.id(), source.role(), sourceUri, markdown);
    }

    private LoadedDocument loadGcs(DocumentSourceConfig source) {
        String bucket = source.gcs()
                .bucket()
                .or(() -> config.gcs().bucket())
                .orElseThrow(() -> new IllegalStateException(
                        "GCS bucket is required for document '" + source.id()
                                + "' (set documents.sources[].gcs.bucket or documents.gcs.bucket)"));
        String object = source.gcs()
                .object()
                .orElseThrow(() -> new IllegalStateException(
                        "documents.sources[].gcs.object is required for gcs source: " + source.id()));
        String markdown = gcsReader.readObject(bucket, object);
        String sourceUri = "gcs://" + bucket + "/" + object;
        return new LoadedDocument(source.id(), source.role(), sourceUri, markdown);
    }

    private String resolveDefaultBucket() {
        return config.gcs()
                .bucket()
                .orElseThrow(() -> new IllegalStateException(
                        "documents.gcs.bucket is required when context prefix expansion is enabled"));
    }

    private static String gcsKeyFromUri(String sourceUri) {
        // gcs://bucket/path/to/object.md
        String withoutScheme = sourceUri.substring("gcs://".length());
        int slash = withoutScheme.indexOf('/');
        if (slash < 0) {
            throw new IllegalStateException("Invalid GCS source URI: " + sourceUri);
        }
        return withoutScheme;
    }

    static String idFromObjectName(String objectName) {
        String fileName = objectName;
        int slash = objectName.lastIndexOf('/');
        if (slash >= 0) {
            fileName = objectName.substring(slash + 1);
        }
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            fileName = fileName.substring(0, fileName.length() - 3);
        }
        if (fileName.isBlank()) {
            throw new IllegalStateException("Cannot derive document id from GCS object: " + objectName);
        }
        return fileName;
    }
}
