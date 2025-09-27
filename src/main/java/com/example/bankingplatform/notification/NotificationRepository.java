package com.example.bankingplatform.notification;

import com.example.bankingplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, Boolean isRead);

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUser_IdAndIsReadOrderByCreatedAtDesc(Integer userId, Boolean isRead);

    Long countByUser_IdAndIsReadFalse(Integer userId);

    List<Notification> findByUser_IdAndTypeOrderByCreatedAtDesc(Integer userId, NotificationType type);

    List<Notification> findByCreatedAtBefore(LocalDateTime cutoffDate);
}