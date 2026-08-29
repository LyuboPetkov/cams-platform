package cams.service;

import cams.exception.InvalidFileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

// S3-compatible, not AWS-specific: endpointOverride + pathStyleAccessEnabled let
// the same client hit a local MinIO container in dev and a real S3/R2 bucket in prod.
@Service
public class StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrlBase;

    private volatile boolean bucketEnsured = false;

    public StorageService(@Value("${application.storage.endpoint}") String endpoint,
                          @Value("${application.storage.region}") String region,
                          @Value("${application.storage.access-key}") String accessKey,
                          @Value("${application.storage.secret-key}") String secretKey,
                          @Value("${application.storage.bucket}") String bucket,
                          @Value("${application.storage.public-url-base}") String publicUrlBase) {
        this.bucket = bucket;
        this.publicUrlBase = publicUrlBase;

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    public String upload(MultipartFile file, String key) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileUploadException("Only PNG, JPEG and WebP images are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileUploadException("File must be 5MB or smaller");
        }

        ensureBucketExists();

        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new InvalidFileUploadException("Failed to read uploaded file");
        }

        return publicUrlBase + "/" + key;
    }

    // Deferred to first upload so an unreachable storage backend doesn't
    // stop the app from starting. Guarded so this only runs once.
    private synchronized void ensureBucketExists() {
        if (bucketEnsured) {
            return;
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            applyPublicReadPolicy();
        }

        bucketEnsured = true;
    }

    // Best-effort: MinIO needs this so uploads are viewable via a plain <img src>.
    // On a real S3/R2 bucket the policy is set out of band and the prod credential
    // may lack PutBucketPolicy, so a failure here is fine.
    private void applyPublicReadPolicy() {
        try {
            String policy = """
                    {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
                    """.formatted(bucket);
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucket).policy(policy).build());
        } catch (Exception e) {
            // See comment above - a missing public-read policy is not fatal.
        }
    }
}
