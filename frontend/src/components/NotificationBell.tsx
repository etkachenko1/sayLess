import { useEffect, useRef, useState } from "react";
import { Bell, X } from "lucide-react";
import type { Notification } from "../utils/websocket";

interface NotificationBellProps {
  notifications: Notification[];
  onMarkAllRead: () => void;
  onDelete: (id: string) => void;
  onClearAll: () => void;
}

function formatTime(dateString: string) {
  const diffMs = Date.now() - new Date(dateString).getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return "just now";
  if (diffMin < 60) return `${diffMin} min ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  return new Date(dateString).toLocaleDateString();
}

export default function NotificationBell({ notifications, onMarkAllRead, onDelete, onClearAll }: NotificationBellProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const unreadCount = notifications.filter((n) => !n.read).length;

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleToggle = () => {
    setIsOpen((prev) => {
      const next = !prev;
      if (next) onMarkAllRead();
      return next;
    });
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={handleToggle}
        className="relative bg-gray-700 text-gray-200 p-2.5 rounded-lg hover:bg-gray-600"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-600 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="fixed left-4 right-4 sm:absolute sm:left-auto sm:right-0 mt-2 sm:w-80 bg-gray-800 border border-gray-700 rounded-xl shadow-lg z-50 max-h-96 overflow-y-auto">
          <div className="px-4 py-3 border-b border-gray-700 flex items-center justify-between">
            <span className="font-semibold text-gray-200">Notifications</span>
            {notifications.length > 0 && (
              <button
                onClick={onClearAll}
                className="text-xs text-gray-400 hover:text-gray-200"
              >
                Clear all
              </button>
            )}
          </div>
          {notifications.length === 0 ? (
            <div className="px-4 py-6 text-center text-gray-500 text-sm">
              No notifications yet
            </div>
          ) : (
            notifications.map((n) => (
              <div
                key={n.id}
                className="px-4 py-3 border-b border-gray-700 last:border-0 flex items-start justify-between gap-2"
              >
                <div>
                  <p className="text-sm text-gray-200">{n.message}</p>
                  <p className="text-xs text-gray-500 mt-1">{formatTime(n.createdAt)}</p>
                </div>
                <button
                  onClick={() => onDelete(n.id)}
                  className="text-gray-500 hover:text-gray-200 shrink-0"
                  aria-label="Dismiss notification"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}