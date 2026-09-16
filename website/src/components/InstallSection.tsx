import React from 'react';
import {
  Download,
  Smartphone,
  FolderDown,
  Sliders,
  ShieldAlert,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';
import { useLatestDownloads } from '../hooks/useLatestDownloads';

interface InstallSectionProps {
  onOpenGatekeeperModal: () => void;
}

export const InstallSection: React.FC<InstallSectionProps> = ({
  onOpenGatekeeperModal,
}) => {
  const downloads = useLatestDownloads();
  return (
    <section id="install" className="w-full py-16 lg:py-24 bg-[#0a0e16]/80 relative">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="flex flex-col mb-12 max-w-2xl">
          <span className="font-mono text-xs text-[#abc7ff] uppercase tracking-widest mb-2 font-semibold">
            Installation
          </span>
          <h2 className="font-headline-section text-[#F8FAFC] tracking-tight">
            macOS 与 Android 安装使用
          </h2>
          <p className="font-body-large text-[#c1c6d7] mt-3">
            macOS 三步完成菜单栏常驻；Android 下载 APK 后即可手动添加凭证，或从 Mac 扫码加密迁移。
          </p>
        </div>

        {/* 3 Step Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Step 1 */}
          <div
            id="install-step-1"
            className="p-6 rounded-2xl bg-[#181c24]/90 border border-white/[0.06] shadow-md flex flex-col justify-between hover:bg-[#1c2028] transition-all"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="px-2.5 py-1 rounded-md bg-[#438fff] text-[#002959] font-mono text-xs font-bold">
                  STEP 01
                </span>
                <Download className="w-5 h-5 text-[#64748B]" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">下载 DMG 安装包</h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                下载按钮会自动读取 GitHub 最新 macOS Release，并按设备架构选择安装镜像。
              </p>
            </div>
            <div className="p-3 rounded-xl bg-[#0a0e16] text-[#94A3B8] font-mono text-xs flex items-center justify-between border border-white/5">
              <span>按设备架构自动选择</span>
              <span className="text-[#10B981] font-semibold">Apple Silicon / Intel</span>
            </div>
          </div>

          {/* Step 2 */}
          <div
            id="install-step-2"
            className="p-6 rounded-2xl bg-[#181c24]/90 border border-white/[0.06] shadow-md flex flex-col justify-between hover:bg-[#1c2028] transition-all"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="px-2.5 py-1 rounded-md bg-[#438fff] text-[#002959] font-mono text-xs font-bold">
                  STEP 02
                </span>
                <FolderDown className="w-5 h-5 text-[#64748B]" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">拖拽至 Applications</h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                双击打开 DMG 文件，将 MyToken 图标顺畅拖动至右侧的“应用程序”快捷文件夹内。
              </p>
            </div>
            <div className="p-3 rounded-xl bg-[#0a0e16] text-[#94A3B8] font-mono text-xs flex items-center justify-between border border-white/5">
              <span>/Applications</span>
              <span className="text-[#abc7ff] font-semibold">无需额外依赖</span>
            </div>
          </div>

          {/* Step 3 */}
          <div
            id="install-step-3"
            className="p-6 rounded-2xl bg-[#181c24]/90 border border-white/[0.06] shadow-md flex flex-col justify-between hover:bg-[#1c2028] transition-all"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="px-2.5 py-1 rounded-md bg-[#438fff] text-[#002959] font-mono text-xs font-bold">
                  STEP 03
                </span>
                <Sliders className="w-5 h-5 text-[#64748B]" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">配置凭据即刻使用</h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                点击顶部菜单栏图标打开设置，选择对应供应商填写 Key、Cookie 或 serviceToken，并点击“验证并保存”，实时指标立即点亮。
              </p>
            </div>
            <div className="p-3 rounded-xl bg-[#0a0e16] text-[#94A3B8] font-mono text-xs flex items-center justify-between border border-white/5">
              <span>验证后刷新</span>
              <span className="text-[#10B981] font-semibold">凭证本地保存</span>
            </div>
          </div>
        </div>

        <div className="mb-6 p-6 sm:p-8 rounded-2xl bg-[#181c24]/90 border border-white/[0.08] flex flex-col md:flex-row items-start md:items-center gap-6 shadow-xl">
          <div className="w-12 h-12 rounded-xl bg-[#22c55e]/15 text-[#22c55e] flex items-center justify-center shrink-0">
            <Smartphone className="w-7 h-7" />
          </div>
          <div className="flex flex-col gap-1.5 grow">
            <h4 className="font-semibold text-lg text-[#F8FAFC]">Android 10+ 伴侣应用</h4>
            <p className="text-sm text-[#c1c6d7] leading-relaxed">
              支持首页用量卡片、凭证详情、桌面小组件、主题切换和用量提醒。也可以从 macOS 端发起一次性二维码迁移，凭证通过局域网端到端加密传输，Android 本地再使用 Keystore 加密保存。
            </p>
          </div>
          <a
            href={downloads.android.url}
            target="_blank"
            rel="noopener noreferrer"
            className="shrink-0 px-4 py-2.5 rounded-xl bg-[#262a33] hover:bg-[#353942] text-[#F8FAFC] text-sm font-medium transition-colors border border-white/5 cursor-pointer flex items-center gap-1.5 shadow-sm"
          >
            <Download className="w-4 h-4" />
            <span>下载 APK</span>
            <span className="text-[#7bd0ff] font-mono text-xs">{downloads.android.version}</span>
          </a>
        </div>
        {/* macOS Gatekeeper Guide Banner */}
        <div
          id="gatekeeper-banner"
          className="p-6 sm:p-8 rounded-2xl bg-[#181c24]/90 border border-white/[0.08] flex flex-col md:flex-row items-start md:items-center gap-6 shadow-xl"
        >
          <div className="w-12 h-12 rounded-xl bg-[#F59E0B]/15 text-[#F59E0B] flex items-center justify-center shrink-0">
            <ShieldAlert className="w-7 h-7" />
          </div>
          <div className="flex flex-col gap-1.5 grow">
            <h4 className="font-semibold text-lg text-[#F8FAFC]">
              macOS 首次运行拦截提示（Gatekeeper 引导）
            </h4>
            <p className="text-sm text-[#c1c6d7] leading-relaxed">
              若因未购买昂贵的苹果官方开发者证书导致系统提示“无法验证开发者”或“已被阻止”，请前往：
              <br className="hidden sm:inline" />
              <span className="text-[#abc7ff] font-medium">
                系统设置 → 隐私与安全性 → 安全性区域
              </span>
              ，找到拦截记录并点击{' '}
              <span className="font-semibold text-[#F8FAFC]">「仍要打开」</span> 即可永久正常运行。
            </p>
          </div>
          <button
            id="btn-open-gatekeeper-guide"
            onClick={onOpenGatekeeperModal}
            className="shrink-0 px-4 py-2.5 rounded-xl bg-[#262a33] hover:bg-[#353942] text-[#F8FAFC] text-sm font-medium transition-colors border border-white/5 cursor-pointer flex items-center gap-1.5 shadow-sm"
          >
            <span>查看图文教程</span>
            <ChevronRight className="w-4 h-4 text-[#94A3B8]" />
          </button>
        </div>
      </div>
    </section>
  );
};
