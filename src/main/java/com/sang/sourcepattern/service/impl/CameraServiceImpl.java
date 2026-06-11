package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.service.CameraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class CameraServiceImpl implements CameraService {

    @Value("${camera.stream-host:localhost}")
    private String streamHost;

    private static final String MTX_API_BASE = "http://camera:9997/v3/config/paths/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String startStream(int bookingId, String rtspUrl) {
        String streamName = "booking-" + bookingId;
        log.info("Configuring MediaMTX stream {} for RTSP: {}", streamName, rtspUrl);

        try {
            // Stop existing stream if any
            stopStream(bookingId);

            String apiUrl = MTX_API_BASE + "add/" + streamName;
            
            // Construct JSON for MediaMTX to read from the RTSP source via TCP
            String jsonBody = "{\"source\": \"" + rtspUrl + "\", \"sourceProtocol\": \"tcp\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Failed to add MediaMTX stream. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new AppException(ErrorCode.DOCKER_NOT_RUNNING);
            }
            
            log.info("Successfully configured MediaMTX stream {}", streamName);
            
            // Return the Nginx proxied URL path (https://api.peteye.com.vn/camera/booking-X/index.m3u8)
            // Note: Cloudflare and Browsers require HTTPS, and we proxy /camera/ to the global MediaMTX container
            return "https://" + streamHost + "/camera/" + streamName + "/index.m3u8";
            
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error configuring MediaMTX for booking {}", bookingId, e);
            throw new AppException(ErrorCode.DOCKER_NOT_RUNNING);
        }
    }

    @Override
    public void stopStream(int bookingId) {
        String streamName = "booking-" + bookingId;
        log.info("Removing MediaMTX stream {}", streamName);
        try {
            String apiUrl = MTX_API_BASE + "delete/" + streamName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .DELETE()
                    .build();
                    
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // Ignore response code, it might be 404 if it didn't exist
            log.info("Successfully removed MediaMTX stream {}", streamName);
        } catch (Exception e) {
            log.warn("Failed to delete stream {}: {}", streamName, e.getMessage());
        }
    }
}
