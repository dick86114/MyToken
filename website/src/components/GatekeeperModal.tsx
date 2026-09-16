import React, { useState } from 'react';
import { X, ShieldAlert, Terminal, Copy, Check, ExternalLink } from 'lucide-react';

interface GatekeeperModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const GatekeeperModal: React.FC<GatekeeperModalProps> = ({ isOpen, onClose }) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const command = 'sudo xattr -r -d com.apple.quarantine /Applications/MyToken.app';

  const copyCommand = () => {
    navigator.clipboard.writeText(command);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-xl rounded-3xl bg-[#181c24] border border-white/10 shadow-2xl p-6 sm:p-8 flex flex-col gap-6 text-left">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-[#F59E0B]/15 text-[#F59E0B] flex items-center justify-center shrink-0">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-semibold text-lg sm:text-xl text-[#F8FAFC]">
                macOS 首次运行拦截引导 (Gatekeeper)
              </h3>
              <p className="text-xs sm:text-sm text-[#94A3B8]">
                解决“无法打开，因为无法验证开发者”提示
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-[#94A3B8] hover:text-white hover:bg-white/5 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Steps visual */}
        <div className="flex flex-col gap-4 text-sm text-[#c1c6d7]">
          {/* Method 1: System settings */}
          <div className="p-4 rounded-2xl bg-[#1c2028] border border-white/5 flex flex-col gap-2.5">
            <span className="font-semibold text-white text-xs font-mono uppercase tracking-wider text-[#abc7ff]">
              推荐方式：系统设置界面放行
            </span>

            <ol className="list-decimal list-inside space-y-2 text-xs sm:text-sm leading-relaxed text-[#dfe2ee]">
              <li>
                双击打开 <span className="text-[#F8FAFC] font-semibold">MyToken.app</span>，若提示弹窗，点击
                <span className="font-mono text-[#F59E0B]">「好」</span>或<span className="font-mono text-[#F59E0B]">「取消」</span>。
              </li>
              <li>
                打开 Mac 的{' '}
                <span className="font-semibold text-[#F8FAFC]">「系统设置」</span> →{' '}
                <span className="font-semibold text-[#F8FAFC]">「隐私与安全性」</span>。
              </li>
              <li>
                向下滑动至「安全性」区域，会看到：
                <div className="my-1.5 p-2.5 rounded-lg bg-[#0a0e16] border border-white/5 font-mono text-xs text-[#94A3B8] flex items-center justify-between">
                  <span>“MyToken”已被阻止使用，因为来自未知开发者。</span>
                  <span className="px-2.5 py-1 rounded bg-[#262a33] text-[#F8FAFC] text-[11px] font-semibold border border-white/10 shrink-0 ml-2">
                    仍要打开
                  </span>
                </div>
              </li>
              <li>
                点击 <span className="font-semibold text-[#10B981]">「仍要打开」</span>，在二次确认弹窗中输入开机密码或触控 ID 即可永久正常运行。
              </li>
            </ol>
          </div>

          {/* Method 2: Terminal one-liner */}
          <div className="p-4 rounded-2xl bg-[#1c2028] border border-white/5 flex flex-col gap-2.5">
            <span className="font-semibold text-white text-xs font-mono uppercase tracking-wider text-[#7bd0ff] flex items-center gap-1.5">
              <Terminal className="w-3.5 h-3.5" />
              开发者快捷方式：终端一键解除隔离属性
            </span>

            <p className="text-xs text-[#94A3B8]">
              打开 Terminal 终端应用，粘贴并运行以下指令（移除 quarantine 隔离标志）：
            </p>

            <div className="p-3 rounded-xl bg-[#0a0e16] border border-white/5 flex items-center justify-between gap-3 font-mono text-xs">
              <code className="text-[#34D399] break-all">{command}</code>

              <button
                onClick={copyCommand}
                className="px-2.5 py-1.5 rounded-lg bg-[#262a33] hover:bg-[#353942] text-[#F8FAFC] text-xs font-mono flex items-center gap-1 transition-colors cursor-pointer shrink-0"
              >
                {copied ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-[#10B981]" />
                    <span className="text-[#10B981]">已复制</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5 text-[#94A3B8]" />
                    <span>复制</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-end pt-2">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-[#438fff] hover:bg-[#00a6e0] text-[#002959] text-sm font-semibold transition-colors cursor-pointer"
          >
            我知道了
          </button>
        </div>
      </div>
    </div>
  );
};
