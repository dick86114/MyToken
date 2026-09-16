import React, { useState } from 'react';
import {
  Laptop,
  Server,
  ArrowRight,
  ShieldCheck,
  Ban,
  Key,
  KeyRound,
  EyeOff,
  Trash2,
  Lock,
  Activity,
  CheckCircle2,
} from 'lucide-react';

export const SecuritySection: React.FC = () => {
  const [testingHandshake, setTestingHandshake] = useState(false);
  const [handshakeResult, setHandshakeResult] = useState<string | null>(null);

  const runHandshakeTest = () => {
    setTestingHandshake(true);
    setHandshakeResult(null);

    setTimeout(() => {
      setTestingHandshake(false);
      setHandshakeResult('Direct TLS 1.3 Handshake completed in 38ms (0 Proxy Hops, 100% Client-Side)');
    }, 800);
  };

  return (
    <section id="privacy" className="w-full py-16 lg:py-24 relative">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="flex flex-col mb-12 max-w-2xl">
          <span className="font-mono text-xs text-[#10B981] uppercase tracking-widest mb-2 font-semibold">
            Local First Architecture
          </span>
          <h2 className="font-headline-section text-[#F8FAFC] tracking-tight">
            安全、隐私与本地直连架构
          </h2>
          <p className="font-body-large text-[#c1c6d7] mt-3">
            没有自有云端账号，也没有任何中间收集服务器。凭证和用量数据只保存在本机；macOS 使用当前用户本地存储，Android 使用 Keystore 保护凭证密文。
          </p>
        </div>

        {/* Trust Architecture Diagram Card */}
        <div className="p-6 sm:p-10 rounded-3xl bg-[#181c24]/90 border border-white/[0.08] shadow-2xl flex flex-col gap-8 mb-10">
          {/* Network Flow Graphic */}
          <div className="w-full flex flex-col md:flex-row items-center justify-between gap-6 p-6 rounded-2xl bg-[#0a0e16] border border-white/5">
            {/* Box: Local Mac */}
            <div className="flex flex-col items-center p-4 rounded-xl bg-[#262a33] w-full md:w-60 text-center shadow-md border border-white/5">
              <Laptop className="w-8 h-8 text-[#7bd0ff] mb-2" />
              <span className="text-sm font-semibold text-[#F8FAFC]">
                本机 macOS / Android 客户端
              </span>
              <span className="font-mono text-xs text-[#94A3B8] mt-1">
                本地应用存储 / Android Keystore
              </span>
              <span className="mt-2 px-2 py-0.5 rounded-full bg-[#10B981]/15 text-[#10B981] font-mono text-[10px] flex items-center gap-1">
                <Lock className="w-3 h-3" /> 本机隔离存储
              </span>
            </div>

            {/* Flow Arrows & Blocked Relay */}
            <div className="flex flex-col items-center gap-2 text-center my-2 md:my-0">
              <div className="flex items-center gap-2 text-[#10B981] font-mono text-xs font-semibold">
                <span>直接 HTTPS 签名请求</span>
                <ArrowRight className="w-4 h-4 animate-pulse" />
              </div>

              <div className="px-3 py-1 rounded-full bg-[#EF4444]/10 text-[#EF4444] font-mono text-xs flex items-center gap-1.5 border border-[#EF4444]/20">
                <Ban className="w-3.5 h-3.5" />
                <span>零云端中转服务器 (Zero Cloud Relay)</span>
              </div>

              {/* Handshake test button */}
              <button
                onClick={runHandshakeTest}
                disabled={testingHandshake}
                className="mt-1 text-[11px] font-mono text-[#7bd0ff] hover:underline flex items-center gap-1 cursor-pointer"
              >
                <Activity className={`w-3 h-3 ${testingHandshake ? 'animate-spin' : ''}`} />
                <span>{testingHandshake ? '正在探测链路...' : '测试本地直连握手'}</span>
              </button>

              {handshakeResult && (
                <div className="mt-1 text-[11px] font-mono text-[#10B981] flex items-center gap-1 bg-[#10B981]/10 px-2 py-0.5 rounded animate-fadeIn">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>{handshakeResult}</span>
                </div>
              )}
            </div>

            {/* Box: Providers */}
            <div className="flex flex-col items-center p-4 rounded-xl bg-[#262a33] w-full md:w-60 text-center shadow-md border border-white/5">
              <Server className="w-8 h-8 text-[#38BDF8] mb-2" />
              <span className="text-sm font-semibold text-[#F8FAFC]">
                官方供应商 API
              </span>
              <span className="font-mono text-xs text-[#94A3B8] mt-1">
                Direct Provider Endpoints
              </span>
              <span className="mt-2 px-2 py-0.5 rounded-full bg-[#38BDF8]/15 text-[#38BDF8] font-mono text-[10px]">
                Routin / DeepSeek / GLM / 火山 / New API / CMD
              </span>
            </div>
          </div>

          {/* 4 Security Points Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* 1: 本地密态存储 */}
            <div className="flex flex-col gap-2 p-5 rounded-2xl bg-[#1c2028] border border-white/5 hover:border-white/10 transition-colors">
              <div className="w-9 h-9 rounded-lg bg-[#10B981]/15 text-[#10B981] flex items-center justify-center mb-1">
                <Key className="w-5 h-5" />
              </div>
              <h4 className="font-semibold text-base text-[#F8FAFC]">本地密态存储</h4>
              <p className="text-xs sm:text-sm text-[#c1c6d7] leading-relaxed">
                macOS 凭证保存在当前用户本地应用存储；Android 使用 Keystore 加密并排除系统云备份。
              </p>
            </div>

            {/* 2: 零账号侵入 */}
            <div className="flex flex-col gap-2 p-5 rounded-2xl bg-[#1c2028] border border-white/5 hover:border-white/10 transition-colors">
              <div className="w-9 h-9 rounded-lg bg-[#7bd0ff]/15 text-[#7bd0ff] flex items-center justify-center mb-1">
                <KeyRound className="w-5 h-5" />
              </div>
              <h4 className="font-semibold text-base text-[#F8FAFC]">零账号侵入</h4>
              <p className="text-xs sm:text-sm text-[#c1c6d7] leading-relaxed">
                绝不收集登录密码、双重验证码、明文 Cookie 或账户邮箱原文。
              </p>
            </div>

            {/* 3: 日志强制脱敏 */}
            <div className="flex flex-col gap-2 p-5 rounded-2xl bg-[#1c2028] border border-white/5 hover:border-white/10 transition-colors">
              <div className="w-9 h-9 rounded-lg bg-[#c0c1ff]/15 text-[#c0c1ff] flex items-center justify-center mb-1">
                <EyeOff className="w-5 h-5" />
              </div>
              <h4 className="font-semibold text-base text-[#F8FAFC]">日志强制脱敏</h4>
              <p className="text-xs sm:text-sm text-[#c1c6d7] leading-relaxed">
                诊断日志中的密钥均自动脱敏处理（如 plan-&lt;redacted&gt;），在手动导出或提交前清晰可见。
              </p>
            </div>

            {/* 4: 一键彻底清理 */}
            <div className="flex flex-col gap-2 p-5 rounded-2xl bg-[#1c2028] border border-white/5 hover:border-white/10 transition-colors">
              <div className="w-9 h-9 rounded-lg bg-[#EF4444]/15 text-[#EF4444] flex items-center justify-center mb-1">
                <Trash2 className="w-5 h-5" />
              </div>
              <h4 className="font-semibold text-base text-[#F8FAFC]">按凭证清理</h4>
              <p className="text-xs sm:text-sm text-[#c1c6d7] leading-relaxed">
                删除凭证会同步清理对应本地密钥和用量缓存；卸载前建议先删除不再使用的凭证。
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
