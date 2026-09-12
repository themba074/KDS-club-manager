package com.kds.backend.documents.infrastructure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kds.backend.documents.application.FileStorageService;
import com.kds.backend.documents.application.StoredContent;
import com.kds.backend.documents.application.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name="app.storage.provider", havingValue="supabase")
public class SupabaseFileStorageService implements FileStorageService {
    private final RestClient client;
    private final ObjectMapper json;
    private final String bucket;
    private final URI storageBase;

    public SupabaseFileStorageService(@Value("${app.storage.supabase.url}") String url,
                                      @Value("${app.storage.supabase.service-role-key}") String key,
                                      @Value("${app.storage.supabase.bucket}") String bucket,
                                      RestClient.Builder builder, ObjectMapper json) {
        if (url.isBlank() || key.isBlank() || !bucket.matches("[a-zA-Z0-9_-]+")) throw new IllegalStateException("Supabase storage configuration is incomplete.");
        this.storageBase=URI.create(url.replaceAll("/$", "") + "/storage/v1/");
        this.client = builder.baseUrl(storageBase.toString())
                .defaultHeader("apikey", key).defaultHeader("Authorization", "Bearer " + key).build();
        this.json = json; this.bucket = bucket;
    }

    @Override public StoredFile store(UUID clubId,String category,String fileName,String contentType,byte[] content) {
        String safeCategory=category.replaceAll("[^a-zA-Z0-9_-]","_");
        return storeAt(clubId, clubId+"/"+safeCategory+"/"+UUID.randomUUID()+extension(fileName), fileName, contentType, content);
    }
    @Override public StoredFile storeAt(UUID clubId,String storageKey,String fileName,String contentType,byte[] content) {
        validateKey(clubId,storageKey);
        client.post().uri(objectUri("object",storageKey))
                .contentType(MediaType.parseMediaType(contentType)).header("x-upsert","false")
                .body(content).retrieve().toBodilessEntity();
        return new StoredFile(storageKey,fileName,contentType,content.length);
    }
    @Override public StoredContent load(UUID clubId,String storageKey) {
        validateKey(clubId,storageKey);
        var response=client.get().uri(objectUri("object",storageKey)).retrieve().toEntity(byte[].class);
        String contentType=response.getHeaders().getContentType()==null?MediaType.APPLICATION_OCTET_STREAM_VALUE:response.getHeaders().getContentType().toString();
        return new StoredContent(response.getBody(),contentType);
    }
    @Override public Optional<URI> signedDownloadUrl(UUID clubId,String storageKey,Duration validity) {
        validateKey(clubId,storageKey);
        String body=client.post().uri(objectUri("object/sign",storageKey))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("expiresIn",validity.toSeconds()))
                .retrieve().body(String.class);
        try {
            JsonNode node=json.readTree(body); String path=node.path("signedURL").asString(node.path("signedUrl").asString());
            if(path.isBlank())throw new IllegalStateException("Supabase did not return a signed URL.");
            return Optional.of(path.startsWith("http")?URI.create(path):storageBase.resolve(path.replaceFirst("^/storage/v1/", "")));
        } catch(Exception exception){throw new IllegalStateException("The download link could not be created.",exception);}
    }
    private URI objectUri(String operation,String storageKey){return storageBase.resolve(operation+"/"+bucket+"/"+storageKey);}
    private static void validateKey(UUID clubId,String key){
        String value=key==null?"":key.replace('\\','/');
        if(!(value.startsWith(clubId+"/")||value.startsWith("documents/"+clubId+"/"))||value.contains("../"))
            throw new IllegalArgumentException("Invalid storage key.");
    }
    private static String extension(String name){if(name==null)return "";int dot=name.lastIndexOf('.');return dot<0||dot<name.length()-6?"":name.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^.a-z0-9]","");}
}
