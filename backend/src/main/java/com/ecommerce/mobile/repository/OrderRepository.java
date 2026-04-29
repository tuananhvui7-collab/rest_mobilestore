package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.images", "customer", "payments", "shipment", "voucher"})
    Optional<Order> findDetailedByOrderId(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.images", "customer", "payments", "shipment", "voucher"})
    List<Order> findByCustomerUserIDOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.images", "customer", "payments", "shipment", "voucher"})
    List<Order> findAllByOrderByCreatedAtDesc();

    Optional<Order> findByOrderCode(String orderCode);
}
