package com.example.s3_bucket.service;

import java.io.*;

public interface VideoCompressionService {

    void compressVideo(File inputFile, File outputFile) throws IOException;

}
