package com.food.Nest.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.Nest.orders.model.OrderRequest;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private static final String ORDER_QUEUE = "food-order-queue";
    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public void sendOrderMessage(OrderRequest orderRequest) {
        try {
            String messageJson = objectMapper.writeValueAsString(orderRequest);
            sqsTemplate.send(ORDER_QUEUE, messageJson);
            log.info("Order message sent successfully to queue: {}", ORDER_QUEUE);
        } catch (Exception e) {
            log.error("Failed to send order message to queue: {}", ORDER_QUEUE, e);
            throw new RuntimeException("Failed to send order message", e);
        }
    }
}
