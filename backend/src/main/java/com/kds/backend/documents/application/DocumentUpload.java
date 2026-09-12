package com.kds.backend.documents.application;

public record DocumentUpload(String fileName, String contentType, byte[] content) {}
