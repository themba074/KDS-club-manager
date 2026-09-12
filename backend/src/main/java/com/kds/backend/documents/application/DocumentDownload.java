package com.kds.backend.documents.application;

import java.net.URI;

public record DocumentDownload(String fileName, String contentType, URI signedUrl, byte[] content) {}
