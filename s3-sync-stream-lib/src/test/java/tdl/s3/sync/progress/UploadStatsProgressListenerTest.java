package tdl.s3.sync.progress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadStatsProgressListenerTest {

    private UploadStatsProgressListener listener;
    private File file;

    @BeforeEach
    public void setUp() {
        listener = new UploadStatsProgressListener();
        file = mock(File.class);
        when(file.length()).thenReturn(Long.valueOf(1000000));
    }

    @Test
    public void isCurrentlyUploadingShouldReturnFalse() {
        assertFalse(listener.isCurrentlyUploading());
    }

    @Test
    public void isCurrentlyUploadingShouldReturnTrue() {
        listener.uploadFileStarted(file, "upload", 0);
        assertTrue(listener.isCurrentlyUploading());
    }

    @Test
    public void handleTimestampZeroFileUploadStat() throws InterruptedException {
        listener.uploadFileStarted(file, "upload", 0);
        UploadStatsProgressListener.FileUploadStat stat = listener.getCurrentStats().get();
        assertEquals(0.0, stat.getMBps(), 0.1);
        Thread.sleep(100);
        stat.incrementUploadedSize(500000);
        assertNotEquals(0.0, stat.getMBps(), 0.1);
    }

    @Test
    public void upload() {
        listener.uploadFileStarted(file, "upload", 0);
        UploadStatsProgressListener.FileUploadStat stat = listener.getCurrentStats().get();
        assertEquals(1000000, stat.getTotalSize());
        assertEquals(0, stat.getUploadedSize());
        listener.uploadFileProgress("upload", 500000);
        assertEquals(500000, stat.getUploadedSize());
        assertEquals(0.5, stat.getUploadRatio(), 0.001);
        listener.uploadFileFinished(file);
        assertFalse(listener.isCurrentlyUploading());
    }
}