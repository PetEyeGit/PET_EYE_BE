package com.sang.sourcepattern.service;

public interface CameraService {
    String startStream(int bookingId, String rtspUrl);
    void stopStream(int bookingId);
}
