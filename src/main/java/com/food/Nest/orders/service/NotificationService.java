package com.food.Nest.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.Nest.orders.model.OrderEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    @Autowired
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.notification-queue}")
    private String notificationQueue;

    @SqsListener(value = "${aws.sqs.notification-queue}")
    public void processNotificationQueue(OrderEvent orderEvent) {
        try {
            log.info("Received notification event: OrderId={}, EventType={}, Status={}",
                    orderEvent.getOrderId(), orderEvent.getEventType(), orderEvent.getStatus());

            sendNotificationToCustomer(orderEvent);

        } catch (Exception e) {
            log.error("Failed to process notification event for OrderId: {}", orderEvent.getOrderId(), e);
            throw new RuntimeException("Failed to process notification event", e);
        }
    }

    private void sendNotificationToCustomer(OrderEvent orderEvent) {
        try {
            String notificationMessage = generateNotificationMessage(orderEvent);

            // Simulate sending email/SMS
            log.info("📧 NOTIFICATION TO {}: {}", orderEvent.getCustomerEmail(), notificationMessage);
            Thread.sleep(500);

        } catch (Exception e) {
            log.error("Failed to send notification for OrderId: {}", orderEvent.getOrderId(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    /**
     * Generate notification message based on order event
     */
    private String generateNotificationMessage(OrderEvent orderEvent) {
        String customerName = orderEvent.getCustomerName();
        Long orderId = orderEvent.getOrderId();

        switch (orderEvent.getStatus()) {
            case CONFIRMED:
                return String.format("Hi %s! Your order #%d has been confirmed and is being prepared. Thank you for choosing us!",
                        customerName, orderId);

            case PREPARING:
                return String.format("Hi %s! Your order #%d is now being prepared by our kitchen team. We'll notify you when it's ready!",
                        customerName, orderId);

            case READY:
                return String.format("Hi %s! Your order #%d is ready for pickup/delivery! Our driver will be with you soon.",
                        customerName, orderId);

            case OUT_FOR_DELIVERY:
                return String.format("Hi %s! Your order #%d is out for delivery! It should arrive within 10-15 minutes.",
                        customerName, orderId);

            case DELIVERED:
                return String.format("Hi %s! Your order #%d has been delivered! We hope you enjoy your meal. Please rate your experience!",
                        customerName, orderId);

            case CANCELLED:
                return String.format("Hi %s, we're sorry to inform you that your order #%d has been cancelled. You will receive a full refund within 3-5 business days.",
                        customerName, orderId);

            default:
                return String.format("Hi %s! There's an update on your order #%d. Current status: %s",
                        customerName, orderId, orderEvent.getStatus().name());
        }
    }
}