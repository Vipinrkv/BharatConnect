import React, { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { MessageSquare, Shield, Zap, Compass } from 'lucide-react';

interface SplashProps {
  onComplete: () => void;
}

export default function SplashView({ onComplete }: SplashProps) {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setTimeout(onComplete, 600);
          return 100;
        }
        return prev + 5;
      });
    }, 80);

    return () => clearInterval(interval);
  }, [onComplete]);

  return (
    <div 
      id="splash_container" 
      className="flex flex-col items-center justify-between h-full w-full bg-elegant-bg text-white p-8 relative overflow-hidden"
    >
      {/* Decorative premium neon background glows */}
      <div className="absolute top-[-20%] left-[-20%] w-[80%] h-[80%] rounded-full bg-elegant-gold/8 blur-[100px] pointer-events-none" />
      <div className="absolute bottom-[-20%] right-[-20%] w-[80%] h-[80%] rounded-full bg-[#1b1e28]/20 blur-[100px] pointer-events-none" />

      {/* Top Brand Logo & Title */}
      <div className="flex-1 flex flex-col items-center justify-center z-10">
        <motion.div 
          initial={{ scale: 0.3, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", stiffness: 100, damping: 15 }}
          className="relative mb-6"
        >
          {/* Animated rings */}
          <div className="absolute inset-0 rounded-3xl bg-elegant-gold/20 blur-xl animate-pulse" />
          <div className="w-24 h-24 bg-gradient-to-tr from-[#9e835e] via-elegant-gold to-elegant-gold-light rounded-3xl flex items-center justify-center shadow-2xl relative border border-white/15">
            <MessageSquare className="w-12 h-12 text-elegant-bg" />
            <div className="absolute -top-1 -right-1 w-5 h-5 bg-elegant-gold-dark rounded-full flex items-center justify-center border-2 border-elegant-bg font-mono text-[10px] text-white font-extrabold">
              3
            </div>
          </div>
        </motion.div>

        <motion.h1 
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="text-3xl font-bold tracking-widest font-display bg-gradient-to-r from-elegant-gold-light via-elegant-gold to-[#9e835e] bg-clip-text text-transparent"
        >
          NEXUS
        </motion.h1>
        
        <motion.p 
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="text-[10px] text-elegant-gold/70 tracking-[0.2em] uppercase mt-2 font-mono"
        >
          Android-First Unified Core
        </motion.p>
      </div>

      {/* Features Showcase inside Splash */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.4 }}
        className="w-full max-w-sm grid grid-cols-3 gap-3 mb-12 z-10"
      >
        <div className="bg-elegant-card/45 backdrop-blur-md border border-elegant-border rounded-2xl p-3 flex flex-col items-center justify-center text-center">
          <Shield className="w-5 h-5 text-elegant-gold mb-1" />
          <span className="text-[10px] font-medium text-slate-300">Verified Help</span>
        </div>
        <div className="bg-elegant-card/45 backdrop-blur-md border border-elegant-border rounded-2xl p-3 flex flex-col items-center justify-center text-center">
          <Zap className="w-5 h-5 text-elegant-gold mb-1" />
          <span className="text-[10px] font-medium text-slate-300">Need It Now</span>
        </div>
        <div className="bg-elegant-card/45 backdrop-blur-md border border-elegant-border rounded-2xl p-3 flex flex-col items-center justify-center text-center">
          <Compass className="w-5 h-5 text-elegant-gold mb-1" />
          <span className="text-[10px] font-medium text-slate-300">Nearby Feed</span>
        </div>
      </motion.div>

      {/* Loader controls at bottom */}
      <div className="w-full max-w-xs flex flex-col items-center mb-6 z-10">
        <div className="w-full bg-elegant-card rounded-full h-1.5 overflow-hidden border border-elegant-border mb-3">
          <motion.div 
            className="h-full bg-gradient-to-r from-[#9e835e] to-elegant-gold"
            style={{ width: `${progress}%` }}
          />
        </div>
        <div className="flex justify-between w-full text-[11px] text-slate-400 font-mono">
          <span>Booting Services...</span>
          <span>{progress}%</span>
        </div>
      </div>

      <button 
        id="btn_skip_splash"
        onClick={onComplete}
        className="text-[11px] font-mono text-elegant-gold/70 hover:text-elegant-gold underline cursor-pointer pb-2 transition-colors z-10"
      >
        Tap to Skip Setup
      </button>

      {/* Bottom system indicators */}
      <div className="text-[9px] text-slate-500 font-mono text-center select-none">
        Inspired by WhatsApp, Telegram & Discord • Security v3.5
      </div>
    </div>
  );
}
