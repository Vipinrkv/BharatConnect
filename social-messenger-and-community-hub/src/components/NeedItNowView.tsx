import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  ShieldAlert, Radio, Flame, Zap, Droplet, UserCheck, 
  MapPin, Check, ExternalLink, Loader, Activity
} from 'lucide-react';

interface UrgentIncident {
  id: string;
  incidentType: 'fire' | 'flooding' | 'power_down' | 'trapped_pet';
  location: string;
  title: string;
  minutesRemaining: number;
  allocatedTeam: string;
  backupsConfirmed: number;
}

export default function NeedItNowView({ isDarkMode }: { isDarkMode: boolean }) {
  const [incidents, setIncidents] = useState<UrgentIncident[]>([
    {
      id: 'inc_1',
      incidentType: 'fire',
      location: 'Warehouse sector 3 (Near Grid A)',
      title: 'Power Line sparking near dry grass fields',
      minutesRemaining: 12,
      allocatedTeam: 'Red Fire Volts Grid',
      backupsConfirmed: 4,
    },
    {
      id: 'inc_2',
      incidentType: 'flooding',
      location: 'Lower Subway Sector 5 exit path',
      title: 'Active basement seepage entering elderly basement corridor',
      minutesRemaining: 24,
      allocatedTeam: 'Regional Drainage Volts Group',
      backupsConfirmed: 8,
    },
    {
      id: 'inc_3',
      incidentType: 'power_down',
      location: 'Block C Medical Storage Center',
      title: 'Back up Generator fuel reserves entering red line critical zone',
      minutesRemaining: 5,
      allocatedTeam: 'Zone C Infrastructure Tech Grid',
      backupsConfirmed: 2,
    }
  ]);

  // Live real-time tick down effect
  useEffect(() => {
    const interval = setInterval(() => {
      setIncidents((current) =>
        current.map((inc) => ({
          ...inc,
          minutesRemaining: Math.max(0, inc.minutesRemaining - 1),
        }))
      );
    }, 45000); // Ticks every 45 secs for aesthetic progress

    return () => clearInterval(interval);
  }, []);

  const handleTransmitBackup = (id: string) => {
    setIncidents((current) =>
      current.map((inc) => {
        if (inc.id === id) {
          return {
            ...inc,
            backupsConfirmed: inc.backupsConfirmed + 1,
            allocatedTeam: 'Nexus Volunteers Grid (Joined!)'
          };
        }
        return inc;
      })
    );
    alert('🚨 Transmitting GPS beacon... Emergency backup grid updated with your location coordinates.');
  };

  const getTypeStyle = (type: string) => {
    switch (type) {
      case 'fire':
        return { bg: 'bg-rose-500/10 text-rose-400 border border-rose-500/20', icon: '🔥', label: 'Fire Hazard' };
      case 'flooding':
        return { bg: 'bg-blue-500/10 text-blue-400 border border-blue-500/20', icon: '🌊', label: 'Flooding' };
      case 'power_down':
        return { bg: 'bg-amber-500/10 text-amber-400 border border-amber-500/20', icon: '⚡', label: 'Power Grid Tripped' };
      default:
        return { bg: 'bg-red-500/10 text-red-400 border border-red-500/20', icon: '⚠️', label: 'Emergency Incident' };
    }
  };

  return (
    <div id="need_now_container" className="flex flex-col h-full w-full">
      
      {/* Flashing Civil Broadcast Header */}
      <div className={`p-4 shrink-0 relative overflow-hidden flex items-center justify-between border-b ${
        isDarkMode ? 'bg-gradient-to-r from-[#1c1214] via-[#2f1c1f] to-[#1c1214] border-elegant-border/20' : 'bg-gradient-to-r from-[#faf8f4] to-[#f3ebd9] border-[#dfd5c6]'
      }`}>
        <div className="absolute inset-0 bg-rose-500/[0.02] animate-pulse" />
        <div className="flex items-center gap-2.5 z-10">
          <div className={`w-5 h-5 rounded-full flex items-center justify-center animate-bounce ${
            isDarkMode ? 'bg-elegant-gold/10' : 'bg-[#dfd5c6]'
          }`}>
            <Radio className={`w-3.5 h-3.5 animate-pulse ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
          </div>
          <div>
            <h3 className={`text-[11.5px] font-bold font-mono tracking-widest uppercase ${
              isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'
            }`}>
              Civil Dispatch Core Network
            </h3>
            <span className={`text-[8.5px] font-mono ${isDarkMode ? 'text-slate-400' : 'text-[#8c7e6c]'}`}>LIVE FEEDBACK SYSTEM • PERSISTENT BACKUPS ACTIVE</span>
          </div>
        </div>
        <div className={`z-10 px-2 py-0.5 rounded text-[8.5px] font-mono tracking-wide font-extrabold uppercase animate-pulse border ${
          isDarkMode 
            ? 'bg-elegant-gold/10 border-elegant-gold/20 text-elegant-gold' 
            : 'bg-[#dfd5c6]/20 border-elegant-gold-dark/20 text-elegant-gold-dark'
        }`}>
          Siren v3
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-thin">
        {incidents.map((inc) => {
          const style = getTypeStyle(inc.incidentType);
          return (
            <motion.div
              key={inc.id}
              initial={{ scale: 0.98, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ type: 'spring', stiffness: 200, damping: 15 }}
              className={`p-4 rounded-3xl border flex flex-col gap-3 transition-colors ${
                isDarkMode ? 'bg-elegant-card border-elegant-border/10 hover:border-elegant-gold/30' : 'bg-white border-[#dfd5c6]/45 shadow-sm'
              }`}
            >
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <span className="text-base select-none">{style.icon}</span>
                  <span className={`text-[9.5px] font-mono font-semibold tracking-wider uppercase px-2.5 py-0.5 rounded-full ${style.bg}`}>
                    {style.label}
                  </span>
                </div>

                {/* Remaining countdown helper style */}
                <div className="text-[10px] text-rose-550 font-mono font-bold flex items-center gap-1.5 animate-pulse bg-rose-500/10 px-2.5 py-0.5 rounded-full text-rose-500">
                  <Activity className="w-3 h-3 animate-spin" />
                  <span>~{inc.minutesRemaining}m left</span>
                </div>
              </div>

              {/* Title & Coordinates description */}
              <div>
                <h4 className={`text-xs font-extrabold tracking-tight leading-normal ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
                  {inc.title}
                </h4>
                <p className={`text-[10.5px] mt-1 flex items-center gap-1 font-mono ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
                  <MapPin className="w-3.5 h-3.5 text-rose-400" /> Location: <span className="text-rose-400 font-bold">{inc.location}</span>
                </p>
              </div>

              {/* Progress status of response grid */}
              <div className={`p-2.5 rounded-xl text-[10px] font-mono flex flex-col gap-1.5 border ${
                isDarkMode ? 'bg-elegant-bg border-elegant-border/10' : 'bg-[#f5f2eb] border-[#dfd5c6]/40'
              }`}>
                <div className="flex justify-between text-slate-400">
                  <span>Authorized Unit:</span>
                  <span className={`font-bold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>{inc.allocatedTeam}</span>
                </div>
                <div className={`w-full rounded-full h-1 overflow-hidden ${isDarkMode ? 'bg-elegant-card' : 'bg-white'}`}>
                  <div 
                    className="h-full bg-gradient-to-r from-elegant-gold-dark to-elegant-gold shadow-lg shadow-elegant-gold/50" 
                    style={{ width: `${Math.min(100, (inc.backupsConfirmed / 10) * 100)}%` }} 
                  />
                </div>
                <div className="flex justify-between text-[9px] text-slate-500 select-none">
                  <span>Required: 10 Responders</span>
                  <span>Currently {inc.backupsConfirmed} Dispatched</span>
                </div>
              </div>

              {/* Active emergency button */}
              <button
                id={`btn_join_backup_${inc.id}`}
                onClick={() => handleTransmitBackup(inc.id)}
                className="w-full bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold py-3 rounded-2xl text-[11px] font-mono flex items-center justify-center gap-1.5 hover:brightness-110 active:scale-95 transition-all shadow-md cursor-pointer"
              >
                <span>Transmit Emergency Backup Signal</span>
                <Radio className="w-4 h-4 animate-pulse" />
              </button>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
