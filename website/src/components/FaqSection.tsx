import React, { useState } from 'react';
import { FAQS } from '../data/mockData';
import { HelpCircle, ChevronDown, ChevronUp } from 'lucide-react';

export const FaqSection: React.FC = () => {
  // All opened by default as shown in the screenshot, but allow collapsible toggling
  const [openMap, setOpenMap] = useState<Record<string, boolean>>({
    'faq-1': true,
    'faq-2': true,
    'faq-3': true,
    'faq-4': true,
    'faq-5': true,
  });

  const toggleFaq = (id: string) => {
    setOpenMap((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <section id="faq" className="w-full py-16 lg:py-24 relative">
      <div className="max-w-[1200px] mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="flex flex-col mb-12 max-w-2xl">
          <span className="font-mono text-xs text-[#7bd0ff] uppercase tracking-widest mb-2 font-semibold">
            Questions &amp; Answers
          </span>
          <h2 className="font-headline-section text-[#F8FAFC] tracking-tight">
            常见问题解答
          </h2>
          <p className="font-body-large text-[#c1c6d7] mt-3">
            有关架构安全、多 Key 机制与用量计算的真实解答。
          </p>
        </div>

        {/* Accordion Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
          {FAQS.map((faq) => {
            const isOpen = !!openMap[faq.id];

            return (
              <div
                key={faq.id}
                id={faq.id}
                className={`p-6 rounded-2xl bg-[#181c24]/90 border border-white/[0.06] shadow-sm flex flex-col gap-3 transition-all ${
                  faq.isWide ? 'md:col-span-2' : ''
                }`}
              >
                {/* Header Question */}
                <button
                  onClick={() => toggleFaq(faq.id)}
                  className="w-full flex items-start justify-between text-left gap-3 cursor-pointer group"
                >
                  <div className="flex items-center gap-2 text-[#abc7ff]">
                    <HelpCircle className="w-5 h-5 shrink-0 text-[#abc7ff] group-hover:scale-110 transition-transform" />
                    <h3 className="font-semibold text-base sm:text-lg text-[#F8FAFC] group-hover:text-[#abc7ff] transition-colors">
                      {faq.question}
                    </h3>
                  </div>

                  <span className="p-1 rounded-md text-[#94A3B8] group-hover:text-white transition-colors">
                    {isOpen ? (
                      <ChevronUp className="w-4 h-4" />
                    ) : (
                      <ChevronDown className="w-4 h-4" />
                    )}
                  </span>
                </button>

                {/* Body Answer */}
                {isOpen && (
                  <p className="text-sm sm:text-base text-[#c1c6d7] leading-relaxed pl-7 animate-fadeIn">
                    {faq.answer}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
