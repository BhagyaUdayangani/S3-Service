package com.example.s3_bucket.util;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ValidateImageVideo {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("heic", "jpg", "jpeg", "png");

    public boolean isVideo(String extension) {
        return VIDEO_EXTENSIONS.contains(extension.toLowerCase());
    }

    public boolean isImage(String extension) {
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

}
