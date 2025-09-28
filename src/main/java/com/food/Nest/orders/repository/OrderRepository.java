package com.food.Nest.orders.repository;

import com.food.Nest.orders.model.entity.OrderEntity;
import com.food.Nest.orders.model.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Query("SELECT o FROM OrderEntity o " +
            "WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<OrderEntity> findOrdersBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);
}
