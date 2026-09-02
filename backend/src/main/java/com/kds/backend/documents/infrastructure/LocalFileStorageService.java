package com.kds.backend.documents.infrastructure;

import com.kds.backend.documents.application.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
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
    private static String extension(String name){
        if(name==null)return ""; int dot=name.lastIndexOf('.');
        return dot<0||dot<name.length()-6?"":name.substring(dot).toLowerCase(java.util.Locale.ROOT).replaceAll("[^.a-z0-9]","");
    }
}
