package com.example.instagram.post.controller;

import com.example.instagram.post.storage.LocalImageStorage;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class MediaController {

    private final LocalImageStorage localImageStorage;

    @GetMapping("/{userId}/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable Long userId, @PathVariable String filename) throws Exception {
        String storageKey = userId + "/" + filename;
        var path = localImageStorage.resolvePhysicalPath(storageKey);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        String probe = Files.probeContentType(path);
        MediaType mediaType = probe != null ? MediaType.parseMediaType(probe) : MediaType.APPLICATION_OCTET_STREAM;
        InputStreamResource body = new InputStreamResource(Files.newInputStream(path));
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(body);
    }
}
