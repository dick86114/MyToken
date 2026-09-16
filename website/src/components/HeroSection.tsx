import React from 'react';
import { ProviderItem } from '../types';
import { MenuBarSimulator } from './MenuBarSimulator';
import {
  Download,
  Smartphone,
  CloudOff,
  LayoutGrid,
  Lock,
  History,
} from 'lucide-react';
import { useLatestDownloads } from '../hooks/useLatestDownloads';

interface HeroSectionProps {
  providers: ProviderItem[];
  onOpenSettings: () => void;
  onTriggerNotification: (title: string, body: string) => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({
  providers,
  onOpenSettings,
  onTriggerNotification,
}) => {
  const downloads = useLatestDownloads();
  return (
    <section className="relative w-full overflow-hidden py-12 lg:py-20 flex flex-col items-center">
      {/* Background ambient lighting */}
      <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_80%_50%_at_50%_-10%,rgba(0,128,255,0.12),rgba(15,19,28,0))] z-0" />

      <div className="max-w-[1200px] mx-auto px-4 sm:px-6 w-full flex flex-col items-center text-center relative z-10">
        {/* Top Tagline Badge */}
        <div
          id="hero-top-badge"
          className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#262a33]/90 text-[#7bd0ff] font-mono text-xs mb-6 border border-white/10 shadow-sm"
        >
          <span className="w-2 h-2 rounded-full bg-[#10B981] animate-pulse" />
          <span>macOS 14+ 与 Android 10+ 双端体验 · 本地运行无云端中转</span>
        </div>

        {/* Main Headline */}
        <h1
          id="hero-headline"
          className="font-headline-hero text-[#F8FAFC] tracking-tight max-w-4xl mx-auto mb-4"
        >
          在菜单栏看清每个{' '}
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#abc7ff] via-[#7bd0ff] to-[#c0c1ff]">
            AI 账户的额度
          </span>
        </h1>

        {/* Subheadline */}
        <p className="font-body-large text-[#c1c6d7] max-w-3xl mx-auto mb-8 leading-relaxed">
          MyToken 是本地运行的 macOS 菜单栏用量监控工具，并提供 Android 伴侣应用。一次配置 Routin、DeepSeek、GLM、火山方舟、New API、Command Code 或小米 MiMo 凭证，即可分别查看套餐周期、5 小时/周限额、月度额度、Token 资源包、余额、购买/赠送剩余、调用次数、重置时间和账户状态。
        </p>

        {/* Primary & Secondary CTAs */}
        <div className="flex flex-wrap items-center justify-center gap-4 mb-10">
          <a
            id="hero-download-btn"
            href={downloads.macos.url}
            target="_blank"
            rel="noopener noreferrer"
            className="group relative inline-flex items-center gap-2.5 px-6 py-3 rounded-xl bg-[#438fff] hover:bg-[#00a6e0] text-[#002959] text-sm sm:text-base font-semibold shadow-[0_0_30px_0_rgba(67,143,255,0.4)] transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
          >
            <Download className="w-5 h-5 transition-transform group-hover:-translate-y-0.5" />
            <span>下载 MyToken.dmg</span>
            <span className="text-[#002959]/80 font-mono text-xs">{downloads.macos.version} · macOS 14+</span>
          </a>
          <a
            id="hero-android-btn"
            href={downloads.android.url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-[#262a33] hover:bg-[#353942] text-[#F8FAFC] text-sm sm:text-base font-medium shadow-md transition-colors duration-200 border border-white/5 group"
          >
            <Smartphone className="w-4 h-4 text-[#94A3B8] group-hover:text-[#abc7ff]" />
            <span>下载 Android App</span>
            <span className="text-[#7bd0ff] font-mono text-xs">{downloads.android.version}</span>
          </a>
        </div>

        {/* Trust Badges */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 w-full max-w-3xl mb-12">
          <div className="flex items-center justify-center gap-2 p-3 rounded-xl bg-[#181c24]/90 border border-white/5 shadow-sm whitespace-nowrap">
            <CloudOff className="w-4 h-4 text-[#10B981] shrink-0" />
            <span className="font-mono text-xs sm:text-sm text-[#dfe2ee] whitespace-nowrap">无自有云端账号</span>
          </div>
          <div className="flex items-center justify-center gap-2 p-3 rounded-xl bg-[#181c24]/90 border border-white/5 shadow-sm whitespace-nowrap">
            <LayoutGrid className="w-4 h-4 text-[#38BDF8] shrink-0" />
            <span className="font-mono text-xs sm:text-sm text-[#dfe2ee] whitespace-nowrap">多个独立菜单栏指标</span>
          </div>
          <div className="flex items-center justify-center gap-2 p-3 rounded-xl bg-[#181c24]/90 border border-white/5 shadow-sm whitespace-nowrap">
            <Lock className="w-4 h-4 text-[#7bd0ff] shrink-0" />
            <span className="font-mono text-xs sm:text-sm text-[#dfe2ee] whitespace-nowrap">凭据仅保留在本机</span>
          </div>
          <div className="flex items-center justify-center gap-2 p-3 rounded-xl bg-[#181c24]/90 border border-white/5 shadow-sm whitespace-nowrap">
            <History className="w-4 h-4 text-[#c0c1ff] shrink-0" />
            <span className="font-mono text-xs sm:text-sm text-[#dfe2ee] whitespace-nowrap">实时/定时智能快照</span>
          </div>
        </div>

        {/* Interactive macOS Menu Bar & Popover Simulator */}
        <MenuBarSimulator
          providers={providers}
          onOpenSettings={onOpenSettings}
          onTriggerNotification={onTriggerNotification}
        />
      </div>
    </section>
  );
};
