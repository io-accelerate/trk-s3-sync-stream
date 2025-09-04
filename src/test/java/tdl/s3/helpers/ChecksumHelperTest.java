package tdl.s3.helpers;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;

public class ChecksumHelperTest {

    @Test
    public void handleNotFoundAlgorithmException() throws Exception {
        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> {
            byte[] bytes = "Hello World!".getBytes();
            ChecksumHelper.digest(bytes, "SOMETHING");
        });
        MatcherAssert.assertThat(runtimeException.getMessage(), containsString("Can't send multipart upload."));
    }

    @Test
    public void digest() {
        byte[] bytes = "Hello World!".getBytes();
        Assertions.assertEquals(ChecksumHelper.digest(bytes, "MD5"), "7Qdih1MuhjZehB6Sv8UNjA==");
    }
}
