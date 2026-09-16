import React, { useState } from 'react';
import {
  Monitor,
  KeyRound,
  RotateCw,
  BellRing,
  AlertTriangle,
  CheckCircle2,
  Lock,
} from 'lucide-react';

interface CapabilitiesSectionProps {
  onTriggerNotification: (title: string, body: string) => void;
}

export const CapabilitiesSection: React.FC<CapabilitiesSectionProps> = ({
  onTriggerNotification,
}) => {
  const [activeInterval, setActiveInterval] = useState('15 min 定时');
  const [selectedKey, setSelectedKey] = useState<'prod' | 'test'>('prod');

  return (
    <section id="features" className="w-full py-16 lg:py-24 relative">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="flex flex-col mb-12 max-w-2xl">
          <span className="font-mono text-xs text-[#abc7ff] uppercase tracking-widest mb-2 font-semibold">
            Native Capabilities
          </span>
          <h2 className="font-headline-section text-[#F8FAFC] tracking-tight">
            为严肃工程而生的核心能力
          </h2>
          <p className="font-body-large text-[#c1c6d7] mt-3">
            摒弃虚浮假数据与复杂云同步，将轻巧、即时、透明的 macOS 原生用量掌控感带给每一位开发者。
          </p>
        </div>

        {/* 4 Bento Grid Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Card 1: 多个独立菜单栏指标 */}
          <div
            id="feature-card-1"
            className="p-6 sm:p-8 rounded-2xl bg-[#181c24]/90 hover:bg-[#1c2028] transition-all duration-300 border border-white/[0.06] shadow-md flex flex-col justify-between group"
          >
            <div>
              <div className="w-12 h-12 rounded-xl bg-[#38BDF8]/10 text-[#38BDF8] flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Monitor className="w-6 h-6" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">
                多个独立菜单栏指标
              </h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                内置短码短标识：<span className="text-[#38BDF8] font-mono font-medium">ROU</span>、
                <span className="text-[#60A5FA] font-mono font-medium">DS</span>、
                <span className="text-[#A78BFA] font-mono font-medium">GLM</span>、
                <span className="text-[#FB923C] font-mono font-medium">VOL</span>、
                <span className="text-[#34D399] font-mono font-medium">NEW</span>、<span className="text-[#22D3EE] font-mono font-medium">CMD</span>、<span className="text-[#00856F] font-mono font-medium">MIMO</span>。进度条真实映射各供应商的有上限额度，余额健康状态精确显示，绝不伪造百分比。
              </p>
            </div>

            {/* Micro Visual Asset */}
            <div className="p-3 rounded-xl bg-[#0a0e16] border border-white/5 flex items-center justify-around gap-2 flex-wrap">
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#38BDF8] font-semibold border border-white/5">
                ROU 42%
              </span>
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#10B981] font-semibold border border-white/5">
                DS ¥12.3
              </span>
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#A78BFA] font-semibold border border-white/5">
                GLM 35%
              </span>
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#FB923C] font-semibold border border-white/5">
                VOL 18%
              </span>
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#34D399] font-semibold border border-white/5">
                NEW $86.2
              </span>
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#22D3EE] font-semibold border border-white/5">
                CMD $42.8
              </span>
              <span className="px-2.5 py-1 rounded bg-[#1c2028] font-mono text-xs text-[#00856F] font-semibold border border-white/5">
                MIMO ¥58.78
              </span>
            </div>
          </div>

          {/* Card 2: 多供应商与凭证隔离 */}
          <div
            id="feature-card-2"
            className="p-6 sm:p-8 rounded-2xl bg-[#181c24]/90 hover:bg-[#1c2028] transition-all duration-300 border border-white/[0.06] shadow-md flex flex-col justify-between group"
          >
            <div>
              <div className="w-12 h-12 rounded-xl bg-[#438fff]/20 text-[#abc7ff] flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <KeyRound className="w-6 h-6" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">
                多供应商与凭证隔离
              </h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                支持在同一供应商下添加多套独立 Key（例如主账户、测试 Key、临时协作密钥），按供应商分类清晰管理，数据互不合并覆盖，本机维护独立加密缓存沙盒。
              </p>
            </div>

            {/* Micro Visual Asset */}
            <div className="p-3 rounded-xl bg-[#0a0e16] border border-white/5 flex flex-col gap-2">
              <div
                onClick={() => setSelectedKey('prod')}
                className={`flex items-center justify-between font-mono text-xs p-1.5 rounded cursor-pointer transition-colors ${
                  selectedKey === 'prod' ? 'bg-[#1c2028] border border-white/10' : ''
                }`}
              >
                <span className="text-[#94A3B8]">Prod Key:</span>
                <span className="text-[#F8FAFC]">sk-prod-9a*** (可用)</span>
                <span className="text-[#10B981] flex items-center gap-1 font-medium">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#10B981]" /> 独立
                </span>
              </div>
              <div
                onClick={() => setSelectedKey('test')}
                className={`flex items-center justify-between font-mono text-xs p-1.5 rounded cursor-pointer transition-colors ${
                  selectedKey === 'test' ? 'bg-[#1c2028] border border-white/10' : ''
                }`}
              >
                <span className="text-[#94A3B8]">Test Key:</span>
                <span className="text-[#F8FAFC]">sk-test-4b*** (隔离)</span>
                <span className="text-[#10B981] flex items-center gap-1 font-medium">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#10B981]" /> 独立
                </span>
              </div>
            </div>
          </div>

          {/* Card 3: 智能刷新与缓存健康 */}
          <div
            id="feature-card-3"
            className="p-6 sm:p-8 rounded-2xl bg-[#181c24]/90 hover:bg-[#1c2028] transition-all duration-300 border border-white/[0.06] shadow-md flex flex-col justify-between group"
          >
            <div>
              <div className="w-12 h-12 rounded-xl bg-[#7bd0ff]/10 text-[#7bd0ff] flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <RotateCw className="w-6 h-6" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">
                智能刷新与缓存健康
              </h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                灵活设定 1/5/15/30 分钟全局静默刷新。当遇到网络抖动、被限流或供应商短暂不可用时，界面清晰标记“缓存已过期”或最近一次成功快照，绝无假数据骚扰。
              </p>
            </div>

            {/* Micro Visual Asset */}
            <div className="p-3 rounded-xl bg-[#0a0e16] border border-white/5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#10B981]" />
                <span className="font-mono text-xs text-[#94A3B8]">
                  快照健康度: <span className="text-[#F8FAFC]">100% 真实返回</span>
                </span>
              </div>

              {/* Toggleable interval pill */}
              <div className="flex items-center gap-1">
                {['1m', '5m', '15m', '30m'].map((it) => (
                  <button
                    key={it}
                    onClick={() => setActiveInterval(`${it} 定时`)}
                    className={`px-2 py-0.5 rounded font-mono text-[11px] transition-colors cursor-pointer ${
                      activeInterval.startsWith(it)
                        ? 'bg-[#7bd0ff]/20 text-[#7bd0ff] font-semibold border border-[#7bd0ff]/30'
                        : 'text-[#64748B] hover:text-white'
                    }`}
                  >
                    {it}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Card 4: 原生通知与用量预警 */}
          <div
            id="feature-card-4"
            className="p-6 sm:p-8 rounded-2xl bg-[#181c24]/90 hover:bg-[#1c2028] transition-all duration-300 border border-white/[0.06] shadow-md flex flex-col justify-between group"
          >
            <div>
              <div className="w-12 h-12 rounded-xl bg-[#F59E0B]/10 text-[#F59E0B] flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <BellRing className="w-6 h-6" />
              </div>
              <h3 className="font-title-card text-[#F8FAFC] mb-2">
                原生通知与用量预警
              </h3>
              <p className="font-body-base text-[#c1c6d7] mb-6 leading-relaxed">
                额度消耗达到 80%、95% 或低于自定义余额阈值时，自动触发 macOS 系统级通知横幅与原生声音提醒。发出的系统通知内容绝不包含任何敏感密钥材料。
              </p>
            </div>

            {/* Micro Visual Asset */}
            <div
              onClick={() =>
                onTriggerNotification(
                  'DeepSeek 余额已低于预警阈值 ¥5.00',
                  '系统通知 · 敏感 Key 已自动隐匿，请尽快充值以免 API 阻断。'
                )
              }
              title="点击测试触发系统通知"
              className="p-3 rounded-xl bg-[#0a0e16] border border-white/5 flex items-center gap-3 cursor-pointer hover:border-[#F59E0B]/40 transition-colors"
            >
              <AlertTriangle className="w-5 h-5 text-[#F59E0B] shrink-0" />
              <div className="flex flex-col">
                <span className="text-xs font-medium text-[#F8FAFC]">
                  DeepSeek 余额已低于预警阈值 ¥5.00
                </span>
                <span className="font-mono text-[11px] text-[#64748B]">
                  系统通知 · 敏感 Key 已自动隐匿 (点击模拟)
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
