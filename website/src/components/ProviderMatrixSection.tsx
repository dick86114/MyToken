import React, { useState } from 'react';
import { MATRIX_DATA } from '../data/mockData';
import { MatrixRow } from '../types';
import {
  Search,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  Terminal,
  Check,
  Radio,
} from 'lucide-react';

export const ProviderMatrixSection: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedRow, setExpandedRow] = useState<string | null>(null);
  const [copiedEndpoint, setCopiedEndpoint] = useState<string | null>(null);

  const filteredData = MATRIX_DATA.filter(
    (item) =>
      item.provider.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.metrics.some((m) => m.toLowerCase().includes(searchTerm.toLowerCase())) ||
      item.exclusiveFeatures.some((f) => f.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleCopy = (endpoint: string, e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(endpoint);
    setCopiedEndpoint(endpoint);
    setTimeout(() => setCopiedEndpoint(null), 2000);
  };

  return (
    <section id="providers" className="w-full py-16 lg:py-24 bg-[#0a0e16]/80 relative">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-10 gap-6">
          <div className="flex flex-col max-w-2xl">
            <span className="font-mono text-xs text-[#38BDF8] uppercase tracking-widest mb-2 font-semibold">
              Provider Matrix
            </span>
            <h2 className="font-headline-section text-[#F8FAFC] tracking-tight">
              内置六大供应商矩阵与深度适配
            </h2>
            <p className="font-body-large text-[#c1c6d7] mt-3">
              针对主流 AI 基础模型服务与中转协议定制解析规则，同时展示套餐周期、剩余额度和账户状态。
            </p>
          </div>

          {/* Quick Search */}
          <div className="relative w-full md:w-64">
            <Search className="w-4 h-4 text-[#94A3B8] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              placeholder="搜索指标或供应商..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 rounded-xl bg-[#181c24] border border-white/10 text-xs sm:text-sm text-white placeholder-[#64748B] focus:outline-none focus:border-[#38bdf8]"
            />
          </div>
        </div>

        {/* Capability Matrix Table */}
        <div className="w-full overflow-x-auto rounded-2xl border border-white/[0.08] shadow-2xl bg-[#181c24]/90 backdrop-blur-md">
          <table className="w-full text-left min-w-[800px] border-collapse">
            <thead>
              <tr className="bg-[#262a33]/90 text-[#94A3B8] font-mono text-xs tracking-wider uppercase border-b border-white/[0.08]">
                <th className="py-4 px-6">供应商 (Provider)</th>
                <th className="py-4 px-6">凭证类型 (Credential)</th>
                <th className="py-4 px-6">监控维度 (Metrics Tracked)</th>
                <th className="py-4 px-6">专属能力与亮点 (Exclusive Features)</th>
                <th className="py-4 px-4 text-center">详情</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.06] text-sm text-[#dfe2ee]">
              {filteredData.map((row) => {
                const isExpanded = expandedRow === row.code;

                return (
                  <React.Fragment key={row.code}>
                    <tr
                      onClick={() => setExpandedRow(isExpanded ? null : row.code)}
                      className={`hover:bg-[#1c2028] transition-colors cursor-pointer ${
                        isExpanded ? 'bg-[#1c2028]' : ''
                      }`}
                    >
                      {/* Provider Col */}
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-2.5">
                          <span
                            className="px-2 py-0.5 rounded font-mono text-xs font-bold"
                            style={{
                              backgroundColor: `${row.color}18`,
                              color: row.color,
                            }}
                          >
                            {row.code}
                          </span>
                          <span className="font-semibold text-[#F8FAFC] text-base">
                            {row.provider}
                          </span>
                        </div>
                      </td>

                      {/* Credential Type Col */}
                      <td className="py-4 px-6 font-mono text-xs text-[#94A3B8]">
                        <span className="px-2 py-1 rounded bg-[#0a0e16] border border-white/5 inline-block">
                          {row.credential}
                        </span>
                      </td>

                      {/* Metrics Tracked Col */}
                      <td className="py-4 px-6 text-[#c1c6d7]">
                        <div className="flex flex-wrap gap-1.5 max-w-xs">
                          {row.metrics.map((m, idx) => (
                            <span
                              key={idx}
                              className="text-xs bg-white/[0.04] px-2 py-0.5 rounded text-[#dfe2ee]"
                            >
                              {m}
                            </span>
                          ))}
                        </div>
                      </td>

                      {/* Exclusive Features Col */}
                      <td className="py-4 px-6">
                        <div className="flex flex-col gap-1 max-w-sm">
                          {row.exclusiveFeatures.map((feat, idx) => (
                            <div key={idx} className="flex items-start gap-1.5 text-xs text-[#c1c6d7]">
                              <span
                                className="w-1.5 h-1.5 rounded-full mt-1.5 shrink-0"
                                style={{ backgroundColor: row.color }}
                              />
                              <span
                                className={
                                  feat.includes('免存密码') ||
                                  feat.includes('真实健康度') ||
                                  feat.includes('专项周期') ||
                                  feat.includes('多 Region') ||
                                  feat.includes('广泛兼容')
                                    ? 'text-[#F8FAFC] font-medium'
                                    : ''
                                }
                              >
                                {feat}
                              </span>
                            </div>
                          ))}
                        </div>
                      </td>

                      {/* Toggle Expander */}
                      <td className="py-4 px-4 text-center">
                        <button
                          className="p-1.5 rounded-lg hover:bg-white/10 text-[#94A3B8] transition-colors"
                          title="查看 API 端点与直连范例"
                        >
                          {isExpanded ? (
                            <ChevronUp className="w-4 h-4 text-[#abc7ff]" />
                          ) : (
                            <ChevronDown className="w-4 h-4" />
                          )}
                        </button>
                      </td>
                    </tr>

                    {/* Expanded Detail Panel */}
                    {isExpanded && (
                      <tr className="bg-[#10141e] border-b border-white/[0.06]">
                        <td colSpan={5} className="py-4 px-6">
                          <div className="p-4 rounded-xl bg-[#0a0e16] border border-white/5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                            <div className="flex flex-col gap-1">
                              <div className="flex items-center gap-2">
                                <Terminal className="w-4 h-4 text-[#38BDF8]" />
                                <span className="font-mono text-xs text-[#F8FAFC] font-semibold">
                                  官方直连校验端点（不经过任何代理中转）:
                                </span>
                              </div>
                              <div className="font-mono text-xs text-[#94A3B8] break-all bg-[#1c2028] px-3 py-1.5 rounded-lg border border-white/5">
                                GET {row.sampleEndpoint}
                              </div>
                            </div>

                            <div className="flex items-center gap-3 shrink-0">
                              <button
                                onClick={(e) => handleCopy(row.sampleEndpoint, e)}
                                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#262a33] hover:bg-[#353942] text-xs font-mono text-[#dfe2ee] transition-colors border border-white/5 cursor-pointer"
                              >
                                {copiedEndpoint === row.sampleEndpoint ? (
                                  <>
                                    <Check className="w-3.5 h-3.5 text-[#10B981]" />
                                    <span className="text-[#10B981]">已复制 URL</span>
                                  </>
                                ) : (
                                  <span>复制端点</span>
                                )}
                              </button>

                              <a
                                href={row.officialDocUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center gap-1 text-xs text-[#7bd0ff] hover:underline"
                              >
                                <span>官方控制台</span>
                                <ExternalLink className="w-3 h-3" />
                              </a>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
};
