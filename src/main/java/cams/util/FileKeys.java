package cams.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

// Shared unique-object-key generator for profile picture / company logo uploads.
@Component
public class FileKeys {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp");

    // Fresh UUID per upload avoids filename collisions and path-traversal issues.
    public String uniqueName(MultipartFile file) {
        String extension = EXTENSIONS.getOrDefault(file.getContentType(), "bin");
        return UUID.randomUUID() + "." + extension;
    }
}
