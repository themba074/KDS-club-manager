package com.kds.backend.documents.application;
import java.util.UUID;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
/** Public Documents/storage boundary. A Supabase adapter can replace the local adapter without changing feature modules. */
public interface FileStorageService {
    StoredFile store(UUID clubId,String category,String fileName,String contentType,byte[] content);
    default StoredFile storeAt(UUID clubId,String storageKey,String fileName,String contentType,byte[] content) {
        throw new UnsupportedOperationException("Exact storage keys are not supported.");
    }
    StoredContent load(UUID clubId,String storageKey);
    default Optional<URI> signedDownloadUrl(UUID clubId, String storageKey, Duration validity) { return Optional.empty(); }
}
