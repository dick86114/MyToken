import React, { useState } from 'react';
import { INITIAL_PROVIDERS } from './data/mockData';
import { ProviderItem } from './types';
import { Header } from './components/Header';
import { HeroSection } from './components/HeroSection';
import { CapabilitiesSection } from './components/CapabilitiesSection';
import { ProviderMatrixSection } from './components/ProviderMatrixSection';
import { SecuritySection } from './components/SecuritySection';
import { InstallSection } from './components/InstallSection';
import { FaqSection } from './components/FaqSection';
import { FinalCtaSection } from './components/FinalCtaSection';
import { Footer } from './components/Footer';
import { GatekeeperModal } from './components/GatekeeperModal';
import { SettingsModal } from './components/SettingsModal';
import { NotificationBanner } from './components/NotificationBanner';

export function App() {
  const [providers, setProviders] = useState<ProviderItem[]>(INITIAL_PROVIDERS);
  const [gatekeeperModalOpen, setGatekeeperModalOpen] = useState(false);
  const [settingsModalOpen, setSettingsModalOpen] = useState(false);
  const [activeNotification, setActiveNotification] = useState<{
    id: number;
    title: string;
    body: string;
  } | null>(null);

  const triggerNotification = (title: string, body: string) => {
    setActiveNotification({
      id: Date.now(),
      title,
      body,
    });
  };

  return (
    <div className="min-h-screen bg-[#0f131c] text-[#dfe2ee] font-sans antialiased selection:bg-[#438fff] selection:text-[#002959] relative flex flex-col">
      {/* Top Fixed Header */}
      <Header
        onOpenSettings={() => setSettingsModalOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 pt-16 flex flex-col">
        {/* Section 1: Hero & macOS Menu Bar Simulator */}
        <HeroSection
          providers={providers}
          onOpenSettings={() => setSettingsModalOpen(true)}
          onTriggerNotification={triggerNotification}
        />

        {/* Section 2: 核心能力 (Native Capabilities) */}
        <CapabilitiesSection onTriggerNotification={triggerNotification} />

        {/* Section 3: 供应商矩阵 (Provider Matrix) */}
        <ProviderMatrixSection />

        {/* Section 4: 安全与隐私 (Local First Architecture) */}
        <SecuritySection />

        {/* Section 5: 安装使用 (Installation & Gatekeeper) */}
        <InstallSection
          onOpenGatekeeperModal={() => setGatekeeperModalOpen(true)}
        />

        {/* Section 6: 常见问题 (FAQ) */}
        <FaqSection />

        {/* Section 7: Final Download CTA */}
        <FinalCtaSection />
      </main>

      {/* Footer */}
      <Footer />

      {/* Interactive Modals */}

      <GatekeeperModal
        isOpen={gatekeeperModalOpen}
        onClose={() => setGatekeeperModalOpen(false)}
      />

      <SettingsModal
        isOpen={settingsModalOpen}
        onClose={() => setSettingsModalOpen(false)}
        providers={providers}
        onUpdateProviders={setProviders}
        onTriggerNotification={triggerNotification}
      />

      {/* macOS System Alert Banner */}
      <NotificationBanner
        notification={activeNotification}
        onDismiss={() => setActiveNotification(null)}
      />
    </div>
  );
}

export default App;
