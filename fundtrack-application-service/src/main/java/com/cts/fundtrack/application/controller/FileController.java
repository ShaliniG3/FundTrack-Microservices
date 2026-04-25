package com.cts.fundtrack.application.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/application-files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            Path uploadDir = Paths.get("C:/uploads/proofs/");

            // Try exact match first
            Path exactPath = uploadDir.resolve(decodedFilename).normalize();
            Resource resource = new UrlResource(exactPath.toUri());

            // If not found, search for file ending with the filename
            if (!resource.exists() || !resource.isReadable()) {
                resource = Files.list(uploadDir)
                        .filter(p -> p.getFileName().toString().endsWith("_" + decodedFilename)
                                || p.getFileName().toString().equals(decodedFilename))
                        .findFirst()
                        .map(p -> {
                            try { return (Resource) new UrlResource(p.toUri()); }
                            catch (Exception e) { return null; }
                        })
                        .orElse(null);
            }

            if (resource == null || !resource.exists() || !resource.isReadable()) {
                log.warn("File not found: {}", decodedFilename);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + decodedFilename + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            log.error("Could not serve file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }
}