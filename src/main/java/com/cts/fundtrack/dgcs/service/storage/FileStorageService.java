package com.cts.fundtrack.dgcs.service.storage;

import com.cts.fundtrack.dgcs.config.StorageConfig;

import com.cts.fundtrack.dgcs.exception.FileStorageException;
import com.cts.fundtrack.dgcs.exception.InvalidFileException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.UUID;

/**
 * Core Infrastructure Service for centralized file management.
 * <p>
 * This service abstracts the underlying filesystem operations, providing a secure and
 * consistent way to persist binary assets. It handles directory initialization
 * and unique file naming to prevent collisions.
 * </p>
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(StorageConfig config) {
        this.storageLocation = Paths.get(config.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageLocation);
        } catch (Exception e) {
            log.error("Storage Initialization Failed: {}", e.getMessage());
            throw new FileStorageException("Could not initialize storage directory");
        }
    }

    /**
     * Persists a multipart file to the configured storage location.
     *
     * @param file The inbound {@link MultipartFile} binary payload.
     * @return The absolute path string of the stored file.
     * @throws InvalidFileException If the file is null or empty.
     * @throws FileStorageException If an I/O error occurs during transfer.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("I/O Abort | Reason: Null or empty file provided.");
            throw new InvalidFileException("Persistence failed: The uploaded document contains no data.");
        }

        String originalName = file.getOriginalFilename();
        try {
            String uniqueFileName = UUID.randomUUID() + "_" + originalName;
            Path targetPath = this.storageLocation.resolve(uniqueFileName).normalize();

            log.info("I/O Transfer Initiated | Resource: {} | Target: {}", originalName, targetPath);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("I/O Transfer Success | Path: {}", targetPath);
            return targetPath.toString();

        } catch (java.io.IOException e) {
            log.error("CRITICAL I/O FAILURE | Resource: {} | Error: {}", originalName, e.getMessage());
            throw new FileStorageException("Physical storage failure: Unable to commit stream to filesystem.");
        }
    }
}