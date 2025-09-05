package io.accelerate.tracking.sync.upload;

import java.io.File;

/**
 * General interface to upload any file to S3 storage
 *
 */
public interface FileUploader {

    void upload(File file) throws UploadingException;

    void upload(File file, String path) throws UploadingException;
}
