# trkl-s3-sync-stream

`s3-sync-stream` is library that continuously syncs the contents of a folder to an S3 bucket. Optimised for streaming file formats (video, logs).

The library will aggresively upload content as it is being generated:
* Each chunk will be uploaded as a part in a multipart upload
* The multipart upload in kept open while the file is being generated 
* It is the responsibility of the generator to create a `.lock` with the same name.
* Once the file generation is completed and the `.lock` file removed, the multipart upload will be finalised.


## To use as a library

### Add as Maven dependency

Add a dependency to `tdl:s3-sync-stream` in `compile` scope. See `bintray` shield for latest release number.
```xml
<dependency>
  <groupId>io.accelerate</groupId>
  <artifactId>s3-sync-stream-lib</artifactId>
  <version>X.Y.Z</version>
</dependency>
```

### Configure AWS user with minimal permissions

**WIP** - TODO Add detailed IAM instructions

### Define sync source and destination

Configure the local folder as a `source` and define AWS S3 as the `destination`
```java
Source source = Source.getBuilder(/* Path */ pathToFolder)
  .traverseDirectories(true)
  .include(endsWith(".mp4")
  .exclude(startsWith(".")
  .exclude(matches("tmp.log"))
  .create();

Destination destination = Destination.getBuilder()
  .loadFromPath(/* Path */ pathToFile)
  .create();
```

Construct the `RemoteSync` and run. The `run` method can be invoked multiple times.
```java
remoteSync = new RemoteSync(source, destination);
remoteSync.run();
```

### Example source definitions

The source will be a set of filters that can be applied to a folder to obtain a list of files to be synced

**Default values** will not include .lock files and hidden files (. files)
```java
Source source = Source.getBuilder(/* Path */ pathToFolder)
  .includeAll()
  .create();
```

**Single file** can be selected using a matcher
```java
Filter filter = Filter.getBuilder().matches("file.txt");

Source source = Source.getBuilder(/* Path */ pathToFolder)
  .include(filter)
  .create();
```

**Multiple files** can be included if they match one of the matchers.
The list of included files can be further filtered via exclude matchers
```java
Filter includeFilter = Filter.getBuilder()
                        .endsWith(".mp4")
                        .endsWith(".log")
                        .create();

Filter excludeFilter = Filter.getBuilder()
                        .matches("tmp.log")
                        .create();

Source source = Source.getBuilder(/* Path */ pathToFolder)
  .include(includeFilter)
  .exclude(excludeFilter)
  .create();
```

By default the library will not **traverse directories**, if you need this behaviour than you can set the `traverseDirectories` flag to true
```java
Source source = Source.getBuilder(/* Path */ pathToFolder)
  .traverseDirectories(true)
  .includeAll()
  .create();
```

If no include matcher is specified then an **IllegalArgumentException** will be raised upon creation:
```java
Source source = Source.getBuilder(/* Path */ pathToFolder)
  .create();
```

## Development

### Prepare environment

Configuration for running this service should be placed in file `.private/aws-test-secrets` in Java Properties file format. For examples.

```properties
aws_access_key_id=ABCDEFGHIJKLM
aws_secret_access_key=ABCDEFGHIJKLM
s3_region=ap-southeast-1
s3_bucket=bucketname
s3_prefix=prefix/
```

The values are:
* `aws_access_key_id` - access key to the AWS account.
* `aws_secret_access_key` - secret key to the AWS account.
* `s3_region` - this contains the region that holds the S3 bucket.
* `s3_bucket` the bucket that will store the uploaded files.
* `s3_prefix` S3 prefix that will be added before all files

### Run tests

Start Minio as a container
```
docker run -d -p 9000:9000 --rm \
    -e "MINIO_ACCESS_KEY=minio_access_key" \
    -e "MINIO_SECRET_KEY=minio_secret_key" \
    -e "MINIO_BROWSER=off" \
    minio/minio:RELEASE.2017-05-05T01-14-51Z server /data
```

Minio can be accessed via the normal AWS client
```
export AWS_ACCESS_KEY_ID=minio_access_key
export AWS_SECRET_ACCESS_KEY=minio_secret_key
export AWS_DEFAULT_REGION=us-east-1

aws --endpoint-url http://127.0.0.1:9000 s3api list-multipart-uploads --bucket localbucket
aws --endpoint-url http://127.0.0.1:9000 s3 ls
```

