package com.kds.backend.documents.application;
import java.util.UUID;
/** Public Documents/storage boundary. A Supabase adapter can replace the local adapter without changing feature modules. */
public interface FileStorageService {
    StoredFile store(UUID clubId,String category,String fileName,String contentType,byte[] content);
}
