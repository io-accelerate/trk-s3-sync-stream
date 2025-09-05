package io.accelerate.tracking.sync.sync.progress;

import java.io.File;

public interface ProgressListener {

    void uploadFileStarted(File file, String uploadId, long uploadedByte);

    void uploadFileProgress(String uploadId, long uploadedByte);

    void uploadFileFinished(File file);
}
