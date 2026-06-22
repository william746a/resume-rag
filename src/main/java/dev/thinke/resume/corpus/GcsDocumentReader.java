package dev.thinke.resume.corpus;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class GcsDocumentReader {

    private final Storage storage;

    @Inject
    public GcsDocumentReader(Storage storage) {
        this.storage = storage;
    }

    public String readObject(String bucket, String object) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("GCS bucket is required");
        }
        if (object == null || object.isBlank()) {
            throw new IllegalArgumentException("GCS object is required");
        }
        Blob blob = storage.get(bucket, object);
        if (blob == null || !blob.exists()) {
            throw new IllegalStateException("GCS object not found: gs://" + bucket + "/" + object);
        }
        byte[] content = blob.getContent();
        if (content == null || content.length == 0) {
            throw new IllegalStateException("GCS object is empty: gs://" + bucket + "/" + object);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    public List<GcsObjectRef> listMarkdownObjects(String bucket, String prefix) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("GCS bucket is required");
        }
        String normalizedPrefix = prefix == null ? "" : prefix;
        List<GcsObjectRef> refs = new ArrayList<>();
        for (Blob blob : storage.list(bucket, BlobListOption.prefix(normalizedPrefix)).iterateAll()) {
            if (blob.isDirectory()) {
                continue;
            }
            String name = blob.getName();
            if (name.toLowerCase(Locale.ROOT).endsWith(".md")) {
                refs.add(new GcsObjectRef(bucket, name));
            }
        }
        return refs;
    }

    public record GcsObjectRef(String bucket, String object) {
        String sourceUri() {
            return "gcs://" + bucket + "/" + object;
        }
    }
}