Run the local tests
```
./gradlew test -i
```
[s3-sync-stream-cli-0.0.17-all.jar](s3-sync-stream-cli/build/libs/s3-sync-stream-cli-0.0.17-all.jar)
### Build and run as command-line app
```bash
./gradlew clean shadowJar -i
java -Dlogback.configurationFile=`pwd`/logback.xml \
    -jar ./s3-sync-stream-cli/build/libs/s3-sync-stream-cli-0.0.17-all.jar \
    -c ./.private/aws-test-secrets \
    -d ./src/test/resources/test_a_1 \
    --filter "^[0-9a-zA-Z\\_]+\\.txt$"
```

### Install to mavenLocal

If you want to build the SNAPSHOT version locally you can install to the local Maven cache
```
./gradlew -x test clean install
```

### Inspect traffic with Charles Proxy

- Install Charles Proxy: https://www.charlesproxy.com/
- Enable SSL Proxying: https://www.charlesproxy.com/documentation/proxying/ssl-proxying/
- Add SSL host: `*.amazonaws.com`
- Export Charles certificate, `SSL Proxying > Save Charles Root Certificate`
- Import into Java Keystore
```
sudo keytool -import -alias charles \
  -file "${CERT_SAVE_LOCATION}/charles-ssl-proxying-certificate.pem" \
  -keystore "${JAVA_HOME}/jre/lib/security/cacerts" \
  -storepass changeit
```
- Traffic should appear in Charles

### Useful AWS commands

List multipart uploads
```
aws s3api list-multipart-uploads --bucket tdl-official-videos \
    > /tmp/uploads.aws
cat /tmp/uploads.aws | jq '.Uploads[] | {init:.Initiated, key: .Key, id:.UploadId}'
```

Abort multipart upload
```
 aws s3api abort-multipart-upload --bucket tdl-official-videos \
  --key CHK/frhh01/record-and-upload-20180609T172016.log \
  --upload-id qro14BxOJj1MfcCWd5U67BWgQwsCrRsKn5UqtN7PKAN753HShMSZR9KN11ySkm_ftLJMQoVO._KGb1Irrl3NjnLDerlsrtPt.iYR2YWynhXb1tnPRX5CkVOPNvoyq6A7tO8cyCcHiON8W3WArgGuMQ--
```

List parts
```
aws s3api list-parts \
  --bucket tdl-official-videos \
  --key "CHK/eijf01/sourcecode_20180611T071715.srcs" \
  --upload-id pDeAaCMyM9veZeS4t1sc7dZG9K58d3zVLPhoGFE_xc8I6jHatZ4EdLzpNZg2L6mHAe6s26AUiBFlqI0CDgwNCOG5b7am_iQjThOSgcoTu7fdGUQQa895yyPjxMxpu6wbADnf1JAEKVe6KQYSk.oC4Q-- \
  > /tmp/part.tags.aws
cat /tmp/part.tags.aws | jq '.Parts[] | {etag:.ETag, num:.PartNumber}'
```

Complete multipart
```
aws s3api complete-multipart-upload \
  --bucket tdl-official-videos \
  --key "CHK/sagp01/screencast_20180607T212124.mp4" \
  --multipart-upload '{"Parts":[{"ETag":"bee8593bf7085ce82a12708ade4b70b5","PartNumber":1},{"ETag":"d58e97a1c8aa3ed54ed1274e6972b428","PartNumber":2},{"ETag":"7ca7bf9efdd01ab39664711a574f0b48","PartNumber":3}]}' \
  --upload-id "jdB1Q.SRfhk0wdRalRHJNLvE8xEoiH5TiQPBrnG2_hkU1oc9wcQSQgM4FcEUmDxNuA2FGHUigd_0LwkovflgXupcQMXCuJ_xYML9ZtKlX4LS8PaXXxaNcA4WOexreZoZ.fZ_NxDHxqCbg15H6enZdg--"
```


### Release

Configure the version inside the "gradle.properties" file

Create publishing bundle into Maven Local
```bash
./gradlew publishToMavenLocal
```

Check Maven Local contains release version:
```
CURRENT_VERSION=$(cat gradle.properties | grep version | cut -d "=" -f2)

ls -l $HOME/.m2/repository/io/accelerate/s3-sync-stream/${CURRENT_VERSION}
```

Publish to Maven Central Staging repo

### Publish to Maven Central - the manual way

At this point publishing to Maven Central from Gradle is only possible manually.
Things might have changed, check this page:
https://central.sonatype.org/publish/publish-portal-gradle/

Generate the Maven Central bundle:
```
./generateMavenCentralBundle.sh
```

Upload the bundle to Maven Central by clicking the "Publish Component" button.
https://central.sonatype.com/publishing
