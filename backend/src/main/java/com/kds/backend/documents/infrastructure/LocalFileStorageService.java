package com.kds.backend.documents.infrastructure;

import com.kds.backend.documents.application.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnProperty(name="app.storage.provider", havingValue="local", matchIfMissing=true)
public class LocalFileStorageService implements FileStorageService {
    private final Path root;
    public LocalFileStorageService(@Value("${app.storage.local-root}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    @Override public StoredFile store(UUID clubId,String category,String fileName,String contentType,byte[] content){
        String safeCategory=category.replaceAll("[^a-zA-Z0-9_-]","_");
        String extension=extension(fileName); String storedName=UUID.randomUUID()+extension;
        Path directory=root.resolve(clubId.toString()).resolve(safeCategory).normalize();
        Path target=directory.resolve(storedName).normalize();
        if(!target.startsWith(root))throw new IllegalArgumentException("Invalid storage path.");
        try { Files.createDirectories(directory); Files.write(target,content,StandardOpenOption.CREATE_NEW); }
        catch(IOException exception){throw new IllegalStateException("The proof could not be stored.",exception);}
        String key=root.relativize(target).toString().replace('\\','/');
        return new StoredFile(key,fileName,contentType,content.length);
    }
    @Override public StoredFile storeAt(UUID clubId,String storageKey,String fileName,String contentType,byte[] content){
        validateKey(clubId, storageKey);
        Path target=root.resolve(storageKey).normalize();
        if(!target.startsWith(root))throw new IllegalArgumentException("Invalid storage path.");
        try { Files.createDirectories(target.getParent()); Files.write(target,content,StandardOpenOption.CREATE_NEW); }
        catch(IOException exception){throw new IllegalStateException("The document could not be stored.",exception);}
        return new StoredFile(storageKey,fileName,contentType,content.length);
    }
    @Override public StoredContent load(UUID clubId,String storageKey){
        validateKey(clubId, storageKey);
        Path source=root.resolve(storageKey).normalize();
        if(!source.startsWith(root))throw new IllegalArgumentException("Invalid storage path.");
        try{return new StoredContent(Files.readAllBytes(source),Files.probeContentType(source));}
        catch(IOException exception){throw new IllegalStateException("The file could not be loaded.",exception);}
    }
    private static void validateKey(UUID clubId,String storageKey){
        String normalized=storageKey==null?"":storageKey.replace('\\','/');
        String legacy=clubId+"/"; String documents="documents/"+clubId+"/";
        if(!(normalized.startsWith(legacy)||normalized.startsWith(documents))||normalized.contains("../"))
            throw new IllegalArgumentException("Invalid storage key.");
    }
    private static String extension(String name){
        if(name==null)return ""; int dot=name.lastIndexOf('.');
        return dot<0||dot<name.length()-6?"":name.substring(dot).toLowerCase(java.util.Locale.ROOT).replaceAll("[^.a-z0-9]","");
    }
}
