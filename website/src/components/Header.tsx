import React, { useState, useEffect } from 'react';
import { Terminal, Menu, X } from 'lucide-react';

interface HeaderProps {
  onOpenDownload?: () => void;
  onOpenSettings?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onOpenSettings }) => {
  const [activeSection, setActiveSection] = useState('features');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);

      const sections = ['features', 'providers', 'privacy', 'install', 'faq'];
      const scrollPosition = window.scrollY + 120;

      for (const sectionId of sections) {
        const el = document.getElementById(sectionId);
        if (el) {
          const top = el.offsetTop;
          const height = el.offsetHeight;
          if (scrollPosition >= top && scrollPosition < top + height) {
            setActiveSection(sectionId);
            break;
          }
        }
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);


  const navItems = [
    { id: 'features', label: '核心功能' },
    { id: 'providers', label: '供应商矩阵' },
    { id: 'privacy', label: '安全与隐私' },
    { id: 'install', label: '安装使用' },
    { id: 'faq', label: '常见问题' },
  ];

  const scrollTo = (id: string) => {
    setMobileMenuOpen(false);
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
      setActiveSection(id);
    }
  };

  return (
    <header
      id="main-header"
      className={`fixed top-0 w-full z-50 transition-all duration-300 ${
        scrolled
          ? 'bg-[#0f131c]/90 backdrop-blur-xl border-b border-white/[0.06] shadow-[0_4px_24px_rgba(0,0,0,0.5)]'
          : 'bg-[#0f131c]/80 backdrop-blur-xl border-b border-transparent shadow-[0_1px_8px_rgba(0,0,0,0.4)]'
      }`}
    >
      <div className="h-16 max-w-[1200px] mx-auto px-4 sm:px-6 flex items-center justify-between gap-3">
        {/* Left: Brand & Logo */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
            className="flex items-center gap-2 text-left group"
          >
            <img
              alt="MyToken icon"
              className="h-8 w-8 object-contain transition-transform duration-200 group-hover:scale-105"
              src="/app-icon.png"
            />
            <span className="font-semibold text-lg text-[#F8FAFC] tracking-tight group-hover:text-[#abc7ff] transition-colors">
              MyToken
            </span>
            <span className="px-2 py-0.5 rounded-md bg-[#262a33] text-[#c1c6d7] font-mono text-[11px] font-medium border border-white/5">
              macOS 14+ · Android 10+
            </span>
          </button>
        </div>

        {/* Center: Desktop Navigation Bar */}
        <nav
          id="nav-container"
          className="hidden lg:flex items-center gap-1 px-1.5 py-1 rounded-xl bg-[#0a0e16]/60 backdrop-blur-md border border-white/[0.06]"
        >
          {navItems.map((item) => {
            const isActive = activeSection === item.id;
            return (
              <button
                key={item.id}
                id={`nav-link-${item.id}`}
                onClick={() => scrollTo(item.id)}
                className={`px-3 py-1 text-sm rounded-lg transition-all duration-200 cursor-pointer ${
                  isActive
                    ? 'bg-[#262a33] text-[#abc7ff] font-medium shadow-sm border border-white/5'
                    : 'text-[#94A3B8] hover:text-[#F8FAFC] hover:bg-white/[0.04]'
                }`}
              >
                {item.label}
              </button>
            );
          })}
        </nav>

        {/* Right: Actions */}
        <div className="flex items-center gap-2.5 shrink-0">
          {/* GitHub Button */}
          <a
            id="header-github-btn"
            href="https://github.com/dick86114/MyToken"
            target="_blank"
            rel="noopener noreferrer"
            title="打开 GitHub 仓库"
            className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#262a33] hover:bg-[#353942] text-[#dfe2ee] font-mono text-xs transition-colors border border-white/5 group cursor-pointer"
          >
            <Terminal className="w-3.5 h-3.5 text-[#94A3B8] group-hover:text-[#abc7ff]" />
            <span>GitHub</span>
          </a>

          {/* Mobile Menu Toggle */}
          <button
            id="mobile-menu-toggle"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="lg:hidden p-2 rounded-lg bg-[#262a33] text-[#c1c6d7] hover:text-white"
            aria-label="打开导航菜单"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="lg:hidden px-4 py-3 bg-[#0f131c]/98 border-b border-white/[0.08] backdrop-blur-2xl flex flex-col gap-1.5 animate-fadeIn">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => scrollTo(item.id)}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                activeSection === item.id
                  ? 'bg-[#262a33] text-[#abc7ff] font-medium'
                  : 'text-[#94A3B8] hover:text-white hover:bg-white/[0.04]'
              }`}
            >
              {item.label}
            </button>
          ))}
          <div className="pt-2 mt-1 border-t border-white/[0.06] flex items-center justify-between">
            <a
              href="https://github.com/dick86114/MyToken"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 text-xs text-[#94A3B8] hover:text-[#abc7ff]"
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>访问 GitHub 仓库</span>
            </a>
          </div>
        </div>
      )}
    </header>
  );
};
