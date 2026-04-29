package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Feedback;
import com.ecommerce.mobile.enums.FeedbackStatus;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<Feedback> findByCustomerUserIDOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<Feedback> findByEmployeeUserIDOrderByCreatedAtDesc(Long employeeId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);

    @EntityGraph(attributePaths = {"customer", "employee"})
    Optional<Feedback> findByFeedbackIdAndCustomerUserID(Long feedbackId, Long customerId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    Optional<Feedback> findByFeedbackIdAndEmployeeUserID(Long feedbackId, Long employeeId);
}
