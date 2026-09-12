package com.kds.backend.documents.infrastructure;


import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseFileStorageServiceTests {
    @Test void uploadsWithoutOverwriteAndBuildsExpiringPrivateDownloadUrl(){RestClient.Builder builder=RestClient.builder();MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();UUID club=UUID.randomUUID();String key="documents/"+club+"/doc/version.pdf";
        var service=new SupabaseFileStorageService("https://project.supabase.co","service-secret","private",builder,new ObjectMapper());
        String objectUrl="https://project.supabase.co/storage/v1/object/private/"+key;
        server.expect(once(),requestTo(objectUrl)).andExpect(method(HttpMethod.POST)).andExpect(header("apikey","service-secret")).andExpect(header("Authorization","Bearer service-secret")).andExpect(header("x-upsert","false")).andExpect(content().bytes(new byte[]{1,2})).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        server.expect(once(),requestTo("https://project.supabase.co/storage/v1/object/sign/private/"+key)).andExpect(method(HttpMethod.POST)).andExpect(content().json("{\"expiresIn\":300}")).andRespond(withSuccess("{\"signedURL\":\"/storage/v1/object/sign/private/tokenized?token=abc\"}",MediaType.APPLICATION_JSON));
        assertEquals(key,service.storeAt(club,key,"rules.pdf","application/pdf",new byte[]{1,2}).storageKey());
        assertEquals("https://project.supabase.co/storage/v1/object/sign/private/tokenized?token=abc",service.signedDownloadUrl(club,key,Duration.ofMinutes(5)).orElseThrow().toString());server.verify();}
}
