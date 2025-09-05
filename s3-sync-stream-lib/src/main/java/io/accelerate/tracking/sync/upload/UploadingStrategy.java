package io.accelerate.tracking.sync.upload;


import io.accelerate.tracking.sync.sync.destination.Destination;
import io.accelerate.tracking.sync.sync.destination.DestinationOperationException;
import io.accelerate.tracking.sync.sync.progress.ProgressListener;

import java.io.File;
import java.io.IOException;

public interface UploadingStrategy {
    
    void setDestination(Destination destination);

    void upload(File file, String remotePath) throws DestinationOperationException, IOException;

    void setListener(ProgressListener listener);
}
