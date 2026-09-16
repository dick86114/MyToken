import React from 'react';
import { Lock, ShieldCheck, Heart } from 'lucide-react';

export const Footer: React.FC = () => {
  const scrollTo = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      const offset = 80;
      const bodyRect = document.body.getBoundingClientRect().top;
      const elementRect = element.getBoundingClientRect().top;
      const elementPosition = elementRect - bodyRect;
      const offsetPosition = elementPosition - offset;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth',
      });
    }
  };

  return (
    <footer className="w-full border-t border-white/[0.08] bg-[#0a0e16] py-12 text-[#94A3B8] font-body-small">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-8">
        {/* Brand & Mission */}
        <div className="flex flex-col gap-3 max-w-sm">
          <div className="flex items-center gap-2">
            <img
              alt="MyToken"
              className="h-7 w-7 object-contain"
              src="/app-icon.png"
            />
            <span className="font-semibold text-white text-base">MyToken</span>
          </div>
          <p className="text-xs text-[#64748B] leading-relaxed">
            本地运行的 macOS 菜单栏与 Android 大模型用量监控工具，支持 Routin、DeepSeek、GLM、火山方舟、New API 及 Command Code 凭证管理。
          </p>

          <div className="flex items-center gap-3 text-[11px] font-mono text-[#10B981]">
            <span className="flex items-center gap-1">
              <Lock className="w-3 h-3" /> 本机凭证存储
            </span>
            <span className="flex items-center gap-1">
              <ShieldCheck className="w-3 h-3" /> 0 云端中转
            </span>
          </div>
        </div>

        {/* Navigation columns */}
        <div className="flex flex-wrap gap-8 sm:gap-12 text-xs">
          <div className="flex flex-col gap-2">
            <span className="font-semibold text-white font-mono uppercase tracking-wider text-[11px]">
              产品导航
            </span>
            <button
              onClick={() => scrollTo('features')}
              className="hover:text-white transition-colors text-left cursor-pointer"
            >
              核心功能
            </button>
            <button
              onClick={() => scrollTo('providers')}
              className="hover:text-white transition-colors text-left cursor-pointer"
            >
              供应商矩阵
            </button>
            <button
              onClick={() => scrollTo('privacy')}
              className="hover:text-white transition-colors text-left cursor-pointer"
            >
              安全与隐私
            </button>
            <button
              onClick={() => scrollTo('install')}
              className="hover:text-white transition-colors text-left cursor-pointer"
            >
              安装使用
            </button>
            <button
              onClick={() => scrollTo('faq')}
              className="hover:text-white transition-colors text-left cursor-pointer"
            >
              常见问题
            </button>
          </div>

          <div className="flex flex-col gap-2">
            <span className="font-semibold text-white font-mono uppercase tracking-wider text-[11px]">
              开发与社区
            </span>
            <a
              href="https://github.com/dick86114/MyToken"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              GitHub 仓库
            </a>
            <a
              href="https://github.com/dick86114/MyToken/releases"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              发布版本记录
            </a>
            <a
              href="https://github.com/dick86114/MyToken/issues"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              提交 Issue 反馈
            </a>
          </div>
        </div>
      </div>

      <div className="max-w-[1200px] mx-auto px-4 sm:px-6 mt-8 pt-6 border-t border-white/5 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-[#64748B]">
        <span>© 2026 MyToken. Completed by Dickies.</span>
        <span className="flex items-center gap-1">
          Designed with <Heart className="w-3 h-3 text-[#EF4444] fill-[#EF4444]" /> for developer clarity
        </span>
      </div>
    </footer>
  );
};
