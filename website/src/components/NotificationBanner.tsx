import React, { useEffect } from 'react';
import { X, Bell } from 'lucide-react';

interface NotificationBannerProps {
  notification: {
    id: number;
    title: string;
    body: string;
  } | null;
  onDismiss: () => void;
}

export const NotificationBanner: React.FC<NotificationBannerProps> = ({
  notification,
  onDismiss,
}) => {
  useEffect(() => {
    if (!notification) return;
    const timer = setTimeout(() => {
      onDismiss();
    }, 4500);
    return () => clearTimeout(timer);
  }, [notification, onDismiss]);

  if (!notification) return null;

  return (
    <div className="fixed top-20 right-4 sm:right-6 z-50 w-80 sm:w-96 rounded-2xl bg-[#1e293b]/95 backdrop-blur-2xl border border-white/10 shadow-[0_12px_36px_rgba(0,0,0,0.6)] p-3.5 flex flex-col gap-1 text-left animate-slideDown select-none">
      {/* Top row */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <img
            alt="MyToken"
            className="w-4 h-4 object-contain"
            src="/app-icon.png"
          />
          <span className="font-semibold text-xs text-[#F8FAFC]">MyToken</span>
          <span className="text-[10px] text-[#94A3B8] font-mono">· 刚刚</span>
        </div>

        <button
          onClick={onDismiss}
          className="p-1 rounded text-[#94A3B8] hover:text-white hover:bg-white/5 transition-colors cursor-pointer"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Title & Body */}
      <div className="pl-6">
        <div className="text-xs font-semibold text-[#F8FAFC] leading-snug">
          {notification.title}
        </div>
        <div className="text-xs text-[#c1c6d7] mt-0.5 leading-relaxed">
          {notification.body}
        </div>
      </div>
    </div>
  );
};
