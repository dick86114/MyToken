import React, { useState, useEffect } from 'react';
import { useLatestDownloads } from '../hooks/useLatestDownloads';
import { ProviderItem } from '../types';
import {
  RotateCw,
  Clock,
  Settings,
  Power,
  ExternalLink,
  Search,
  Check,
  Zap,
  Key,
  Flame,
  ChevronDown,
  Info,
  SlidersHorizontal,
} from 'lucide-react';

interface MenuBarSimulatorProps {
  providers: ProviderItem[];
  onOpenSettings: () => void;
  onTriggerNotification: (title: string, body: string) => void;
}

interface RealAccountCard {
  id: string;
  avatarChar: string;
  name: string;
  plan: string;
  providerCode: 'GLM' | 'ROU' | 'DS' | 'VOL' | 'NEW' | 'CMD' | 'MIMO';
  overallStatus: string;
  overallStatusColor: string;
  startDate?: string;
  endDate?: string;
  cardTheme: {
    border: string;
    bg: string;
    glow: string;
  };
  metricsType: 'dual_progress' | 'triple_progress' | 'balance_grid' | 'stat_grid' | 'xiaomi_grid';
  // Dual / Triple progress metrics
  metric1?: {
    title: string;
    percent: string;
    barPercent: number;
    barColor: string;
    usedText: string;
    remainText: string;
    resetTime: string;
    countdownText: string;
    countdownHighlight?: boolean;
  };
  metric2?: {
    title: string;
    percent: string;
    barPercent: number;
    barColor: string;
    usedText: string;
    remainText: string;
    resetTime: string;
    countdownText: string;
    countdownHighlight?: boolean;
  };
  metric3?: {
    title: string;
    percent: string;
    barPercent: number;
    barColor: string;
    usedText: string;
    remainText: string;
    resetTime: string;
    countdownText: string;
  };
  callsText?: string;
  // DeepSeek Balance metrics
  balanceData?: {
    balance: string;
    grantBalance: string;
    rechargeBalance: string;
    accountStatus: string;
  };
  // New API Grid metrics
  newApiData?: {
    remain: string;
    total: string;
    used: string;
    todayCost: string;
    weekCost: string;
    monthCost: string;
    req60s: string;
    token60s: string;
    totalReq: string;
  };
  // 小米 MiMo API 按量指标
  xiaomiData?: {
    accountBalance: string;
    totalConsumption: string;
    cashBalance: string;
    giftBalance: string;
    historyTokens: string;
    outputTokens: string;
    cacheHitTokens: string;
    cacheMissTokens: string;
  };
}

