package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.service.CameraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.List;

@Service
@Slf4j
public class CameraServiceImpl implements CameraService {

    @Value("${camera.stream-host:localhost}")
    private String streamHost;

    @Override
    public String startStream(int bookingId, String rtspUrl) {
        int port = findFreePort();
        stopStream(bookingId); // Clean up any existing container for this booking

        log.info("Starting MediaMTX Docker container for booking {} with RTSP: {} on port {}", bookingId, rtspUrl, port);

        // Run Docker command in background:
        List<String> command = List.of(
                "docker", "run", "-d",
                "--name", "peteye-camera-" + bookingId,
                "-p", port + ":8888",
                "-e", "MTX_PATHS_STREAM_SOURCE=" + rtspUrl,
                "-e", "MTX_READCORS=yes",
                "-e", "MTX_PATHS_STREAM_SOURCEPROTOCOL=tcp",
                "bluenviron/mediamtx"
        );

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            // Read output to avoid hang and print debug info
            StringBuilder output = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Failed to start docker container for booking {}. Exit code: {}. Output: {}", bookingId, exitCode, output.toString().trim());
                throw new AppException(ErrorCode.DOCKER_NOT_RUNNING);
            }
            
            log.info("Successfully started docker container for booking {}", bookingId);
            return "http://" + streamHost + ":" + port + "/stream/index.m3u8";
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error running docker for booking {}", bookingId, e);
            throw new AppException(ErrorCode.DOCKER_NOT_RUNNING);
        }
    }

    @Override
    public void stopStream(int bookingId) {
        log.info("Stopping and removing MediaMTX Docker container for booking {}", bookingId);
        List<String> rmCommand = List.of("docker", "rm", "-f", "peteye-camera-" + bookingId);
        try {
            ProcessBuilder builder = new ProcessBuilder(rmCommand);
            builder.redirectErrorStream(true);
            Process rmProc = builder.start();
            
            // Consume output to prevent process hang
            try (BufferedReader r = new BufferedReader(new InputStreamReader(rmProc.getInputStream()))) {
                while (r.readLine() != null) {
                    // discard
                }
            }
            rmProc.waitFor();
            log.info("Successfully stopped and removed docker container for booking {}", bookingId);
        } catch (Exception e) {
            log.warn("Failed to stop container for booking {} (it may not exist): {}", bookingId, e.getMessage());
        }
    }

    private int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            log.error("Error finding free port, defaulting to 8888", e);
            return 8888;
        }
    }
}
