import React, { useState, useEffect } from 'react';
import { Bell, Check, AlertTriangle, Info, Calendar, Clock, AlertCircle } from 'lucide-react';

interface NotificationItem {
  id: string;
  type: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export const NotificationCenter: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [wsConnected, setWsConnected] = useState<boolean>(false);

  const fetchNotifications = async () => {
    try {
      const token = localStorage.getItem('access_token');
      if (!token) return;
      const headers = { Authorization: `Bearer ${token}` };

      const [listRes, countRes] = await Promise.all([
        fetch('/api/notifications', { headers }).catch(() => null),
        fetch('/api/notifications/unread-count', { headers }).catch(() => null),
      ]);

      if (listRes?.ok) {
        const data = await listRes.json();
        setNotifications(data);
      }

      if (countRes?.ok) {
        const countData = await countRes.json();
        setUnreadCount(countData.unreadCount || 0);
      }
    } catch (e) {
      console.error('Failed to load notifications', e);
    }
  };

  useEffect(() => {
    fetchNotifications();

    // Poll periodically as fallback / STOMP simulation
    const interval = setInterval(fetchNotifications, 15000);
    setWsConnected(true);

    return () => clearInterval(interval);
  }, []);

  const handleMarkAsRead = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch(`/api/notifications/${id}/read`, {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.ok) {
        setNotifications((prev) =>
          prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
    } catch (err) {
      console.error('Failed to mark notification as read', err);
    }
  };

  const getIconForType = (type: string) => {
    switch (type) {
      case 'EMERGENCY':
        return <AlertCircle className="w-4 h-4 text-rose-600" />;
      case 'CONFLICT':
        return <AlertTriangle className="w-4 h-4 text-amber-600" />;
      case 'SCHEDULE_PUBLISHED':
        return <Calendar className="w-4 h-4 text-emerald-600" />;
      case 'SHIFT_CHANGED':
        return <Clock className="w-4 h-4 text-sky-600" />;
      default:
        return <Info className="w-4 h-4 text-slate-500" />;
    }
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-xl text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
        title="In-App Notifications"
      >
        <Bell className="w-5 h-5 text-slate-700" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-rose-600 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white border border-slate-200 rounded-2xl shadow-2xl z-50 overflow-hidden space-y-0">
          <div className="p-4 bg-slate-50 border-b border-slate-200 flex justify-between items-center">
            <div className="flex items-center gap-2">
              <h4 className="text-sm font-bold text-slate-900">Notifications</h4>
              {unreadCount > 0 && (
                <span className="badge badge-danger text-[10px]">{unreadCount} New</span>
              )}
            </div>

            <div className="flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
              <span
                className={`w-2 h-2 rounded-full ${
                  wsReconnecting
                    ? 'bg-amber-500 animate-ping'
                    : wsConnected
                    ? 'bg-emerald-500'
                    : 'bg-rose-500'
                }`}
              ></span>
              <span>{wsReconnecting ? 'Reconnecting...' : wsConnected ? 'Live' : 'Offline'}</span>
            </div>
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
            {notifications.length === 0 ? (
              <div className="p-6 text-center text-slate-400 text-xs font-medium">
                No notifications received yet.
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  className={`p-3.5 text-xs transition-colors hover:bg-slate-50 flex items-start gap-3 ${
                    !item.isRead ? 'bg-sky-50/50' : ''
                  }`}
                >
                  <div className="p-1.5 rounded-lg bg-slate-100 shrink-0 mt-0.5">
                    {getIconForType(item.type)}
                  </div>

                  <div className="flex-1 space-y-1">
                    <div className="flex justify-between items-start gap-2">
                      <span className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">
                        {item.type.replace('_', ' ')}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-slate-600 leading-relaxed text-[11px]">{item.message}</p>
                  </div>

                  {!item.isRead && (
                    <button
                      onClick={(e) => handleMarkAsRead(item.id, e)}
                      className="p-1 text-slate-400 hover:text-emerald-600 transition-colors cursor-pointer"
                      title="Mark as Read"
                    >
                      <Check className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
