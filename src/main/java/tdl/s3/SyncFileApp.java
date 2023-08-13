package tdl.s3;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import tdl.s3.credentials.AWSSecretProperties;
import tdl.s3.sync.Filters;
import tdl.s3.sync.RemoteSync;
import tdl.s3.sync.Source;
import tdl.s3.sync.destination.Destination;
import tdl.s3.sync.destination.DestinationOperationException;
import tdl.s3.sync.destination.S3BucketDestination;
import tdl.s3.sync.progress.UploadStatsProgressListener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

@Parameters
public class SyncFileApp {

    @Parameter(names = {"--config", "-c"})
    private String configPath = "./.private/aws-test-secrets";

    @Parameter(names = {"--dir", "-d"}, required = true)
    private String dirPath;

    @Parameter(names = {"--recursive", "-R"})
    private boolean recursive = false;

    @Parameter(names = {"--filter"})
    private String regex = "^[0-9a-zA-Z\\_]+\\.mp4";

    private static final NumberFormat percentageFormatter = NumberFormat.getPercentInstance();
    private static final NumberFormat uploadSpeedFormatter = NumberFormat.getNumberInstance();

    static {
        percentageFormatter.setMinimumFractionDigits(1);
        uploadSpeedFormatter.setMinimumFractionDigits(1);
    }

    public static void main(String[] args) throws DestinationOperationException {
        S3BucketDestination.runSanityCheck();

        SyncFileApp app = new SyncFileApp();
        JCommander jCommander = new JCommander(app);
        jCommander.parse(args);

        app.run();
    }

    private void run() throws DestinationOperationException {
        // Prepare
        Source source = buildSource();
        Destination destination = buildDestination();
        RemoteSync sync = new RemoteSync(source, destination);

        // Check destination
        destination.startS3SyncSession();

        // Configure progress listener
        UploadStatsProgressListener uploadStatsProgressListener = new UploadStatsProgressListener();
        sync.setListener(uploadStatsProgressListener);
        Timer timer = new Timer();


        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                uploadStatsProgressListener.getCurrentStats().ifPresent(fileUploadStat -> System.out.println("\rUploaded : "
                        + percentageFormatter.format(fileUploadStat.getUploadRatio())
                        + ". "
                        + fileUploadStat.getUploadedSize() + "/" + fileUploadStat.getTotalSize()
                        + " bytes. "
                        + uploadSpeedFormatter.format(fileUploadStat.getMBps())
                        + " Mbps"));
            }
        }, 0, 1000);

        // Run (blocking)
        sync.run();
        timer.cancel();
    }

    private Source buildSource() {
        Filters filters = Filters.getBuilder()
                .include(Filters.matches(regex))
                .create();
        return Source.getBuilder(Paths.get(dirPath))
                .setFilters(filters)
                .setRecursive(recursive)
                .create();
    }

    private Destination buildDestination() {
        Path path = Paths.get(configPath);
        AWSSecretProperties awsSecretProperties = AWSSecretProperties.fromPlainTextFile(path);

        return new S3BucketDestination(
                awsSecretProperties.createClient(),
                awsSecretProperties.getS3Bucket(),
                awsSecretProperties.getS3Prefix());
    }
}
