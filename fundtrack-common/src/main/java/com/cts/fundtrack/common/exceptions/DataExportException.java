package com.cts.fundtrack.common.exceptions;

/**
 * Exception thrown when an error occurs during the data export process.
 * <p>
 * This is typically used in the Analytics or Finance modules when the system
 * fails to generate, format, or stream files (such as CSV, Excel, or PDF)
 * to the user.
 * </p>
 * <p>
 * Common triggers include:
 * <ul>
 * <li>I/O errors during file writing</li>
 * <li>Empty data sets where a file is required</li>
 * <li>Memory issues during large report generation</li>
 * </ul>
 * </p>
 */
public class DataExportException extends RuntimeException {

    /**
     * Constructs a new DataExportException with a detailed error message.
     *
     * @param message the detail message describing the cause of the export failure
     * (e.g., "Empty dataset for report" or "IO error during PDF generation").
     */
    public DataExportException(String message) {
        super(message);
    }
}