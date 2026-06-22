package dev.thinke.resume.corpus;

public record LoadedDocument(String id, DocumentRole role, String sourceUri, String markdown) {}
