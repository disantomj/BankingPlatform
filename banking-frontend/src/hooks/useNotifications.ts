import { useState, useEffect } from 'react';
import { apiClient } from '@/lib/api/client';

export interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  actionUrl?: string;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

export function useNotifications(userId?: number) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchNotifications = async () => {
    if (!userId) return;

    setIsLoading(true);
    setError(null);

    try {
      const response = await apiClient.getUserNotifications(userId);

      if (response.success) {
        setNotifications(response.data || []);
      } else {
        setError(response.error || 'Failed to fetch notifications');
      }
    } catch (err) {
      setError('An error occurred while fetching notifications');
      console.error('Error fetching notifications:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchUnreadCount = async () => {
    if (!userId) return;

    try {
      const response = await apiClient.getUnreadNotificationsCount(userId);

      if (response.success) {
        setUnreadCount(response.data || 0);
      }
    } catch (err) {
      console.error('Error fetching unread count:', err);
    }
  };

  const markAsRead = async (notificationId: number) => {
    try {
      const response = await apiClient.markNotificationAsRead(notificationId);

      if (response.success) {
        setNotifications(prev =>
          prev.map(notif =>
            notif.id === notificationId
              ? { ...notif, isRead: true, readAt: new Date().toISOString() }
              : notif
          )
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
        return { success: true };
      } else {
        return { success: false, error: response.error };
      }
    } catch (err) {
      console.error('Error marking notification as read:', err);
      return { success: false, error: 'Failed to mark as read' };
    }
  };

  const markAllAsRead = async () => {
    if (!userId) return { success: false, error: 'No user ID' };

    try {
      const response = await apiClient.markAllNotificationsAsRead(userId);

      if (response.success) {
        setNotifications(prev =>
          prev.map(notif => ({
            ...notif,
            isRead: true,
            readAt: notif.isRead ? notif.readAt : new Date().toISOString()
          }))
        );
        setUnreadCount(0);
        return { success: true };
      } else {
        return { success: false, error: response.error };
      }
    } catch (err) {
      console.error('Error marking all notifications as read:', err);
      return { success: false, error: 'Failed to mark all as read' };
    }
  };

  const deleteNotification = async (notificationId: number) => {
    try {
      const response = await apiClient.deleteNotification(notificationId);

      if (response.success) {
        const deletedNotification = notifications.find(n => n.id === notificationId);
        setNotifications(prev => prev.filter(notif => notif.id !== notificationId));

        if (deletedNotification && !deletedNotification.isRead) {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }

        return { success: true };
      } else {
        return { success: false, error: response.error };
      }
    } catch (err) {
      console.error('Error deleting notification:', err);
      return { success: false, error: 'Failed to delete notification' };
    }
  };

  useEffect(() => {
    if (userId) {
      fetchNotifications();
      fetchUnreadCount();
    }
  }, [userId]);

  return {
    notifications,
    unreadCount,
    isLoading,
    error,
    refetch: fetchNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification
  };
}