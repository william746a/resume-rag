package dev.thinke.resume.corpus;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ClasspathDocumentReader {

    public String read(String classpathPath) throws IOException {
        if (classpathPath == null || classpathPath.isBlank()) {
            throw new IllegalArgumentException("Classpath path is required");
        }
        String normalized = classpathPath.startsWith("/") ? classpathPath.substring(1) : classpathPath;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(normalized)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + normalized);
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new IllegalStateException("Classpath resource is empty: " + normalized);
            }
            return content;
        }
    }
}
