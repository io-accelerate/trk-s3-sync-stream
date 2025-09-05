package io.accelerate.tracking.sync.upload;

import io.accelerate.tracking.sync.sync.destination.Destination;
import io.accelerate.tracking.sync.sync.progress.DummyProgressListener;
import io.accelerate.tracking.sync.sync.progress.ProgressListener;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUploadingService {

    private final Destination destination;

    private ProgressListener listener = new DummyProgressListener();

    public FileUploadingService(Destination destination) {
        this.destination = destination;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void upload(File file) {
        upload(file, file.getName());
    }

    public void upload(File file, String remoteName) {
        FileUploader fileUploader = createFileUploader();
        try {
            fileUploader.upload(file, remoteName);
        } catch (UploadingException ex) {
            Logger.getLogger(FileUploadingService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private FileUploader createFileUploader() {
        UploadingStrategy strategy = new MultipartUploadFileUploadingStrategy(destination);
        strategy.setListener(listener);
        return new FileUploaderImpl(destination, strategy);
    }
}
