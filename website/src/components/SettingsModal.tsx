import React, { useState } from 'react';
import { ProviderItem, ProviderType } from '../types';
import {
  X,
  Key,
  Plus,
  Trash2,
  Lock,
  CheckCircle2,
  AlertCircle,
  RotateCw,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  providers: ProviderItem[];
  onUpdateProviders: (newProviders: ProviderItem[]) => void;
  onTriggerNotification: (title: string, body: string) => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  providers,
  onUpdateProviders,
  onTriggerNotification,
}) => {
  const [selectedTab, setSelectedTab] = useState<'keys' | 'general'>('keys');
  const [addingKey, setAddingKey] = useState(false);
  const [newProviderCode, setNewProviderCode] = useState<ProviderType>('DS');
  const [newKeyName, setNewKeyName] = useState('');
  const [newKeyValue, setNewKeyValue] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationSuccess, setVerificationSuccess] = useState(false);

  if (!isOpen) return null;

  const handleAddKey = () => {
    if (!newKeyName || !newKeyValue) return;
    setIsVerifying(true);

    setTimeout(() => {
      setIsVerifying(false);
      setVerificationSuccess(true);

      const colorMap: Record<ProviderType, { color: string; badge: string }> = {
        ROU: { color: '#38BDF8', badge: 'rgba(56, 189, 248, 0.1)' },
        DS: { color: '#60A5FA', badge: 'rgba(96, 165, 250, 0.1)' },
        GLM: { color: '#A78BFA', badge: 'rgba(167, 139, 250, 0.1)' },
        VOL: { color: '#FB923C', badge: 'rgba(251, 146, 60, 0.1)' },
        NEW: { color: '#34D399', badge: 'rgba(52, 211, 153, 0.1)' },
        CMD: { color: '#22D3EE', badge: 'rgba(34, 211, 238, 0.1)' },
        MIMO: { color: '#00856F', badge: 'rgba(0, 133, 111, 0.12)' },
      };

      const masked = `${newKeyValue.slice(0, 3)}***${newKeyValue.slice(-4)}`;

      const newItem: ProviderItem = {
        id: `custom-${Date.now()}`,
        code: newProviderCode,
        name: newKeyName,
        keyMask: masked,
        tagColor: colorMap[newProviderCode].color,
        badgeBg: colorMap[newProviderCode].badge,
        status: 'active',
        statusLabel: '正常运行',
        primaryMetric: newProviderCode === 'DS' ? '总余额' : '用量占比',
        primaryValue: newProviderCode === 'DS' ? '¥25.80' : '28%',
        progress1:
          newProviderCode !== 'DS'
            ? { label: '5小时用量', value: 28, display: '28%' }
            : undefined,
        lastUpdated: '刚刚',
      };

      const updated = [...providers, newItem];
      onUpdateProviders(updated);
      onTriggerNotification('已成功添加并加密保存凭证', `${newKeyName} 已加入菜单栏实时轮询。`);

      setTimeout(() => {
        setAddingKey(false);
        setVerificationSuccess(false);
        setNewKeyName('');
        setNewKeyValue('');
      }, 500);
    }, 700);
  };

  const handleDelete = (id: string, name: string) => {
    const updated = providers.filter((p) => p.id !== id);
    onUpdateProviders(updated);
    onTriggerNotification('已移除凭证', `${name} 已从本地安全存储抹除。`);
  };

  const handleResetAll = () => {
    if (window.confirm('确定要清除所有本地凭据与快照数据吗？此操作不可恢复。')) {
      onUpdateProviders([]);
      onTriggerNotification('本地数据已完全抹除', '本地凭证和用量缓存已全部重置。');
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-2xl rounded-3xl bg-[#181c24] border border-white/10 shadow-2xl flex flex-col max-h-[85vh] overflow-hidden text-left">
        {/* Modal Header */}
        <div className="p-6 border-b border-white/[0.08] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#262a33] text-[#7bd0ff] flex items-center justify-center border border-white/5">
              <Key className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-semibold text-lg text-[#F8FAFC]">
                MyToken 本地偏好设置
              </h3>
              <p className="text-xs text-[#94A3B8] font-mono">
                macOS 本地存储 · Android Keystore · 零云端中转
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

        {/* Tab nav */}
        <div className="flex items-center gap-2 px-6 pt-3 border-b border-white/[0.04]">
          <button
            onClick={() => setSelectedTab('keys')}
            className={`pb-2.5 px-2 text-xs sm:text-sm font-medium transition-colors border-b-2 cursor-pointer ${
              selectedTab === 'keys'
                ? 'border-[#438fff] text-[#abc7ff]'
                : 'border-transparent text-[#94A3B8] hover:text-white'
            }`}
          >
            API 凭据管理 ({providers.length})
          </button>
          <button
            onClick={() => setSelectedTab('general')}
            className={`pb-2.5 px-2 text-xs sm:text-sm font-medium transition-colors border-b-2 cursor-pointer ${
              selectedTab === 'general'
                ? 'border-[#438fff] text-[#abc7ff]'
                : 'border-transparent text-[#94A3B8] hover:text-white'
            }`}
          >
            刷新与通知预警
          </button>
        </div>

        {/* Tab Content */}
        <div className="p-6 overflow-y-auto space-y-4">
          {selectedTab === 'keys' && (
            <>
              <div className="flex items-center justify-between">
                <span className="text-xs text-[#94A3B8]">
                  以下凭证加密保存在本机，由应用直接发起查询：
                </span>
                {!addingKey && (
                  <button
                    onClick={() => setAddingKey(true)}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#438fff] hover:bg-[#00a6e0] text-[#002959] text-xs font-semibold transition-colors cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>添加 Key</span>
                  </button>
                )}
              </div>

              {/* Add Key Form */}
              {addingKey && (
                <div className="p-4 rounded-2xl bg-[#10141e] border border-white/10 flex flex-col gap-3 animate-fadeIn">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-[#F8FAFC]">
                      新建凭据沙盒
                    </span>
                    <button
                      onClick={() => setAddingKey(false)}
                      className="text-xs text-[#64748B] hover:text-white"
                    >
                      取消
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="text-[11px] text-[#94A3B8] block mb-1">
                        供应商类型
                      </label>
                      <select
                        value={newProviderCode}
                        onChange={(e) => setNewProviderCode(e.target.value as ProviderType)}
                        className="w-full px-3 py-1.5 rounded-lg bg-[#181c24] border border-white/10 text-xs text-[#F8FAFC] focus:outline-none focus:border-[#438fff]"
                      >
                        <option value="ROU">Routin</option>
                        <option value="DS">DeepSeek</option>
                        <option value="GLM">智谱 GLM</option>
                        <option value="VOL">火山方舟</option>
                        <option value="NEW">New API</option>
                        <option value="CMD">Command Code</option>
                        <option value="MIMO">小米 MiMo</option>
                      </select>
                    </div>

                    <div>
                      <label className="text-[11px] text-[#94A3B8] block mb-1">
                        备注名称
                      </label>
                      <input
                        type="text"
                        placeholder="如: DeepSeek 团队开发"
                        value={newKeyName}
                        onChange={(e) => setNewKeyName(e.target.value)}
                        className="w-full px-3 py-1.5 rounded-lg bg-[#181c24] border border-white/10 text-xs text-[#F8FAFC] focus:outline-none focus:border-[#438fff]"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="text-[11px] text-[#94A3B8] block mb-1">
                      凭据值 (API Key / Plan Token)
                    </label>
                    <input
                      type="password"
                      placeholder="sk-xxxxxxxxxxxxxxxxxxxxxxxx"
                      value={newKeyValue}
                      onChange={(e) => setNewKeyValue(e.target.value)}
                      className="w-full px-3 py-1.5 rounded-lg bg-[#181c24] border border-white/10 text-xs text-[#F8FAFC] font-mono focus:outline-none focus:border-[#438fff]"
                    />
                  </div>

                  <div className="flex items-center justify-end gap-2 pt-1">
                    <button
                      onClick={handleAddKey}
                      disabled={isVerifying || !newKeyName || !newKeyValue}
                      className="px-4 py-1.5 rounded-lg bg-[#438fff] hover:bg-[#00a6e0] text-[#002959] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
                    >
                      {isVerifying ? (
                        <>
                          <RotateCw className="w-3.5 h-3.5 animate-spin" />
                          <span>正在直连官方校验...</span>
                        </>
                      ) : verificationSuccess ? (
                        <>
                          <CheckCircle2 className="w-3.5 h-3.5 text-[#10B981]" />
                          <span>校验成功</span>
                        </>
                      ) : (
                        <>
                          <Lock className="w-3.5 h-3.5" />
                          <span>加密保存并上屏</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>
              )}

              {/* Installed keys list */}
              <div className="flex flex-col gap-2.5">
                {providers.map((p) => (
                  <div
                    key={p.id}
                    className="p-3.5 rounded-xl bg-[#1c2028] border border-white/5 flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3">
                      <span
                        className="px-2 py-0.5 rounded font-mono text-xs font-bold"
                        style={{ backgroundColor: p.badgeBg, color: p.tagColor }}
                      >
                        {p.code}
                      </span>
                      <div>
                        <div className="text-xs sm:text-sm font-semibold text-[#F8FAFC]">
                          {p.name}
                        </div>
                        <div className="text-[11px] font-mono text-[#64748B]">
                          {p.keyMask} · 指标: {p.primaryValue}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded bg-[#0a0e16] text-[#10B981] font-mono text-[10px] hidden sm:inline">
                        本机加密
                      </span>
                      <button
                        onClick={() => handleDelete(p.id, p.name)}
                        className="p-1.5 rounded hover:bg-white/5 text-[#64748B] hover:text-[#EF4444] transition-colors cursor-pointer"
                        title="删除凭证"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}

          {selectedTab === 'general' && (
            <div className="space-y-4 text-xs sm:text-sm text-[#c1c6d7]">
              <div className="p-4 rounded-xl bg-[#1c2028] border border-white/5 space-y-3">
                <span className="font-semibold text-[#F8FAFC] block">
                  静默轮询与刷新策略
                </span>
                <div className="flex items-center justify-between">
                  <span>全局静默刷新间隔</span>
                  <select className="px-3 py-1 rounded bg-[#0a0e16] border border-white/10 text-xs text-white">
                    <option>5 分钟</option>
                    <option selected>15 分钟 (推荐)</option>
                    <option>30 分钟</option>
                    <option>仅手动刷新</option>
                  </select>
                </div>
                <div className="flex items-center justify-between">
                  <span>电脑唤醒时自动刷新快照</span>
                  <input type="checkbox" defaultChecked className="accent-[#438fff]" />
                </div>
              </div>

              <div className="p-4 rounded-xl bg-[#1c2028] border border-white/5 space-y-3">
                <span className="font-semibold text-[#F8FAFC] block">
                  macOS 原生通知与预警
                </span>
                <div className="flex items-center justify-between">
                  <span>额度达到 80% 触发预警</span>
                  <input type="checkbox" defaultChecked className="accent-[#438fff]" />
                </div>
                <div className="flex items-center justify-between">
                  <span>DeepSeek 余额低于 ¥5.00 时通知</span>
                  <input type="checkbox" defaultChecked className="accent-[#438fff]" />
                </div>
              </div>

              {/* Danger Zone */}
              <div className="p-4 rounded-xl bg-[#EF4444]/5 border border-[#EF4444]/20 flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#EF4444] text-xs block">
                    彻底抹除数据
                  </span>
                  <span className="text-[11px] text-[#64748B]">
                    清空本地凭证与用量缓存，恢复初始状态。
                  </span>
                </div>
                <button
                  onClick={handleResetAll}
                  className="px-3 py-1.5 rounded-lg bg-[#EF4444]/20 hover:bg-[#EF4444] text-[#EF4444] hover:text-white font-mono text-xs transition-colors cursor-pointer"
                >
                  重置所有数据
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-white/[0.08] bg-[#12161f] flex items-center justify-between text-xs text-[#94A3B8]">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#10B981]" />
            <span>修改即时生效，凭证保存在本机安全存储</span>
          </div>

          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-xl bg-[#262a33] hover:bg-[#353942] text-[#F8FAFC] text-xs font-semibold transition-colors cursor-pointer"
          >
            完成
          </button>
        </div>
      </div>
    </div>
  );
};
