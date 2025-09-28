package com.food.Nest.menu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SqsClient sqsClient;

    @Value("${aws.sqs.order-processing-queue}")
    private String orderProcessingQueueUrl;

    @Value("${aws.sqs.notification-queue}")
    private String notificationQueueUrl;

    public AdminController(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @GetMapping("/queue-status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("orderProcessingQueueMessages", getQueueMessageCount(orderProcessingQueueUrl));
        status.put("notificationQueueMessages", getQueueMessageCount(notificationQueueUrl));
        status.put("orderProcessingQueueUrl", orderProcessingQueueUrl);
        status.put("notificationQueueUrl", notificationQueueUrl);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(health);
    }

    private int getQueueMessageCount(String queueUrl) {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build();
            var response = sqsClient.getQueueAttributes(request);
            String count = response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
            return Integer.parseInt(count);
        } catch (Exception e) {
            return 0;
        }
    }
}