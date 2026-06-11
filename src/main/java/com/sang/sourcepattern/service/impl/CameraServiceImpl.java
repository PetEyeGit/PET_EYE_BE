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

    @Value("${camera.api-url:http://camera:9997}")
    private String cameraApiUrl;

    @Value("${camera.stream-url-format:https://%s/camera/%s/index.m3u8}")
    private String streamUrlFormat;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String startStream(int bookingId, String rtspUrl) {
        String streamName = "booking-" + bookingId;
        log.info("Client requested stream {} for RTSP: {}", streamName, rtspUrl);

        try {
            // TRONG MÔ HÌNH PUSH (ĐẨY): 
            // Chúng ta KHÔNG ĐƯỢC ra lệnh cho MediaMTX đi kéo (pull) luồng video.
            // Nếu gửi API Pull, MediaMTX sẽ khóa đường dẫn này và đá văng FFmpeg ra ngoài.
            // Do đó, toàn bộ đoạn code gọi HTTP Request tới MediaMTX ở dưới đã được ẩn đi.
            
            /*
            stopStream(bookingId);
            String apiUrl = cameraApiUrl + "/v3/config/paths/add/" + streamName;
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
            */
            
            log.info("MediaMTX config skipped for Push Mode: {}", streamName);
            
            // Format URL based on environment (direct HTTP locally vs Nginx HTTPS on VPS)
            return String.format(streamUrlFormat, streamHost, streamName);
            
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
            // Tương tự, không cần xóa cấu hình Pull nữa.
            /*
            String apiUrl = cameraApiUrl + "/v3/config/paths/delete/" + streamName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .DELETE()
                    .build();
                    
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            */
            log.info("Successfully skipped removing MediaMTX stream for Push Mode: {}", streamName);
        } catch (Exception e) {
            log.warn("Failed to delete stream {}: {}", streamName, e.getMessage());
        }
    }
}