const REAL_ACCOUNTS: RealAccountCard[] = [
  {
    id: 'mimo-1',
    avatarChar: '米',
    name: '小米 MiMo',
    plan: 'API 按量',
    providerCode: 'MIMO',
    overallStatus: '¥58.78',
    overallStatusColor: '#00856f',
    startDate: '—',
    endDate: '—',
    cardTheme: {
      border: 'border-[#16463d]/70',
      bg: 'bg-[#10221f]/85',
      glow: 'shadow-[0_0_20px_rgba(0,133,111,0.10)]',
    },
    metricsType: 'xiaomi_grid',
    xiaomiData: {
      accountBalance: '¥58.78',
      totalConsumption: '¥1.25',
      cashBalance: '¥58.78',
      giftBalance: '¥0.00',
      historyTokens: '3,323,413',
      outputTokens: '28,658',
      cacheHitTokens: '2,695,553',
      cacheMissTokens: '599,202',
    },
  },
  {
    id: 'glm-zhao',
    avatarChar: '赵',
    name: 'GLM',
    plan: 'Coding Plan',
    providerCode: 'GLM',
    overallStatus: '75%',
    overallStatusColor: '#f59e0b',
    startDate: '—',
    endDate: '—',
    cardTheme: {
      border: 'border-[#32452e]/60',
      bg: 'bg-[#152018]/85',
      glow: 'shadow-[0_0_20px_rgba(245,158,11,0.06)]',
    },
    metricsType: 'dual_progress',
    metric1: {
      title: '5 小时用量',
      percent: '75%',
      barPercent: 75,
      barColor: '#f59e0b',
      usedText: '已用 75 / 100',
      remainText: '剩余 25',
      resetTime: '重置 21:28',
      countdownText: '剩余 42分钟',
      countdownHighlight: true,
    },
    metric2: {
      title: '每周用量',
      percent: '35%',
      barPercent: 35,
      barColor: '#22c55e',
      usedText: '已用 35 / 100',
      remainText: '剩余 65',
      resetTime: '重置 09-09 16:29',
      countdownText: '剩余 5天 19小时 43分钟',
    },
    callsText: '474 次',
  },
  {
    id: 'cmd-1',
    avatarChar: 'C',
    name: 'Command Code',
    plan: 'GOAT',
    providerCode: 'CMD',
    overallStatus: '$42.80',
    overallStatusColor: '#22d3ee',
    startDate: '2026-09-01 00:00',
    endDate: '2026-10-01 00:00',
    cardTheme: {
      border: 'border-[#1b333c]/60',
      bg: 'bg-[#0f1f26]/85',
      glow: 'shadow-[0_0_20px_rgba(34,211,238,0.05)]',
    },
    metricsType: 'balance_grid',
    balanceData: {
      balance: '$42.80',
      grantBalance: '$0.00',
      rechargeBalance: '$5.20',
      accountStatus: '47,280 次',
    },
  },
  {
    id: 'vol-sun',
    avatarChar: '孙',
    name: '火山方舟',
    plan: 'medium Plan',
    providerCode: 'VOL',
    overallStatus: '0%',
    overallStatusColor: '#22c55e',
    startDate: '—',
    endDate: '—',
    cardTheme: {
      border: 'border-[#433022]/60',
      bg: 'bg-[#1e1713]/85',
      glow: 'shadow-[0_0_20px_rgba(251,146,60,0.05)]',
    },
    metricsType: 'triple_progress',
    metric1: {
      title: '近 5 小时用量',
      percent: '1%',
      barPercent: 1,
      barColor: '#22c55e',
      usedText: '已用 69.0414 / 10000',
      remainText: '剩余 9930.9586',
      resetTime: '重置 22:28',
      countdownText: '剩余 1小时 42分钟',
    },
    metric2: {
      title: '近一周用量',
      percent: '13%',
      barPercent: 13,
      barColor: '#22c55e',
      usedText: '已用 4410.9671 / 35000',
      remainText: '剩余 30589.0329',
      resetTime: '重置 09-07 00:00',
      countdownText: '剩余 3天 3小时 14分钟',
    },
    metric3: {
      title: '近一月用量',
      percent: '42%',
      barPercent: 42,
      barColor: '#22c55e',
      usedText: '已用 42381.0638 / 100000',
      remainText: '剩余 57618.9362',
      resetTime: '重置 09-19 23:59',
      countdownText: '剩余 16天 3小时 14分钟',
    },
  },
  {
    id: 'rou-li',
    avatarChar: '李',
    name: 'Routin',
    plan: '成长版',
    providerCode: 'ROU',
    overallStatus: '0%',
    overallStatusColor: '#38bdf8',
    startDate: '2026-08-23 02:03',
    endDate: '2026-09-23 07:59',
    cardTheme: {
      border: 'border-[#1e3848]/60',
      bg: 'bg-[#101b24]/85',
      glow: 'shadow-[0_0_20px_rgba(56,189,248,0.05)]',
    },
    metricsType: 'dual_progress',
    metric1: {
      title: '5 小时',
      percent: '0%',
      barPercent: 0,
      barColor: '#38bdf8',
      usedText: '$0.00 / $60.00',
      remainText: '剩余 $60.00',
      resetTime: '重置 09-04 01:45',
      countdownText: '剩余 4小时 59分钟',
    },
    metric2: {
      title: '周',
      percent: '100%',
      barPercent: 100,
      barColor: '#ef4444',
      usedText: '$401.09 / $400.00',
      remainText: '剩余 $-1.09',
      resetTime: '重置 09-06 02:03',
      countdownText: '剩余 2天 5小时 17分钟',
      countdownHighlight: true,
    },
  },
  {
    id: 'ds-zhou',
    avatarChar: '周',
    name: 'DeepSeek',
    plan: 'API 余额',
    providerCode: 'DS',
    overallStatus: '余额 33.19 CNY',
    overallStatusColor: '#10b981',
    startDate: '—',
    endDate: '—',
    cardTheme: {
      border: 'border-[#1b333c]/60',
      bg: 'bg-[#0f1f26]/85',
      glow: 'shadow-[0_0_20px_rgba(168,85,247,0.05)]',
    },
    metricsType: 'balance_grid',
    balanceData: {
      balance: '33.19 CNY',
      grantBalance: '0 CNY',
      rechargeBalance: '33.19 CNY',
      accountStatus: '可用',
    },
  },
  {
    id: 'new-wu',
    avatarChar: '吴',
    name: 'New API',
    plan: 'New API · default',
    providerCode: 'NEW',
    overallStatus: '余额 0 额度',
    overallStatusColor: '#ef4444',
    startDate: '—',
    endDate: '—',
    cardTheme: {
      border: 'border-[#38263e]/60',
      bg: 'bg-[#1b1220]/85',
      glow: 'shadow-[0_0_20px_rgba(168,85,247,0.05)]',
    },
    metricsType: 'stat_grid',
    newApiData: {
      remain: '0 额度',
      total: '0 额度',
      used: '0 额度',
      todayCost: '0 额度',
      weekCost: '0 额度',
      monthCost: '0 额度',
      req60s: '0 次',
      token60s: '0',
      totalReq: '0 次',
    },
  },
  {
    id: 'rou-zheng',
    avatarChar: '郑',
    name: 'Routin',
    plan: '成长版',
    providerCode: 'ROU',
    overallStatus: '0%',
    overallStatusColor: '#38bdf8',
    startDate: '2026-08-27 09:07',
    endDate: '2026-09-28 07:59',
    cardTheme: {
      border: 'border-[#1e3848]/60',
      bg: 'bg-[#101b24]/85',
      glow: 'shadow-[0_0_20px_rgba(56,189,248,0.05)]',
    },
    metricsType: 'dual_progress',
    metric1: {
      title: '5 小时',
      percent: '6%',
      barPercent: 6,
      barColor: '#22c55e',
      usedText: '$3.52 / $60.00',
      remainText: '剩余 $56.48',
      resetTime: '重置 09-04 00:52',
      countdownText: '剩余 4小时 6分钟',
    },
    metric2: {
      title: '周',
      percent: '20%',
      barPercent: 20,
      barColor: '#22c55e',
      usedText: '$80.86 / $400.00',
      remainText: '剩余 $319.14',
      resetTime: '重置 09-10 09:07',
      countdownText: '剩余 6天 12小时 21分钟',
    },
  },
  {
    id: 'rou-wang',
    avatarChar: '王',
    name: 'Routin',
    plan: '成长版',
    providerCode: 'ROU',
    overallStatus: '15%',
    overallStatusColor: '#38bdf8',
    startDate: '2026-08-22 10:19',
    endDate: '2026-09-23 07:59',
    cardTheme: {
      border: 'border-[#1e3848]/60',
      bg: 'bg-[#101b24]/85',
      glow: 'shadow-[0_0_20px_rgba(56,189,248,0.05)]',
    },
    metricsType: 'dual_progress',
    metric1: {
      title: '5 小时',
      percent: '15%',
      barPercent: 15,
      barColor: '#22c55e',
      usedText: '$9.15 / $60.00',
      remainText: '剩余 $50.85',
      resetTime: '重置 09-04 01:15',
      countdownText: '剩余 4小时 29分钟',
    },
    metric2: {
      title: '周',
      percent: '87%',
      barPercent: 87,
      barColor: '#ef4444',
      usedText: '$349.16 / $400.00',
      remainText: '剩余 $50.84',
      resetTime: '重置 09-05 10:19',
      countdownText: '剩余 1天 13小时 33分钟',
      countdownHighlight: true,
    },
  },
];

