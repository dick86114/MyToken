import React from 'react';
import { Download, Smartphone } from 'lucide-react';
import { useLatestDownloads } from '../hooks/useLatestDownloads';

interface FinalCtaSectionProps {
}

export const FinalCtaSection: React.FC<FinalCtaSectionProps> = () => {
  const downloads = useLatestDownloads();

  return (
    <section className="w-full py-16 lg:py-24 relative">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6">
        <div className="relative overflow-hidden rounded-3xl bg-[#181c24]/90 border border-white/[0.08] p-8 sm:p-14 lg:p-16 shadow-[0_20px_50px_rgba(0,0,0,0.6)] flex flex-col items-center text-center">
          {/* Ambient subtle glow inside card */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-96 bg-[#438fff]/15 blur-3xl -z-10 rounded-full pointer-events-none" />

          {/* App Icon */}
          <div className="w-20 h-20 sm:w-24 sm:h-24 mb-6 relative flex items-center justify-center">
            <img
              alt="MyToken Icon"
              className="w-full h-full object-contain rounded-2xl shadow-2xl transition-transform hover:scale-105"
              src="/app-banner.png"
            />
          </div>

          <h2 className="font-headline-section text-[#F8FAFC] tracking-tight max-w-2xl mb-3">
            告别逐个登录控制台，在菜单栏掌控所有大模型资产
          </h2>
          <p className="font-body-large text-[#c1c6d7] max-w-xl mb-8 leading-relaxed">
            专为重度 AI 开发者打造，本地优先、无自有云端中转。选择 macOS 或 Android 版本立即开始。
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-wrap items-center justify-center gap-4">
            <a
              id="cta-download-btn"
              href={downloads.macos.url}
              target="_blank"
              rel="noopener noreferrer"
              className="group inline-flex items-center gap-2.5 px-7 py-3.5 rounded-xl bg-[#438fff] hover:bg-[#00a6e0] text-[#002959] text-base font-semibold shadow-[0_0_30px_0_rgba(67,143,255,0.4)] transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
            >
              <Download className="w-5 h-5 transition-transform group-hover:-translate-y-0.5" />
              <span>立即下载 MyToken.dmg</span>
              <span className="font-mono text-xs">{downloads.macos.version}</span>
            </a>

            <a
              id="cta-android-btn"
              href={downloads.android.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-6 py-3.5 rounded-xl bg-[#262a33] hover:bg-[#353942] text-[#F8FAFC] text-base font-medium shadow-md transition-colors duration-200 border border-white/5 cursor-pointer"
            >
              <Smartphone className="w-5 h-5 text-[#7bd0ff]" />
              <span>下载 Android App</span>
              <span className="text-[#7bd0ff] font-mono text-xs">{downloads.android.version}</span>
            </a>
          </div>

          {/* Platform tags */}
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3 text-[#94A3B8] font-mono text-xs">
            <span>支持 macOS 14.0+ 与 Android 10+</span>
            <span>•</span>
            <span>Apple Silicon / Intel macOS 安装包</span>
            <span>•</span>
            <span className="text-[#10B981]">凭证本机保存</span>
          </div>
        </div>
      </div>
    </section>
  );
};
