package com.food.Nest.orders.service;

import com.food.Nest.menu.model.MenuItem;
import com.food.Nest.menu.repository.MenuRepository;
import com.food.Nest.orders.model.OrderEvent;
import com.food.Nest.orders.model.OrderItem;
import com.food.Nest.orders.model.OrderItemRequest;
import com.food.Nest.orders.model.OrderRequest;
import com.food.Nest.orders.model.entity.OrderEntity;
import com.food.Nest.orders.model.entity.OrderStatus;
import com.food.Nest.orders.repository.OrderRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.food.Nest.orders.model.entity.OrderStatus.CONFIRMED;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private static final String NOTIFICATION_QUEUE = "order-notification-queue";

    @Autowired
    private final OrderRepository orderRepository;

    @Autowired
    private final MenuRepository menuRepository;

    @Autowired
    private final SqsTemplate sqsTemplate;

    public OrderEntity createOrder(OrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerEmail());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            MenuItem menuItem = menuRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemRequest.getMenuItemId()));

            if (!menuItem.getAvailable()) {
                throw new RuntimeException("Menu item is not available: " + menuItem.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItem.setSpecialInstructions(itemRequest.getSpecialInstructions());
            orderItem.calculateAndSetTotalPrice();

            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            orderItems.add(orderItem);
        }

        OrderEntity order = new OrderEntity();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
        }

        return orderRepository.save(order);
    }

    public Optional<OrderEntity> getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<OrderEntity> getOrdersByCustomerEmail(String customerEmail) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail);
    }

    public List<OrderEntity> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<OrderEntity> getPendingOrders() {
        return getOrdersByStatus(OrderStatus.PENDING);
    }

    public OrderStatistics getOrderStatistics() {
        OrderStatistics stats = new OrderStatistics();
        stats.setPendingCount(orderRepository.countByStatus(OrderStatus.PENDING));
        stats.setConfirmedCount(orderRepository.countByStatus(OrderStatus.CONFIRMED));
        stats.setPreparingCount(orderRepository.countByStatus(OrderStatus.PREPARING));
        stats.setReadyCount(orderRepository.countByStatus(OrderStatus.READY));
        stats.setOutForDeliveryCount(orderRepository.countByStatus(OrderStatus.OUT_FOR_DELIVERY));
        stats.setDeliveredCount(orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.setCancelledCount(orderRepository.countByStatus(OrderStatus.CANCELLED));

        return stats;
    }

    public OrderEntity updateOrderStatus(Long orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        switch (newStatus) {
            case CONFIRMED:
                order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(40));
                break;
            case PREPARING:
                order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(25));
                break;
            case READY:
                order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(15));
                break;
            case OUT_FOR_DELIVERY:
                order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(10));
                break;
            case CANCELLED:
            case DELIVERED:
                order.setEstimatedDeliveryTime(null);
                break;
            default:
                break;
        }

        OrderEntity updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        OrderEvent orderEvent = new OrderEvent(
                order.getOrderId(),
                order.getStatus(),
                LocalDateTime.now(),
                order.getCustomerEmail(),
                order.getCustomerName(),
                "ORDER_STATUS_UPDATED"
        );

        try {
            sqsTemplate.send(NOTIFICATION_QUEUE, orderEvent);
            log.info("Order status update event sent to queue: {}", NOTIFICATION_QUEUE);
        } catch (Exception e) {
            log.error("Failed to send order status update event to queue: {}", NOTIFICATION_QUEUE, e);
            throw new RuntimeException("Failed to send order status update event", e);
        }

        return updatedOrder;
    }

    public OrderEntity cancelOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    public static class OrderStatistics {
        private Long pendingCount;
        private Long confirmedCount;
        private Long preparingCount;
        private Long readyCount;
        private Long outForDeliveryCount;
        private Long deliveredCount;
        private Long cancelledCount;

        public Long getPendingCount() { return pendingCount; }
        public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }

        public Long getConfirmedCount() { return confirmedCount; }
        public void setConfirmedCount(Long confirmedCount) { this.confirmedCount = confirmedCount; }

        public Long getPreparingCount() { return preparingCount; }
        public void setPreparingCount(Long preparingCount) { this.preparingCount = preparingCount; }

        public Long getReadyCount() { return readyCount; }
        public void setReadyCount(Long readyCount) { this.readyCount = readyCount; }

        public Long getOutForDeliveryCount() { return outForDeliveryCount; }
        public void setOutForDeliveryCount(Long outForDeliveryCount) { this.outForDeliveryCount = outForDeliveryCount; }

        public Long getDeliveredCount() { return deliveredCount; }
        public void setDeliveredCount(Long deliveredCount) { this.deliveredCount = deliveredCount; }

        public Long getCancelledCount() { return cancelledCount; }
        public void setCancelledCount(Long cancelledCount) { this.cancelledCount = cancelledCount; }
    }
}