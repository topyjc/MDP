package com.mdp.server.controller;

import com.mdp.server.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/media")
public class MediaController {

    @Value("${media.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("group") String group
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, "empty file", null, null, null));
            }

            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path groupDir = Paths.get(uploadDir, group).toAbsolutePath().normalize();
            Files.createDirectories(groupDir);

            Path target = groupDir.resolve(fileName);
            Files.write(target, file.getBytes());

            String fileUrl = "/media/files/" + group + "/" + fileName;

            UploadResponse response = new UploadResponse(
                    true,
                    "uploaded",
                    group,
                    fileName,
                    fileUrl
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new UploadResponse(false, e.getMessage(), group, null, null));
        }
    }

    @GetMapping("/files/{group}/{fileName:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String group,
            @PathVariable String fileName
    ) throws Exception {
        Path filePath = Paths.get(uploadDir, group, fileName).toAbsolutePath().normalize();
        Resource resource = toResource(filePath);

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("MEDIA_SERVER_OK");
    }

    private Resource toResource(Path path) throws MalformedURLException {
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("file not found: " + path);
        }
        return resource;
    }
}