export const MenuBarSimulator: React.FC<MenuBarSimulatorProps> = ({
  onOpenSettings,
  onTriggerNotification,
}) => {
  const downloads = useLatestDownloads();
  const [activeFilter, setActiveFilter] = useState<string>('ALL');
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastRefreshedTime, setLastRefreshedTime] = useState('2026-09-03 20:45:53');
  const [currentTime, setCurrentTime] = useState('20:45');
  const [powerActive, setPowerActive] = useState(true);

  // Live real clock updater for the menu bar
  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      const h = String(now.getHours()).padStart(2, '0');
      const m = String(now.getMinutes()).padStart(2, '0');
      setCurrentTime(`${h}:${m}`);
    };
    updateClock();
    const interval = setInterval(updateClock, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleRefresh = () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    setTimeout(() => {
      const now = new Date();
      const y = now.getFullYear();
      const mo = String(now.getMonth() + 1).padStart(2, '0');
      const d = String(now.getDate()).padStart(2, '0');
      const h = String(now.getHours()).padStart(2, '0');
      const m = String(now.getMinutes()).padStart(2, '0');
      const s = String(now.getSeconds()).padStart(2, '0');
      setLastRefreshedTime(`${y}-${mo}-${d} ${h}:${m}:${s}`);
      setIsRefreshing(false);
      onTriggerNotification('已完成最新快照拉取', `全量 ${REAL_ACCOUNTS.length} 个 Key 指标与周期重置倒计时已即时同步。`);
    }, 650);
  };

  const filteredCards =
    activeFilter === 'ALL'
      ? REAL_ACCOUNTS
      : REAL_ACCOUNTS.filter((c) => c.providerCode === activeFilter);

  return (
    <div className="w-full max-w-4xl relative flex flex-col items-center">
      {/* Ambient radial glow behind the whole simulator */}
      <div className="absolute inset-0 bg-[#007aff]/10 blur-3xl -z-10 rounded-full pointer-events-none" />

      {/* ============================================================== */}
      {/* 1. macOS Menu Bar (Image 3: Actual macOS status capsule bar) */}
      {/* ============================================================== */}
      <div
        id="macos-menubar"
        className="w-full h-10 rounded-t-2xl bg-[#090b10]/95 backdrop-blur-2xl flex items-center justify-between px-3 sm:px-5 border-t border-x border-white/[0.1] shadow-2xl select-none text-xs"
      >
        {/* Left: System Menu items */}
        <div className="flex items-center gap-3 sm:gap-4 text-[#8f96a3]">
          <span className="text-white text-sm font-semibold hover:opacity-80 transition-opacity cursor-default">
            
          </span>
          <span className="font-semibold text-white tracking-tight">MyToken</span>
          <span className="hover:text-white cursor-default hidden sm:inline transition-colors">文件</span>
          <span
            onClick={onOpenSettings}
            className="hover:text-white cursor-pointer hidden sm:inline transition-colors"
          >
            偏好设置
          </span>
          <span className="hover:text-white cursor-default hidden md:inline transition-colors">窗口</span>
          <span className="hover:text-white cursor-default hidden md:inline transition-colors">帮助</span>
        </div>

        {/* Right: The Actual Menu Bar Status Bar Capsule Widget (Image 3) */}
        <div className="flex items-center gap-2 sm:gap-3">
          <div
            id="menubar-status-items"
            title="MyToken 菜单栏指标"
            className="flex items-center gap-1.5 cursor-pointer"
            onClick={() => setActiveFilter('ALL')}
          >
            {[
              { code: 'GLM', percent: 75, color: '#f59e0b' },
              { code: 'CMD', percent: 61, color: '#22d3ee' },
              { code: 'VOL', percent: 16, color: '#22c55e' },
              { code: 'ROU', percent: 0, color: '#8c9ba5' },
              { code: 'DS', percent: 40, color: '#22c55e' },
              { code: 'MIMO', percent: 55, color: '#00856f' },
            ].map((item) => (
              <div key={item.code} className="flex items-center gap-1">
                <div className="flex flex-col text-[7px] font-mono font-extrabold text-white leading-[7px] tracking-tighter text-center">
                  {item.code.split('').map((character) => (
                    <span key={character}>{character}</span>
                  ))}
                </div>
                <div className="w-[4px] h-4 rounded-full bg-[#333338] relative overflow-hidden flex flex-col justify-end">
                  <div
                    className="w-full rounded-full"
                    style={{ height: `${item.percent}%`, backgroundColor: item.color }}
                  />
                </div>
              </div>
            ))}
          </div>

          {/* Additional macOS status items (Image 3 right side) */}
          <div className="hidden sm:flex items-center gap-2 text-[#94a3b8]">
            <Key className="w-3.5 h-3.5 hover:text-white transition-colors" />
            <Clock className="w-3.5 h-3.5 hover:text-white transition-colors" />
            <div className="w-3.5 h-3.5 rounded-full border border-white/40 flex items-center justify-center text-[8px] font-bold text-white">
              R
            </div>
            <div className="flex items-center gap-1 text-[10px] font-mono font-medium text-[#cbd5e1]">
              <span className="text-[9px] text-[#64748b]">PWR</span>
              <span>25W</span>
            </div>
            <span className="font-mono text-white text-xs ml-1">{currentTime}</span>
          </div>
        </div>
      </div>

      {/* Popover Triangle Anchor Notch */}
      <div className="w-0 h-0 border-l-[9px] border-l-transparent border-r-[9px] border-r-transparent border-b-[9px] border-b-[#1c2024]/95 self-end mr-12 sm:mr-32 -mt-[1px] z-20" />

      {/* ============================================================== */}
      {/* 2. Popover Window Showcase (Images 1 & 2: Authentic Real UI)    */}
      {/* ============================================================== */}
      <div
        id="macos-popover-window"
        className="w-full max-w-[490px] bg-[#1a1e24]/95 backdrop-blur-3xl rounded-[22px] border border-white/[0.12] shadow-[0_25px_65px_rgba(0,0,0,0.85)] p-3.5 sm:p-4 text-left flex flex-col gap-3 z-10 -mt-1"
      >
        {/* Top Header of Popover: 当前版本、Logo 与操作按钮 */}
        <div className="flex items-center justify-between pb-1">
          {/* Left: Version badge */}
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-[#272b32] border border-white/10 text-xs text-[#cbd5e1] font-mono">
            <span className="text-[#94a3b8]">🏷️</span>
            <span>{downloads.macos.version}</span>
            <ExternalLink className="w-3 h-3 text-[#64748b]" />
          </div>

          {/* Center: MyToken Official App Icon */}
          <div className="flex items-center justify-center">
            <div className="relative w-8 h-8 rounded-xl overflow-hidden border border-white/15 flex items-center justify-center shadow-[0_0_15px_rgba(56,189,248,0.25)] hover:scale-105 transition-transform">
              <img
                src="/app-icon.png"
                alt="MyToken Official Icon"
                className="w-full h-full object-cover"
              />
            </div>
          </div>

          {/* Right: Refresh & Checkmark */}
          <div className="flex items-center gap-1.5">
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="p-1.5 rounded-lg bg-[#272b32] hover:bg-[#333842] border border-white/10 text-[#cbd5e1] hover:text-white transition-colors cursor-pointer"
              title="即刻拉取最新快照"
            >
              <RotateCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-[#38bdf8]' : ''}`} />
            </button>
            <div
              className="p-1.5 rounded-lg bg-[#272b32] border border-white/10 text-[#22c55e]"
              title="官方接口服务探活正常"
            >
              <Check className="w-3.5 h-3.5" />
            </div>
          </div>
        </div>

        {/* Subtitle & Quick Filter Bar */}
        <div className="flex items-end justify-between pt-0.5 pb-1 border-b border-white/[0.06]">
          <div>
            <h3 className="text-base sm:text-lg font-bold text-white tracking-tight leading-none">
              账户用量
            </h3>
            <span className="text-xs text-[#94a3b8] font-mono mt-1 block">
              {REAL_ACCOUNTS.length} 个 Key
            </span>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1 overflow-x-auto text-[11px] font-medium">
            {[
              { label: '全部', code: 'ALL' },
              { label: 'GLM', code: 'GLM' },
              { label: '火山', code: 'VOL' },
              { label: 'Routin', code: 'ROU' },
              { label: 'DeepSeek', code: 'DS' },
              { label: 'New API', code: 'NEW' },
              { label: 'Command Code', code: 'CMD' },
              { label: '小米 MiMo', code: 'MIMO' },
            ].map((tab) => (
              <button
                key={tab.code}
                onClick={() => setActiveFilter(tab.code)}
                className={`px-2 py-0.5 rounded-md transition-colors cursor-pointer whitespace-nowrap ${
                  activeFilter === tab.code
                    ? 'bg-[#38bdf8]/20 text-[#7bd0ff] font-semibold border border-[#38bdf8]/40'
                    : 'text-[#8c9ba5] hover:text-white hover:bg-white/[0.05]'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Scrollable Cards Container (Faithful to Images 1 & 2) */}
        <div className="flex flex-col gap-3 max-h-[460px] overflow-y-auto pr-1 select-none custom-scroll">
          {filteredCards.map((card) => {
            const isSelected = selectedAccountId === card.id;

            return (
              <div
                key={card.id}
                onClick={() => setSelectedAccountId(isSelected ? null : card.id)}
                className={`p-3.5 rounded-2xl border transition-all duration-200 cursor-pointer ${card.cardTheme.bg} ${card.cardTheme.border} ${card.cardTheme.glow} ${
                  isSelected ? 'ring-1 ring-[#38bdf8]/50' : 'hover:brightness-105'
                }`}
              >
                {/* 1. Card Top Header */}
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-start gap-2.5">
                    {/* Big Chinese Avatar character */}
                    <span className="text-xl sm:text-2xl font-bold text-white leading-none">
                      {card.avatarChar}
                    </span>

                    <div>
                      <div className="text-xs sm:text-sm font-semibold text-white flex items-center gap-1.5">
                        <span>{card.name}</span>
                        <span className="text-[#64748b]">·</span>
                        <span className="text-[#cbd5e1]">{card.plan}</span>
                      </div>
                      <div className="text-[11px] font-mono text-[#8c9ba5] mt-0.5">
                        <span>开始 {card.startDate}</span>
                        <span className="ml-3">结束 {card.endDate}</span>
                      </div>
                    </div>
                  </div>

                  {/* Top Right Percentage / Balance */}
                  <div className="flex items-center gap-1">
                    <span
                      className="text-sm sm:text-base font-bold font-mono"
                      style={{ color: card.overallStatusColor }}
                    >
                      {card.overallStatus}
                    </span>
                    {card.providerCode === 'ROU' && (
                      <Search className="w-3.5 h-3.5 text-[#64748b] hover:text-white transition-colors" />
                    )}
                  </div>
                </div>

                {/* 2. DUAL PROGRESS METRICS (GLM & Routin) */}
                {card.metricsType === 'dual_progress' && card.metric1 && card.metric2 && (
                  <div>
                    <div className="grid grid-cols-2 gap-3 sm:gap-4 my-1">
                      {/* Metric 1 */}
                      <div className="flex flex-col gap-1 text-[11px] font-mono">
                        <div className="flex items-center justify-between">
                          <span className="text-[#cbd5e1]">{card.metric1.title}</span>
                          <span
                            className="font-bold"
                            style={{ color: card.metric1.barColor }}
                          >
                            {card.metric1.percent}
                          </span>
                        </div>
                        {/* Horizontal Progress Bar */}
                        <div className="w-full h-1.5 rounded-full bg-[#272b32] overflow-hidden my-0.5">
                          <div
                            className="h-full rounded-full transition-all duration-500"
                            style={{
                              width: `${card.metric1.barPercent}%`,
                              backgroundColor: card.metric1.barColor,
                            }}
                          />
                        </div>
                        <div className="text-[#cbd5e1]">{card.metric1.usedText}</div>
                        <div className="text-[#cbd5e1]">{card.metric1.remainText}</div>
                        <div className="text-[#cbd5e1]">{card.metric1.resetTime}</div>
                        <div
                          className={`font-medium ${
                            card.metric1.countdownHighlight
                              ? 'text-[#22c55e]'
                              : 'text-[#8c9ba5]'
                          }`}
                        >
                          {card.metric1.countdownText}
                        </div>
                      </div>

                      {/* Metric 2 */}
                      <div className="flex flex-col gap-1 text-[11px] font-mono">
                        <div className="flex items-center justify-between">
                          <span className="text-[#cbd5e1]">{card.metric2.title}</span>
                          <span
                            className="font-bold"
                            style={{ color: card.metric2.barColor }}
                          >
                            {card.metric2.percent}
                          </span>
                        </div>
                        {/* Horizontal Progress Bar */}
                        <div className="w-full h-1.5 rounded-full bg-[#272b32] overflow-hidden my-0.5">
                          <div
                            className="h-full rounded-full transition-all duration-500"
                            style={{
                              width: `${card.metric2.barPercent}%`,
                              backgroundColor: card.metric2.barColor,
                            }}
                          />
                        </div>
                        <div className="text-[#cbd5e1]">{card.metric2.usedText}</div>
                        <div className="text-[#cbd5e1]">{card.metric2.remainText}</div>
                        <div className="text-[#cbd5e1]">{card.metric2.resetTime}</div>
                        <div
                          className={`font-medium ${
                            card.metric2.countdownHighlight
                              ? 'text-[#22c55e]'
                              : 'text-[#8c9ba5]'
                          }`}
                        >
                          {card.metric2.countdownText}
                        </div>
                      </div>
                    </div>

                    {/* Footer Row (GLM calls count) */}
                    {card.callsText && (
                      <div className="flex items-center justify-between pt-2 mt-1 border-t border-white/[0.05] text-[11px] text-[#8c9ba5]">
                        <div className="flex items-center gap-2">
                          <span>调用量</span>
                          <span className="text-white font-mono font-bold text-sm">
                            {card.callsText}
                          </span>
                        </div>
                        <span className="text-[#cbd5e1] hover:text-white transition-colors cursor-pointer">
                          账户信息
                        </span>
                      </div>
                    )}
                  </div>
                )}

                {/* 3. TRIPLE PROGRESS METRICS (火山方舟) */}
                {card.metricsType === 'triple_progress' &&
                  card.metric1 &&
                  card.metric2 &&
                  card.metric3 && (
                    <div className="flex flex-col gap-2.5 my-1 text-[11px] font-mono">
                      <div className="grid grid-cols-2 gap-3 sm:gap-4">
                        {/* 5小时 */}
                        <div className="flex flex-col gap-1">
                          <div className="flex items-center justify-between">
                            <span className="text-[#cbd5e1]">{card.metric1.title}</span>
                            <span className="text-[#22c55e] font-bold">
                              {card.metric1.percent}
                            </span>
                          </div>
                          <div className="w-full h-1.5 rounded-full bg-[#272b32] overflow-hidden my-0.5">
                            <div
                              className="h-full rounded-full bg-[#22c55e]"
                              style={{ width: `${card.metric1.barPercent}%` }}
                            />
                          </div>
                          <div className="text-[#cbd5e1] truncate">{card.metric1.usedText}</div>
                          <div className="text-[#cbd5e1]">{card.metric1.remainText}</div>
                          <div className="text-[#cbd5e1]">{card.metric1.resetTime}</div>
                          <div className="text-[#22c55e]">{card.metric1.countdownText}</div>
                        </div>

                        {/* 一周 */}
                        <div className="flex flex-col gap-1">
                          <div className="flex items-center justify-between">
                            <span className="text-[#cbd5e1]">{card.metric2.title}</span>
                            <span className="text-[#22c55e] font-bold">
                              {card.metric2.percent}
                            </span>
                          </div>
                          <div className="w-full h-1.5 rounded-full bg-[#272b32] overflow-hidden my-0.5">
                            <div
                              className="h-full rounded-full bg-[#22c55e]"
                              style={{ width: `${card.metric2.barPercent}%` }}
                            />
                          </div>
                          <div className="text-[#cbd5e1] truncate">{card.metric2.usedText}</div>
                          <div className="text-[#cbd5e1]">{card.metric2.remainText}</div>
                          <div className="text-[#cbd5e1]">{card.metric2.resetTime}</div>
                          <div className="text-[#22c55e]">{card.metric2.countdownText}</div>
                        </div>
                      </div>

                      {/* 一月 */}
                      <div className="flex flex-col gap-1 pt-1.5 border-t border-white/[0.05]">
                        <div className="flex items-center justify-between">
                          <span className="text-[#cbd5e1]">{card.metric3.title}</span>
                          <span className="text-[#22c55e] font-bold">
                            {card.metric3.percent}
                          </span>
                        </div>
                        <div className="w-full h-1.5 rounded-full bg-[#272b32] overflow-hidden my-0.5">
                          <div
                            className="h-full rounded-full bg-[#22c55e]"
                            style={{ width: `${card.metric3.barPercent}%` }}
                          />
                        </div>
                        <div className="flex justify-between items-center text-[#cbd5e1]">
                          <span>{card.metric3.usedText}</span>
                          <span>{card.metric3.remainText}</span>
                        </div>
                        <div className="flex justify-between items-center text-[#8c9ba5]">
                          <span>{card.metric3.resetTime}</span>
                          <span className="text-[#22c55e]">{card.metric3.countdownText}</span>
                        </div>
                      </div>
                    </div>
                  )}

                {/* 4. BALANCE GRID (DeepSeek) */}
                {card.metricsType === 'balance_grid' && card.balanceData && (
                  <div className="grid grid-cols-2 gap-3.5 my-1 text-[11px] font-mono">
                    {/* Left: 总余额 */}
                    <div className="flex flex-col gap-0.5">
                      <div className="text-base sm:text-lg font-bold text-[#10b981]">
                        {card.balanceData.balance}
                      </div>
                      <div className="text-[#8c9ba5]">{card.providerCode === 'CMD' ? '月度剩余' : '账户余额'}</div>
                      <div className="mt-2 text-white font-semibold">
                        {card.balanceData.rechargeBalance}
                      </div>
                      <div className="text-[#8c9ba5]">{card.providerCode === 'CMD' ? '购买剩余' : '充值余额'}</div>
                    </div>

                    {/* Right: 赠金 & 可用状态 */}
                    <div className="flex flex-col gap-0.5">
                      <div className="text-base sm:text-lg font-bold text-white">
                        {card.balanceData.grantBalance}
                      </div>
                      <div className="text-[#8c9ba5]">{card.providerCode === 'CMD' ? '赠送剩余' : '赠金余额'}</div>
                      <div className="mt-2 text-[#10b981] font-semibold">
                        {card.balanceData.accountStatus}
                      </div>
                      <div className="text-[#8c9ba5]">{card.providerCode === 'CMD' ? '累计请求' : '账户状态'}</div>
                    </div>
                  </div>
                )}

                {/* 5. STAT GRID (New API) */}
                {/* 小米 MiMo API 指标：字段在上、数值在下的四列布局 */}
                {card.metricsType === 'xiaomi_grid' && card.xiaomiData && (
                  <div className="my-1 flex flex-col gap-3 text-[11px] font-mono">
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {[
                        ['账户余额', card.xiaomiData.accountBalance],
                        ['累计消费', card.xiaomiData.totalConsumption],
                        ['现金余额', card.xiaomiData.cashBalance],
                        ['赠送余额', card.xiaomiData.giftBalance],
                      ].map(([label, value]) => (
                        <div key={label} className="flex flex-col gap-0.5">
                          <span className="text-[#8c9ba5]">{label}</span>
                          <span className="font-semibold text-[#00856f]">{value}</span>
                        </div>
                      ))}
                    </div>
                    <div className="flex flex-col gap-2">
                      <span className="text-[10px] font-semibold text-[#8c9ba5]">Token</span>
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                        {[
                          ['历史消耗', card.xiaomiData.historyTokens],
                          ['输出', card.xiaomiData.outputTokens],
                          ['命中缓存', card.xiaomiData.cacheHitTokens],
                          ['未命中缓存', card.xiaomiData.cacheMissTokens],
                        ].map(([label, value]) => (
                          <div key={label} className="flex flex-col gap-0.5">
                            <span className="text-[#8c9ba5]">{label}</span>
                            <span className="font-semibold text-white">{value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
                {card.metricsType === 'stat_grid' && card.newApiData && (
                  <div className="grid grid-cols-2 gap-2 my-1 text-[11px] font-mono">
                    <div>
                      <div className="text-[#ef4444] font-bold">
                        {card.newApiData.remain}
                      </div>
                      <div className="text-[#8c9ba5]">剩余额度</div>
                    </div>
                    <div>
                      <div className="text-white font-bold">
                        {card.newApiData.total}
                      </div>
                      <div className="text-[#8c9ba5]">总额度</div>
                    </div>
                    <div>
                      <div className="text-white">
                        {card.newApiData.used}
                      </div>
                      <div className="text-[#8c9ba5]">已用额度</div>
                    </div>
                    <div>
                      <div className="text-white">
                        {card.newApiData.todayCost}
                      </div>
                      <div className="text-[#8c9ba5]">今日消费</div>
                    </div>
                    <div>
                      <div className="text-white">
                        {card.newApiData.weekCost}
                      </div>
                      <div className="text-[#8c9ba5]">近 7 天消费</div>
                    </div>
                    <div>
                      <div className="text-white">
                        {card.newApiData.monthCost}
                      </div>
                      <div className="text-[#8c9ba5]">近 30 天消费</div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Window Bottom Footer Bar (Images 1 & 2 bottom bar) */}
        <div className="flex items-center justify-between pt-2 border-t border-white/[0.08] text-xs text-[#8c9ba5]">
          {/* Left: Last refreshed time */}
          <div className="flex items-center gap-1.5 font-mono text-[11px]">
            <Clock className="w-3.5 h-3.5 text-[#8c9ba5]" />
            <span>最后刷新 {lastRefreshedTime}</span>
          </div>

          {/* Right: Settings and Power button */}
          <div className="flex items-center gap-2">
            <button
              onClick={onOpenSettings}
              className="p-1.5 rounded-lg bg-[#272b32] hover:bg-[#353942] border border-white/10 text-[#cbd5e1] hover:text-white transition-colors cursor-pointer"
              title="偏好设置与密钥管理"
            >
              <Settings className="w-4 h-4" />
            </button>

            <button
              onClick={() => {
                setPowerActive(!powerActive);
                onTriggerNotification(
                  powerActive ? 'MyToken 监控已暂停' : 'MyToken 监控已唤醒',
                  powerActive ? '菜单栏与后台静默轮询已挂起。' : '正在恢复菜单栏指标与本机凭证连接。'
                );
              }}
              className={`p-1.5 rounded-lg border transition-colors cursor-pointer ${
                powerActive
                  ? 'bg-[#272b32] hover:bg-[#353942] border-white/10 text-[#cbd5e1] hover:text-[#ef4444]'
                  : 'bg-[#ef4444]/20 border-[#ef4444]/40 text-[#ef4444]'
              }`}
              title={powerActive ? '挂起 / 退出监控' : '唤醒监控'}
            >
              <Power className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
