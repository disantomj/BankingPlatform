package com.example.bankingplatform.notification;

import com.example.bankingplatform.user.User;
import com.example.bankingplatform.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(Integer userId, NotificationType type, String title, String message, String actionUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = new Notification(user, type, title, message, actionUrl);
        return notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Integer userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByUser_IdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    public Long getUnreadCount(Integer userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(Integer userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    public void cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Notification> oldNotifications = notificationRepository.findByCreatedAtBefore(cutoffDate);
        notificationRepository.deleteAll(oldNotifications);
    }

    @Transactional(readOnly = true)
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }
}