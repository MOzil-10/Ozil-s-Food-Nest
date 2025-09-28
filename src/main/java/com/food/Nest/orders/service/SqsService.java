package com.food.Nest.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.Nest.orders.model.OrderEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SqsService {

    private static final Logger logger = LoggerFactory.getLogger(SqsService.class);

    @Autowired
    private final SqsTemplate sqsTemplate;

    @Autowired
    private final ObjectMapper objectMapper;

    @Autowired
    private final SqsClient sqsClient;

    @Value("${aws.sqs.order-processing-queue}")
    private String orderProcessingQueue;

    @Value("${aws.sqs.notification-queue}")
    private String notificationQueue;

    public CompletableFuture<Void> sendOrderForProcessing(OrderEvent orderEvent) {
        try {
            logger.info("Sending order event to processing queue: OrderId={}", orderEvent.getOrderId());
            sqsTemplate.send(orderProcessingQueue, orderEvent);
            logger.info("Order event sent successfully to queue: {}, OrderId={}", orderProcessingQueue, orderEvent.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to send order event to processing queue for OrderId: {}", orderEvent.getOrderId(), e);
            throw new RuntimeException("Failed to send order event", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> sendNotificationEvent(OrderEvent orderEvent) {
        try {
            logger.info("Sending notification event to queue: OrderId={}", orderEvent.getOrderId());
            sqsTemplate.send(notificationQueue, orderEvent);
            logger.info("Notification event sent successfully to queue: {}, OrderId={}", notificationQueue, orderEvent.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to send notification event for OrderId: {}", orderEvent.getOrderId(), e);
            throw new RuntimeException("Failed to send notification event", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public int getQueueMessageCount(String queueUrl) {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build();

            var response = sqsClient.getQueueAttributes(request);
            String count = response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
            return Integer.parseInt(count);
        } catch (Exception e) {
            logger.error("Failed to get queue message count for: {}", queueUrl, e);
            return 0;
        }
    }
}