import React from 'react';
import { Bell, Trash2, Check } from 'lucide-react';
import { useAppSelector, useAppDispatch } from '../hooks/useRedux';
import { markAsRead, markAllAsRead, deleteNotification, clearAllNotifications } from '../store/notificationSlice';
import { formatDate } from '../utils/format';

const Notifications: React.FC = () => {
  const dispatch = useAppDispatch();
  const { items, unreadCount } = useAppSelector((state) => state.notifications);

  const handleMarkAsRead = (id: string) => {
    dispatch(markAsRead(id));
  };

  const handleMarkAllAsRead = () => {
    dispatch(markAllAsRead());
  };

  const handleDelete = (id: string) => {
    dispatch(deleteNotification(id));
  };

  const handleClearAll = () => {
    if (window.confirm('Are you sure you want to clear all notifications?')) {
      dispatch(clearAllNotifications());
    }
  };

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'success':
        return 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200';
      case 'error':
        return 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200';
      case 'warning':
        return 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200';
      default:
        return 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Notifications</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">
            {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount !== 1 ? 's' : ''}` : 'All caught up!'}
          </p>
        </div>
        {items.length > 0 && (
          <div className="flex space-x-2">
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllAsRead}
                className="flex items-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
              >
                <Check size={20} />
                <span>Mark All Read</span>
              </button>
            )}
            <button
              onClick={handleClearAll}
              className="flex items-center space-x-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              <Trash2 size={20} />
              <span>Clear All</span>
            </button>
          </div>
        )}
      </div>

      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg">
        {items.length > 0 ? (
          <div className="space-y-3">
            {items.map((notification) => (
              <div
                key={notification.id}
                className={`p-4 rounded-lg ${
                  notification.read ? 'bg-gray-50 dark:bg-gray-700' : 'bg-blue-50 dark:bg-blue-900/20 border-l-4 border-blue-500'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-2 mb-1">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getNotificationColor(notification.type)}`}>
                        {notification.type}
                      </span>
                      {!notification.read && (
                        <span className="px-2 py-1 bg-blue-500 text-white rounded-full text-xs font-medium">
                          New
                        </span>
                      )}
                    </div>
                    <h3 className="font-semibold text-gray-900 dark:text-white mb-1">
                      {notification.title}
                    </h3>
                    <p className="text-gray-600 dark:text-gray-400 text-sm mb-2">
                      {notification.message}
                    </p>
                    <p className="text-gray-500 dark:text-gray-500 text-xs">
                      {formatDate(notification.timestamp, 'long')}
                    </p>
                  </div>
                  <div className="flex space-x-2 ml-4">
                    {!notification.read && (
                      <button
                        onClick={() => handleMarkAsRead(notification.id)}
                        className="p-2 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-lg transition-colors"
                        title="Mark as read"
                      >
                        <Check className="w-4 h-4 text-gray-600 dark:text-gray-400" />
                      </button>
                    )}
                    <button
                      onClick={() => handleDelete(notification.id)}
                      className="p-2 hover:bg-red-100 dark:hover:bg-red-900/20 rounded-lg transition-colors"
                      title="Delete"
                    >
                      <Trash2 className="w-4 h-4 text-red-600 dark:text-red-400" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <Bell className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">
              No Notifications
            </h3>
            <p className="text-gray-600 dark:text-gray-400">
              You're all caught up! Check back later for updates.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Notifications;
