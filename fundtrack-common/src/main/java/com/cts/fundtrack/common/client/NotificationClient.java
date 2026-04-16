package com.cts.fundtrack.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.config.FeignConfig;

@FeignClient(
    name = "fundtrack-notification-service", 
    configuration = FeignConfig.class
)
public interface NotificationClient {

    @PostMapping("/api/v1/internal/notifications/send")
    void sendNotification(@RequestBody NotificationRequestDTO request);
}