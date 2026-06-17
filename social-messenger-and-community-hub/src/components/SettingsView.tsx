import React from 'react';
import { motion } from 'motion/react';
import { 
  Sun, Moon, Shield, Volume2, VolumeX, Eye, Bell, 
  MapPin, HelpCircle, Landmark, LogOut, Sparkles, FileText
} from 'lucide-react';
import { AppSettings } from '../types';

interface SettingsProps {
  settings: AppSettings;
  onUpdateSettings: (settings: AppSettings) => void;
  onLogout: () => void;
  isDarkMode: boolean;
}

export default function SettingsView({ settings, onUpdateSettings, onLogout, isDarkMode }: SettingsProps) {
  
  const toggleTheme = () => {
    onUpdateSettings({
      ...settings,
      theme: settings.theme === 'dark' ? 'light' : 'dark',
    });
  };

  const setFontSize = (size: 'small' | 'medium' | 'large') => {
    onUpdateSettings({
      ...settings,
      accessibilityTextSize: size,
    });
  };

  const handleToggle = (key: keyof AppSettings) => {
    onUpdateSettings({
      ...settings,
      [key]: !settings[key],
    });
  };

  return (
    <div id="settings_container" className="flex flex-col h-full w-full overflow-y-auto pb-6 scrollbar-thin">
      
      {/* Settings Sections Group */}
      <div className="p-5 space-y-6">
        
        {/* Core Theme Toggle Mode Card */}
        <div>
          <span className="text-[9.5px] font-mono text-slate-400 uppercase tracking-widest block mb-1.5">
            Interface Theme mode
          </span>
          <div className={`p-4 rounded-3xl border flex items-center justify-between transition-colors ${
            isDarkMode ? 'bg-elegant-card border-elegant-border/10' : 'bg-white border-[#dfd5c6]/45 shadow-sm'
          }`}>
            <div className="flex items-center gap-3">
              <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDarkMode ? 'bg-elegant-gold/10 text-elegant-gold' : 'bg-[#dfd5c6]/30 text-elegant-gold-dark'
              }`}>
                {isDarkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
              </div>
              <div className="flex flex-col">
                <span className={`text-[12.5px] font-bold ${isDarkMode ? 'text-white' : 'text-[#3c3730]'}`}>
                  {isDarkMode ? 'Light Mode theme' : 'Immersive Dark Mode'}
                </span>
                <span className="text-[10px] text-slate-400 font-mono">Changes background shades instantly</span>
              </div>
            </div>

            {/* Tactile Switch button */}
            <button
              id="btn_toggle_theme_mode"
              onClick={toggleTheme}
              className="bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold text-[10px] font-mono px-3.5 py-1.5 rounded-xl cursor-pointer shadow-md transition-all active:scale-95 text-center hover:brightness-110"
            >
              Toggle Mode
            </button>
          </div>
        </div>

        {/* Accessibility Layout size selectors */}
        <div>
          <span className="text-[9.5px] font-mono text-slate-400 uppercase tracking-widest block mb-2">
            Accessibility Text Size Scaling
          </span>
          <div className={`p-4 rounded-3xl border ${
            isDarkMode ? 'bg-elegant-card border-elegant-border/10' : 'bg-white border-[#dfd5c6]/45 shadow-sm'
          }`}>
            <p className="text-[10px] text-slate-404 leading-relaxed font-mono mb-3 text-slate-400">
              Scales display headings and chat dialogues spacing for better field readability.
            </p>
            <div className="grid grid-cols-3 gap-2">
              {(['small', 'medium', 'large'] as const).map((sz) => {
                const isSelected = settings.accessibilityTextSize === sz;
                return (
                  <button
                    key={sz}
                    id={`btn_set_fontsize_${sz}`}
                    onClick={() => setFontSize(sz)}
                    className={`px-3 py-2.5 rounded-xl text-xs font-mono font-bold border capitalize transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg border-transparent shadow'
                        : isDarkMode
                        ? 'bg-elegant-bg border-elegant-border/10 text-slate-404 hover:text-white hover:bg-[#1a1613]'
                        : 'bg-[#f5f2eb] border-[#dfd5c6] text-[#5c5346] hover:bg-[#faf9f5]'
                    }`}
                  >
                    {sz === 'small' ? 'Compact' : sz === 'medium' ? 'Regular' : 'Enlarged'}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Device & Hardware Integration Switches */}
        <div>
          <span className="text-[9.5px] font-mono text-slate-400 uppercase tracking-widest block mb-2">
            Nexus System Hardware integration
          </span>
          <div className={`rounded-3xl border divide-y overflow-hidden ${
            isDarkMode ? 'bg-elegant-card border-elegant-border/10 divide-elegant-border/5' : 'bg-white border-[#dfd5c6]/45 shadow-sm divide-[#dfd5c6]/35'
          }`}>
            {[
              { 
                key: 'notificationsEnabled', 
                label: 'Direct Push Notifications', 
                desc: 'Delivers immediate sound / overlay banners.', 
                icon: <Bell className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} /> 
              },
              { 
                key: 'soundEffects', 
                label: 'Audio Tones & Ringers', 
                desc: 'Simulates Telegram message chime and call alerts.', 
                icon: <Volume2 className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} /> 
              },
              { 
                key: 'hapticFeedback', 
                label: 'Android Tactile Vibrate feedback', 
                desc: 'Pulsates on keyboard taps & call connects.', 
                icon: <Sparkles className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} /> 
              },
              { 
                key: 'locationSharing', 
                label: 'Precise GPS Pin Sharing', 
                desc: 'Optimizes nearby radar grid with active location coordinate.', 
                icon: <MapPin className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} /> 
              },
            ].map((sw) => (
              <div key={sw.key} className="p-3.5 flex items-center justify-between transition-colors hover:bg-slate-550/5">
                <div className="flex gap-3 items-start p-0.5">
                  <div className="mt-0.5">{sw.icon}</div>
                  <div className="flex flex-col">
                    <span className={`text-[12px] font-bold ${isDarkMode ? 'text-white' : 'text-[#3c3730]'}`}>{sw.label}</span>
                    <span className="text-[10px] text-slate-400 font-mono tracking-tight leading-normal max-w-[180px]">{sw.desc}</span>
                  </div>
                </div>

                {/* Tactile Apple/Android stylized tick box toggle */}
                <button
                  id={`btn_toggle_setting_${sw.key}`}
                  onClick={() => handleToggle(sw.key as any)}
                  className={`w-11 h-6 rounded-full p-1 relative transition-colors cursor-pointer ${
                    settings[sw.key as keyof AppSettings] 
                      ? (isDarkMode ? 'bg-elegant-gold' : 'bg-elegant-gold-dark')
                      : (isDarkMode ? 'bg-[#1e1a17]' : 'bg-[#e5dfd3]')
                  }`}
                >
                  <motion.div 
                    layout
                    className={`w-4 h-4 rounded-full ${isDarkMode ? 'bg-elegant-bg' : 'bg-white'}`}
                    animate={{ x: settings[sw.key as keyof AppSettings] ? 20 : 0 }}
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Security / System status */}
        <div className={`p-4 rounded-3xl border flex flex-col gap-2.5 ${
          isDarkMode ? 'bg-[#000000]/20 border-elegant-border/10' : 'bg-[#f5f2eb] border-[#dfd5c6]/45'
        }`}>
          <span className="text-[9px] font-mono text-slate-400 uppercase tracking-widest leading-none block">
            Cryptographic Node Info
          </span>
          <div className="flex justify-between text-[10.5px] font-mono text-slate-500">
            <span>Core Version:</span>
            <span className={isDarkMode ? 'text-elegant-gold font-bold' : 'text-elegant-gold-dark font-bold'}>v3.5 Premium Android-First SPA</span>
          </div>
          <div className="flex justify-between text-[10.5px] font-mono text-slate-500">
            <span>Sandboxed State:</span>
            <span className={isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}>Offline Local Persistence</span>
          </div>
        </div>

        {/* Logout session action */}
        <div className="pt-2">
          <button
            id="btn_logout_action"
            onClick={() => {
              if (confirm('Disconnect secure session keys and return to entry Splash?')) {
                onLogout();
              }
            }}
            className="w-full bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 hover:text-[#faf9f4] border border-rose-500/25 py-3.5 rounded-2xl text-xs font-mono font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow"
          >
            <LogOut className="w-4 h-4 animate-pulse" /> Disconnect Security Keys
          </button>
        </div>

      </div>
    </div>
  );
}
