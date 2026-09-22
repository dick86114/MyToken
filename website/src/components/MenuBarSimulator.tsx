import React, { useState, useEffect } from 'react';
import { useLatestDownloads } from '../hooks/useLatestDownloads';
import { ProviderItem } from '../types';
import {
  RotateCw,
  Clock,
  Settings,
  Power,
  ExternalLink,
  Share,
  ChevronDown,
  Check,
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

/** 与 macOS 端一致的简洁卡片语义：>=80% 告急，>=50% 预警。 */
const parsePct = (value?: string): number => {
  const parsed = parseFloat((value || '').replace('%', ''));
  return Number.isFinite(parsed) ? parsed : 0;
};

type Tone = 'normal' | 'warning' | 'critical';
const toneOf = (percent: number): Tone =>
  percent >= 80 ? 'critical' : percent >= 50 ? 'warning' : 'normal';

const ProviderName: Record<string, string> = {
  GLM: 'GLM',
  ROU: 'Routin',
  DS: 'DeepSeek',
  VOL: '火山方舟',
  NEW: 'New API',
  CMD: 'Command Code',
  MIMO: '小米 MiMo',
};

const ProviderAccent: Record<string, string> = {
  GLM: '#10b981',
  ROU: '#3b82f6',
  DS: '#6366f1',
  VOL: '#f97316',
  NEW: '#a855f7',
  CMD: '#22d3ee',
  MIMO: '#ec4899',
};

const metricTones = (card: RealAccountCard): Tone[] => {
  if (card.metricsType === 'dual_progress')
    return [parsePct(card.metric1?.percent), parsePct(card.metric2?.percent)].map(toneOf);
  if (card.metricsType === 'triple_progress')
    return [
      parsePct(card.metric1?.percent),
      parsePct(card.metric2?.percent),
      parsePct(card.metric3?.percent),
    ].map(toneOf);
  return [];
};

const nearlyExhausted = (card: RealAccountCard) =>
  metricTones(card).some((tone) => tone === 'critical');

const tileSurface = (tone: Tone): string =>
  tone === 'critical'
    ? 'bg-[#ef4444]/[0.12] border border-[#ef4444]/40'
    : tone === 'warning'
      ? 'bg-[#f59e0b]/[0.12] border border-[#f59e0b]/40'
      : '';

const Ring: React.FC<{
  percent: number;
  size: number;
  color: string;
  numberSize: number;
  percentSize: number;
}> = ({ percent, size, color, numberSize, percentSize }) => {
  const stroke = size * 0.1;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.min(Math.max(percent, 0), 100);

  return (
    <div className="relative flex-shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke="rgba(255,255,255,0.08)"
          strokeWidth={stroke}
          fill="none"
        />
        {clamped > 0 && (
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={color}
            strokeWidth={stroke}
            fill="none"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={circumference * (1 - clamped / 100)}
          />
        )}
      </svg>
      <div className="absolute inset-0 flex items-baseline justify-center">
        <span
          className="font-mono font-bold text-white leading-none"
          style={{ fontSize: numberSize }}
        >
          {Math.round(clamped)}
        </span>
        <span
          className="font-mono font-semibold text-white/70 leading-none"
          style={{ fontSize: percentSize }}
        >
          %
        </span>
      </div>
    </div>
  );
};

const CircleButton: React.FC<{
  title: string;
  onClick?: () => void;
  disabled?: boolean;
  children: React.ReactNode;
}> = ({ title, onClick, disabled, children }) => (
  <button
    title={title}
    onClick={onClick}
    disabled={disabled}
    className="w-[28px] h-[28px] rounded-full bg-white/[0.06] border border-white/[0.12] flex items-center justify-center text-[#94a3b8] hover:text-white hover:bg-white/[0.10] transition-colors cursor-pointer disabled:opacity-40"
  >
    {children}
  </button>
);

export const MenuBarSimulator: React.FC<MenuBarSimulatorProps> = ({
  providers,
  onOpenSettings,
  onTriggerNotification,
}) => {
  const { macos } = useLatestDownloads();
  const [activeFilter, setActiveFilter] = useState<string>('ALL');
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastRefreshedTime, setLastRefreshedTime] = useState('2026-09-22 20:45:53');
  const [currentTime, setCurrentTime] = useState('20:45');
  const [powerActive, setPowerActive] = useState(true);

  useEffect(() => {
    const interval = setInterval(() => {
      const now = new Date();
      setCurrentTime(
        `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
      );
    }, 15000);
    return () => clearInterval(interval);
  }, []);

  const handleRefresh = () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    setTimeout(() => {
      setLastRefreshedTime(
        `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(
          now.getHours()
        )}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
      );
      setIsRefreshing(false);
      onTriggerNotification('用量已刷新', '所有已启用凭证的用量快照已更新。');
    }, 900);
  };

  const filteredCards = REAL_ACCOUNTS.filter(
    (card) => activeFilter === 'ALL' || card.providerCode === activeFilter
  );

  return (
    <div className="w-full max-w-4xl relative flex flex-col items-center">
      {/* Ambient radial glow behind the whole simulator */}
      <div className="absolute inset-0 bg-[#007aff]/10 blur-3xl -z-10 rounded-full pointer-events-none" />

      {/* 1. macOS Menu Bar */}
      <div
        id="macos-menubar"
        className="w-full h-10 rounded-t-2xl bg-[#090b10]/95 backdrop-blur-2xl flex items-center justify-between px-3 sm:px-5 border-t border-x border-white/[0.1] shadow-2xl select-none text-xs"
      >
        <div className="flex items-center gap-3 sm:gap-4 text-[#8f96a3]">
          <span className="text-white text-sm font-semibold"></span>
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

          <div className="hidden sm:flex items-center gap-2 text-[#94a3b8]">
            <Clock className="w-3.5 h-3.5" />
            <span className="font-mono text-white text-xs ml-1">{currentTime}</span>
          </div>
        </div>
      </div>

      {/* Popover Triangle Anchor Notch */}
      <div className="w-0 h-0 border-l-[9px] border-l-transparent border-r-[9px] border-r-transparent border-b-[9px] border-b-[#1c2024]/95 self-end mr-12 sm:mr-32 -mt-[1px] z-20" />

      {/* 2. Popover Window（新版简洁卡片） */}
      <div
        id="macos-popover-window"
        className="w-full max-w-[490px] bg-[#0a0d14]/95 backdrop-blur-3xl rounded-[22px] border border-white/[0.10] shadow-[0_25px_65px_rgba(0,0,0,0.85)] p-3.5 sm:p-4 text-left flex flex-col gap-3 z-10 -mt-1"
      >
        {/* Top Toolbar */}
        <div className="flex items-center justify-between pb-1">
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/[0.07] border border-white/[0.12] text-[11px] font-mono font-semibold text-[#cbd5e1]">
            {macos.version}
          </div>

          <div className="flex items-center justify-center">
            <div className="relative w-9 h-9 rounded-[14px] overflow-hidden border border-white/15 shadow-[0_4px_14px_rgba(0,0,0,0.45)]">
              <img
                src="/app-icon.png"
                alt="MyToken Official Icon"
                className="w-full h-full object-cover"
              />
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              title="刷新全部 Key"
              className="w-[28px] h-[28px] rounded-full bg-white/[0.05] border border-white/[0.14] flex items-center justify-center text-[#cbd5e1] hover:bg-white/[0.10] transition-colors cursor-pointer disabled:opacity-40"
            >
              <RotateCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-[#38bdf8]' : ''}`} />
            </button>
            <div
              className="w-[28px] h-[28px] rounded-full bg-white/[0.05] border border-white/[0.14] flex items-center justify-center text-[#22c55e]"
              title="官方接口服务探活正常"
            >
              <Check className="w-3.5 h-3.5" />
            </div>
          </div>
        </div>

        {/* Title & Provider Filter */}
        <div className="flex items-center justify-between pt-0.5">
          <div className="flex items-center gap-2">
            <h3 className="text-[23px] leading-none font-bold text-white tracking-tight">
              账户用量
            </h3>
            <span className="text-[11px] font-medium text-[#34d399] bg-[#10f49c]/10 border border-[#10f49c]/30 rounded-full px-2 py-0.5">
              {REAL_ACCOUNTS.length} 个 Key
            </span>
          </div>

          <button
            onClick={() => setActiveFilter('ALL')}
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-[9px] bg-white/[0.08] border border-white/[0.14] text-[11px] font-medium text-white/90 hover:bg-white/[0.12] transition-colors cursor-pointer"
          >
            <ChevronDown className="w-3 h-3 text-white/50" />
            供应商：全部
          </button>
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
                  ? 'bg-white/10 text-white font-semibold border border-white/20'
                  : 'text-[#8c9ba5] hover:text-white hover:bg-white/[0.05] border border-transparent'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Cards */}
        <div className="flex flex-col gap-3.5 max-h-[460px] overflow-y-auto pr-1 select-none custom-scroll">
          {filteredCards.map((card) => {
            const isSelected = selectedAccountId === card.id;
            const tones = metricTones(card);
            const isBalanceCard = card.metricsType === 'balance_grid' || card.metricsType === 'xiaomi_grid';
            const nearly = nearlyExhausted(card);
            const accent = ProviderAccent[card.providerCode] || '#10b981';

            return (
              <div
                key={card.id}
                onClick={() => setSelectedAccountId(isSelected ? null : card.id)}
                className={`relative overflow-hidden p-[14px] rounded-[16px] border transition-all duration-200 cursor-pointer bg-[#121722]/90 ${
                  isSelected ? 'border-white/25' : 'border-white/10 hover:border-white/20'
                }`}
              >
                {nearly && (
                  <div className="absolute -top-10 -right-10 w-28 h-28 bg-[#ef4444]/15 rounded-full blur-2xl pointer-events-none" />
                )}
                {isBalanceCard && (
                  <svg
                    className="absolute bottom-0 left-0 w-full h-10 text-[#10b981]/10 pointer-events-none"
                    viewBox="0 0 100 40"
                    preserveAspectRatio="none"
                    fill="currentColor"
                  >
                    <path d="M0,35 Q25,12 55,26 T100,10 L100,40 L0,40 Z" />
                  </svg>
                )}

                {/* Card Header */}
                <div className="relative z-10 flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div
                      className="w-[30px] h-[30px] rounded-[12px] flex items-center justify-center text-[12px] font-bold"
                      style={{
                        color: accent,
                        backgroundColor: `${accent}26`,
                        border: `1px solid ${accent}4D`,
                      }}
                    >
                      {card.avatarChar}
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-[13.5px] font-semibold text-white leading-none">
                          {card.name}
                        </span>
                        {nearly && (
                          <span className="text-[10px] font-medium text-[#ef4444] bg-[#ef4444]/10 border border-[#ef4444]/25 rounded px-1.5 leading-4">
                            即将耗尽
                          </span>
                        )}
                      </div>
                      <div className="text-[10.5px] font-medium text-[#94a3b8] mt-[3px]">
                        {card.plan
                          ? `${ProviderName[card.providerCode] || card.providerCode} · ${card.plan}`
                          : ProviderName[card.providerCode] || card.providerCode}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <CircleButton
                      title={`分享 ${card.name}`}
                      onClick={() => setSelectedAccountId(card.id)}
                    >
                      <Share className="w-3.5 h-3.5" />
                    </CircleButton>
                    <CircleButton title={`刷新 ${card.name}`} onClick={handleRefresh}>
                      <RotateCw
                        className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`}
                      />
                    </CircleButton>
                  </div>
                </div>

                {/* Metrics Body */}
                <div className="relative z-10 mt-2.5">
                  {isBalanceCard && (
                    <div className="flex items-center justify-between py-1">
                      <div className="w-2" />
                      <div className="flex flex-col items-center">
                        <span className="text-[10px] font-medium text-[#94a3b8]">
                          {card.metricsType === 'xiaomi_grid' ? '账户余额' : '余额'}
                        </span>
                        <span
                          className="text-[20px] font-bold leading-tight"
                          style={{ color: '#10F49C' }}
                        >
                          {card.metricsType === 'xiaomi_grid'
                            ? card.xiaomiData?.accountBalance
                            : card.balanceData?.balance}
                        </span>
                      </div>
                      <span className="text-[10px] font-mono font-medium text-[#34d399] bg-[#10b981]/10 border border-[#10b981]/25 rounded-full px-2 py-0.5">
                        充足
                      </span>
                    </div>
                  )}

                  {card.metricsType === 'dual_progress' && card.metric1 && card.metric2 && (
                    <div className="flex gap-2.5">
                      {[card.metric1, card.metric2].map((metric) => {
                        const percent = parsePct(metric.percent);
                        const tone = toneOf(percent);
                        return (
                          <div
                            key={metric.title}
                            className={`flex-1 rounded-[12px] p-2.5 flex items-center gap-3 ${tileSurface(tone)}`}
                          >
                            <Ring
                              percent={percent}
                              size={44}
                              color={metric.barColor}
                              numberSize={13.5}
                              percentSize={9.5}
                            />
                            <div className="min-w-0">
                              <div className="text-[11px] font-medium text-white/80 leading-tight">
                                {metric.title}
                              </div>
                              <div className="text-[10px] font-mono text-[#94a3b8] mt-1 truncate">
                                {metric.resetTime}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {card.metricsType === 'triple_progress' &&
                    card.metric1 &&
                    card.metric2 &&
                    card.metric3 && (
                      <div className="flex gap-2">
                        {[card.metric1, card.metric2, card.metric3].map((metric) => {
                          const percent = parsePct(metric.percent);
                          const tone = toneOf(percent);
                          const toneColor =
                            tone === 'critical'
                              ? '#ef4444'
                              : tone === 'warning'
                                ? '#f59e0b'
                                : null;
                          const subtitle =
                            tone === 'critical'
                              ? '即将耗尽'
                              : tone === 'warning'
                                ? '用量偏高'
                                : metric.resetTime;
                          return (
                            <div
                              key={metric.title}
                              className={`flex-1 rounded-[12px] p-2 flex flex-col items-center text-center gap-1.5 ${tileSurface(tone)}`}
                            >
                              <Ring
                                percent={percent}
                                size={40}
                                color={tone === 'normal' ? '#22c55e' : toneColor!}
                                numberSize={11.5}
                                percentSize={8}
                              />
                              <div
                                className={`text-[10px] font-medium leading-tight ${
                                  toneColor ? '' : 'text-white/80'
                                }`}
                                style={toneColor ? { color: toneColor } : undefined}
                              >
                                {metric.title}
                              </div>
                              <div
                                className={`text-[9px] font-mono truncate w-full ${
                                  toneColor ? '' : 'text-[#94a3b8]'
                                }`}
                                style={toneColor ? { color: toneColor, opacity: 0.85 } : undefined}
                              >
                                {subtitle}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                  {card.metricsType === 'stat_grid' && card.newApiData && (
                    <div className="grid grid-cols-2 gap-2">
                      {[
                        { label: '剩余额度', value: card.newApiData.remain, color: '#ef4444' },
                        { label: '总额度', value: card.newApiData.total, color: undefined },
                        { label: '已用额度', value: card.newApiData.used, color: undefined },
                        { label: '今日消费', value: card.newApiData.todayCost, color: undefined },
                      ].map((cell) => (
                        <div key={cell.label} className="rounded-[12px] p-2.5">
                          <div className="text-[10px] font-medium text-[#94a3b8]">
                            {cell.label}
                          </div>
                          <div
                            className="text-[12px] font-semibold text-white mt-0.5"
                            style={cell.color ? { color: cell.color } : undefined}
                          >
                            {cell.value}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Floating Bottom Bar */}
        <div className="relative z-10 mx-auto w-full max-w-[420px] -mb-5 rounded-full bg-[#121722]/95 border border-white/[0.14] shadow-[0_12px_32px_rgba(0,0,0,0.45)] px-3 py-2 flex items-center justify-between">
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            title="刷新全部 Key"
            className="w-[28px] h-[28px] rounded-full bg-white/[0.05] border border-white/[0.14] flex items-center justify-center text-[#cbd5e1] hover:bg-white/[0.10] transition-colors cursor-pointer disabled:opacity-40"
          >
            <RotateCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>

          <span className="text-[11px] font-mono text-[#94a3b8] absolute left-1/2 -translate-x-1/2">
            最后刷新 {lastRefreshedTime}
          </span>

          <div className="flex items-center gap-1.5">
            <button
              onClick={onOpenSettings}
              title="偏好设置与密钥管理"
              className="w-[28px] h-[28px] rounded-full bg-white/[0.05] border border-white/[0.14] flex items-center justify-center text-[#cbd5e1] hover:bg-white/[0.10] transition-colors cursor-pointer"
            >
              <Settings className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => {
                setPowerActive(!powerActive);
                onTriggerNotification(
                  powerActive ? 'MyToken 监控已暂停' : 'MyToken 监控已唤醒',
                  powerActive ? '菜单栏与后台静默轮询已挂起。' : '正在恢复菜单栏指标与本机凭证连接。'
                );
              }}
              title={powerActive ? '退出 MyToken' : '唤醒监控'}
              className="w-[28px] h-[28px] rounded-full bg-white/[0.05] border border-white/[0.14] flex items-center justify-center text-[#cbd5e1] hover:text-[#ef4444] hover:bg-white/[0.10] transition-colors cursor-pointer"
            >
              <Power className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
