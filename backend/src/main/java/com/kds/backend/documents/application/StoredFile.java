package com.kds.backend.documents.application;
public record StoredFile(String storageKey,String fileName,String contentType,long size) {}
