package io.accelerate.tracking.sync.upload;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import org.slf4j.Logger;
import io.accelerate.tracking.sync.sync.destination.Destination;
import io.accelerate.tracking.sync.sync.destination.DestinationOperationException;
import io.accelerate.tracking.sync.sync.progress.DummyProgressListener;
import io.accelerate.tracking.sync.sync.progress.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class MultipartUploadFileUploadingStrategy implements UploadingStrategy {
    private static final Logger log = getLogger(MultipartUploadFileUploadingStrategy.class);

    private static final int DEFAULT_THREAD_COUNT = 4;

    private Destination destination;

    private ConcurrentMultipartUploader concurrentUploader;

    private ProgressListener listener = new DummyProgressListener();

    /**
     * Creates new Multipart upload strategy
     */
    MultipartUploadFileUploadingStrategy(Destination destination) {
        this(destination, DEFAULT_THREAD_COUNT);
    }

    /**
     * Creates new Multipart upload strategy.
     *
     * @param threadsCount count of threads that should be used for uploading
     */
    private MultipartUploadFileUploadingStrategy(Destination destination, int threadsCount) {
        this.destination = destination;
        concurrentUploader = new ConcurrentMultipartUploader(destination, threadsCount);
    }

    @Override
    public void upload(File file, String remotePath) throws DestinationOperationException, IOException {
        MultipartUploadFile multipartUploadFile = new MultipartUploadFile(file, remotePath, destination);
        multipartUploadFile.validateUploadedFileSize();
        multipartUploadFile.notifyStart(listener);
        uploadRequiredParts(multipartUploadFile);
        multipartUploadFile.notifyFinish(listener);
    }

    private void uploadRequiredParts(MultipartUploadFile multipartUploadFile) throws IOException, DestinationOperationException {
        List<PartETag> eTags = multipartUploadFile.getPartETags();

        Stream<UploadPartRequest> failedPartRequestStream = multipartUploadFile
                .streamUploadPartRequestForFailedParts();
        submitUploadRequestStream(failedPartRequestStream, eTags);

        Stream<UploadPartRequest> incompletePartRequestStream = multipartUploadFile
                .streamUploadPartRequestForIncompleteParts();
        submitUploadRequestStream(incompletePartRequestStream, eTags);

        concurrentUploader.shutdownAndAwaitTermination();

        multipartUploadFile.commitIfFinishedWriting();
    }

    private void submitUploadRequestStream(Stream<UploadPartRequest> requestStream, List<PartETag> partETags) {
        requestStream
                .map(this::attachListenerToRequest)
                .map(concurrentUploader::submitTaskForPartUploading)
                .map(MultipartUploadFileUploadingStrategy::getUploadingResult)
                .filter(Objects::nonNull)
                .map(e -> e.getResult().getPartETag())
                .forEach(partETags::add);
    }

    private UploadPartRequest attachListenerToRequest(UploadPartRequest request) {
        request.setGeneralProgressListener((com.amazonaws.event.ProgressEvent pe)
                -> listener.uploadFileProgress(request.getUploadId(), pe.getBytesTransferred()));
        return request;
    }

    public static MultipartUploadResult getUploadingResult(Future<MultipartUploadResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            log.error("Some part uploads was unsuccessful.", e);
            return null;
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();
            if (ex instanceof DestinationOperationException) {
                log.error("Some part uploads was unsuccessful.", ex);
            }
            log.error("Some part uploads was unsuccessful.", e);
            return null;
        }
    }

    @Override
    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public void setDestination(Destination destination) {
        this.destination = destination;
    }
}
