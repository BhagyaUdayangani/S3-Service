package com.example.s3_bucket.service.Impl;

import com.example.s3_bucket.service.VideoCompressionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoCompressionServiceImpl implements VideoCompressionService {

    @Value("${video.compression.timeout:300}") // 5 minutes default timeout
    private int processTimeoutSeconds;

    @Value("${video.compression.preset:medium}") // balanced preset between speed and compression
    private String compressionPreset;

    @Override
    public void compressVideo(File inputFile, File outputFile) throws IOException {
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Input file does not exist: " + inputFile.getAbsolutePath());
        }

        Process process = null;
        try {
            log.info("LOG:: Starting video compression process for file: {}", inputFile.getAbsolutePath());
            process = getProcess(inputFile, outputFile);
            log.info("LOG:: Video compression process started for file: {}", inputFile.getAbsolutePath());
            // Handle process output in a separate thread to prevent blocking
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
            
            outputGobbler.start();
            errorGobbler.start();

            // Wait for a process with timeout
            if (!process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Video compression timed out after " + processTimeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Video compression failed with exit code: " + exitCode);
            }

            // Verify the output file was created and has size > 0
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("Output file was not created or is empty");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Video compression process was interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private Process getProcess(File inputFile, File outputFile) throws IOException {
        log.info("LOG:: Starting video compression process for file");
        // First, probe the video to detect rotation
        String rotation = getVideoRotation(inputFile);
        log.info("LOG:: Detected video rotation: {}", rotation);

        ProcessBuilder processBuilder = new ProcessBuilder();

        // Base command
        List<String> command = new ArrayList<>(Arrays.asList(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-r", "30",                           // 30 FPS (matches Instagram's standard)
                "-vf", "scale=1080:-2",                 // 1080p width, auto height (divisible by 2)
                "-c:v", "libx264",                     // Correct codec (required by Instagram)
                "-preset", compressionPreset,          // Good choice (balance speed/quality)
                "-crf", "23",                          // Good range (18-28, lower = better)
                "-b:v", "5M",                        // Bitrate for 1080p30 (~5 Mbps)
                "-maxrate", "5M",                   // Instagram's recommended 3.5–4.5Mbps
                "-bufsize", "5M",                   // 2x maxrate (standard for streaming)
                "-pix_fmt", "yuv420p",                // Mandatory for compatibility
                "-movflags", "+faststart",            // Required for web playback
                "-c:a", "aac",                        // Correct audio codec
                "-b:a", "192k",                       // Higher quality within Instagram's 128–256k range
                "-ar", "44100",                       // Instagram prefers 44.1kHz, not 48kHz
                "-y"                                  // Overwrite output
        ));

        // Handle rotation if detected
        if (rotation != null) {
            command.addAll(Arrays.asList("-metadata:s:v", "rotate=0"));
        }

        command.add(outputFile.getAbsolutePath());
        processBuilder.command(command);
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    private String getVideoRotation(File inputFile) throws IOException {
        ProcessBuilder probeBuilder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream_tags=rotate",
                "-of", "default=nw=1:nk=1",
                inputFile.getAbsolutePath()
        );

        Process process = probeBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String rotation = reader.readLine();
            if (rotation != null && !rotation.isEmpty()) {
                return rotation;
            }
        } catch (IOException e) {
            log.error("Error probing video rotation", e);
        }
        return null;
    }

    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading process stream", e);
            }
        }
    }
}