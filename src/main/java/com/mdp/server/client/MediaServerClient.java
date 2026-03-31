package com.mdp.server.client;

import com.mdp.server.dto.MediaUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class MediaServerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${media.server.url}")
    private String mediaServerUrl;

    public MediaUploadResponse uploadImage(byte[] fileBytes, String fileName, String group) {
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("group", group);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        ResponseEntity<MediaUploadResponse> response = restTemplate.exchange(
                mediaServerUrl + "/media/upload",
                HttpMethod.POST,
                requestEntity,
                MediaUploadResponse.class
        );

        return response.getBody();
    }
}