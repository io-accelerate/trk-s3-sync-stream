package io.accelerate.tracking.sync.sync;

import io.accelerate.tracking.sync.sync.destination.Destination;
import io.accelerate.tracking.sync.sync.destination.DestinationOperationException;
import io.accelerate.tracking.sync.sync.progress.ProgressListener;
import io.accelerate.tracking.sync.upload.FileUploadingService;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class FolderSynchronizer {

    private final Source source;

    private final FileUploadingService fileUploadingService;

    FolderSynchronizer(Source source, FileUploadingService fileUploadingService) {
        this.source = source;
        this.fileUploadingService = fileUploadingService;
    }

    void synchronize() {
        Path folder = source.getPath();
        List<String> paths = source.getFilesToUpload();
        Destination destination = fileUploadingService.getDestination();
        List<String> uploadable;
        try {
            uploadable = destination.filterUploadableFiles(paths);
        } catch (DestinationOperationException ex) {
            uploadable = new ArrayList<>();
        }
        if (uploadable.isEmpty()) {
            return;
        }
        uploadable.forEach(upload -> {
                    File uploadFile = new File(folder.toFile(), upload);
                    fileUploadingService.upload(uploadFile, upload);
                });
    }

    void setListener(ProgressListener listener) {
        fileUploadingService.setListener(listener);
    }
}
