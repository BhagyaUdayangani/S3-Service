package com.example.s3_bucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class ProcessedFileInfo {
    private File originalFile;
    private File processedFile;
    private String finalFilename;
    private String extensionType;
    private boolean inappropriate;
}
