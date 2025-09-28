package com.food.Nest.messaging;

import com.food.Nest.orders.model.OrderRequest;
import com.food.Nest.orders.model.entity.OrderEntity;
import com.food.Nest.orders.service.OrderService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private static final String ORDER_QUEUE = "food-order-queue";
    private final OrderService orderService;

    @SqsListener(value = ORDER_QUEUE)
    public void consumeOrderMessage(OrderRequest orderRequest) {
        try {
            log.info("Received order message for customer: {}", orderRequest.getCustomerEmail());
            OrderEntity order = orderService.createOrder(orderRequest);
            log.info("Order processed and saved with ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process order message for customer: {}", orderRequest.getCustomerEmail(), e);
            throw new RuntimeException("Failed to process order message", e);
        }
    }
}
