import React, { useState } from 'react';
import { motion } from 'motion/react';
import { 
  User, Shield, Sparkles, AlertTriangle, Eye, Edit2, CheckCircle2, 
  TrendingUp, Award, Award as BadgeIcon, Save, Key, Clock
} from 'lucide-react';
import { UserProfile } from '../types';

interface ProfileProps {
  user: UserProfile;
  onUpdate: (updatedUser: UserProfile) => void;
  isDarkMode: boolean;
}

export default function ProfileView({ user, onUpdate, isDarkMode }: ProfileProps) {
  const [name, setName] = useState(user.name);
  const [username, setUsername] = useState(user.username);
  const [bio, setBio] = useState(user.bio);
  const [customStatus, setCustomStatus] = useState(user.customStatus || '');
  const [status, setStatus] = useState(user.status);
  const [isEditing, setIsEditing] = useState(false);

  const handleSave = () => {
    onUpdate({
      ...user,
      name,
      username,
      bio,
      customStatus,
      status,
    });
    setIsEditing(false);
  };

  return (
    <div id="profile_container" className="flex flex-col h-full w-full overflow-y-auto pb-6 scrollbar-thin">
      
      {/* Immersive Discord style profile banner */}
      <div className={`h-28 w-full relative shrink-0 ${
        isDarkMode ? 'bg-gradient-to-tr from-elegant-gold-dark via-[#1a1714] to-elegant-bg' : 'bg-gradient-to-tr from-[#dfd5c6] to-white'
      }`}>
        <div className="absolute inset-0 bg-slate-950/20" />
        
        {/* Absolute floating quick stats */}
        <div className={`absolute top-4 right-4 border px-2.5 py-1 rounded-full text-[9px] font-mono font-bold backdrop-blur-sm ${
          isDarkMode ? 'bg-elegant-bg border-elegant-gold/20 text-elegant-gold' : 'bg-white border-[#dfd5c6] text-elegant-gold-dark'
        }`}>
          ⚡ System Core Owner
        </div>
      </div>

      {/* Avatar overlap row */}
      <div className="px-6 -mt-10 mb-2 relative flex items-end justify-between">
        {/* Large Avatar container */}
        <div className="relative">
          <div className={`w-20 h-20 rounded-3xl border-4 overflow-hidden flex items-center justify-center text-3xl font-extrabold text-white shadow-xl relative select-none ${
            isDarkMode ? 'bg-[#151210] border-elegant-bg' : 'bg-white border-[#faf9f5]'
          }`}>
            {user.avatar}
          </div>
          {/* Presence State Circle Overlay */}
          <span className={`absolute bottom-0.5 right-0.5 w-5 h-5 rounded-full border-4 ${
            isDarkMode ? 'border-elegant-bg' : 'border-[#faf9f5]'
          } ${
            status === 'online' ? 'bg-emerald-500' :
            status === 'idle' ? 'bg-amber-400' :
            status === 'dnd' ? 'bg-rose-500' : 'bg-slate-400'
          }`} />
        </div>

        {/* Edit profile tactile trigger button */}
        {!isEditing ? (
          <button
            id="btn_edit_profile_toggle"
            onClick={() => setIsEditing(true)}
            className={`px-4 py-2 rounded-xl text-xs font-mono font-bold border transition-colors cursor-pointer ${
              isDarkMode ? 'bg-elegant-card border-elegant-border/10 text-slate-300 hover:bg-[#1a1613]' : 'bg-white border-[#dfd5c6] text-slate-700 hover:bg-[#faf9f5] shadow-sm'
            }`}
          >
            Edit Bio State
          </button>
        ) : (
          <button
            id="btn_save_profile_state"
            onClick={handleSave}
            className="px-4 py-2 bg-gradient-to-tr from-elegant-gold-dark to-elegant-gold text-elegant-bg rounded-xl text-xs font-mono font-bold flex items-center gap-1 cursor-pointer hover:brightness-110 shadow-sm"
          >
            <Save className="w-3.5 h-3.5" /> Save Changes
          </button>
        )}
      </div>

      {/* Profile Details Container */}
      <div className="px-6 mt-3 space-y-5">
        
        {/* Name and Discord username header */}
        <div>
          {!isEditing ? (
            <>
              <div className="flex items-center gap-1.5">
                <h3 className={`text-lg font-bold leading-tight ${isDarkMode ? 'text-white' : 'text-[#3c3730]'}`}>
                  {user.name}
                </h3>
                <CheckCircle2 className={`w-4.5 h-4.5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} title="Verified Nexus Account" />
              </div>
              <span className="text-xs text-slate-400 font-mono tracking-tight">{user.username}</span>
            </>
          ) : (
            <div className="space-y-2">
              <input
                id="edit_profile_name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className={`w-full border rounded-xl p-2 text-xs font-semibold outline-none ${
                  isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100 placeholder-slate-600 focus:ring-1 focus:ring-elegant-gold/45' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                }`}
                placeholder="Display Name"
              />
              <input
                id="edit_profile_username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className={`w-full border rounded-xl p-2 text-xs font-mono outline-none ${
                  isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-elegant-gold focus:ring-1 focus:ring-elegant-gold/45' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                }`}
                placeholder="Username Selector"
              />
            </div>
          )}
        </div>

        {/* Custom Status Bar (Telegram-style custom subtitle) */}
        <div className={`p-3 rounded-2xl border ${
          isDarkMode ? 'bg-elegant-card border-elegant-border/10' : 'bg-[#fbfaf6] border-[#dfd5c6]/45'
        }`}>
          <span className="text-[9px] font-mono text-slate-400 uppercase tracking-widest block mb-1">
            Current Status Key
          </span>
          {!isEditing ? (
            <p className="text-xs text-slate-300 italic font-medium flex items-center gap-1.5">
              {user.customStatus || "No custom status configured"}
            </p>
          ) : (
            <input
              id="edit_profile_status"
              type="text"
              value={customStatus}
              onChange={(e) => setCustomStatus(e.target.value)}
              className={`w-full border rounded-xl p-2 text-xs outline-none ${
                isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100 placeholder-slate-600 focus:ring-1 focus:ring-elegant-gold/45' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
              }`}
              placeholder="What are you up to right now?"
            />
          )}

          {/* Quick Status Presence select widgets if editing */}
          {isEditing && (
            <div className="mt-3 flex gap-2">
              {(['online', 'idle', 'dnd', 'offline'] as const).map((pr) => (
                <button
                  key={pr}
                  id={`btn_presence_${pr}`}
                  type="button"
                  onClick={() => setStatus(pr)}
                  className={`px-3 py-1.5 rounded-full text-[9px] font-mono border capitalize transition-all cursor-pointer ${
                    status === pr
                      ? 'bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg border-transparent font-bold'
                      : isDarkMode
                      ? 'bg-transparent text-slate-400 border-elegant-border/20 hover:text-white'
                      : 'bg-transparent text-[#5c5346] border-[#dfd5c6] hover:bg-[#faf9f5]'
                  }`}
                >
                  {pr}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Bio segment */}
        <div>
          <span className="text-[9px] font-mono text-slate-400 uppercase tracking-widest block mb-1">
            Biographic Core
          </span>
          {!isEditing ? (
            <p className={`text-xs leading-relaxed ${isDarkMode ? 'text-slate-300' : 'text-slate-650'}`}>{user.bio}</p>
          ) : (
            <textarea
              id="edit_profile_bio"
              rows={3}
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              className={`w-full border rounded-xl p-2.5 text-xs resize-none leading-relaxed outline-none ${
                isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100 focus:ring-1 focus:ring-elegant-gold/45' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
              }`}
              placeholder="Tell the community about your expertise and coordinates..."
            />
          )}
        </div>

        {/* Discord/Telegram style badge shelf */}
        <div>
          <span className="text-[9px] font-mono text-slate-400 uppercase tracking-widest block mb-2">
            Tactical Badges & Achievements
          </span>
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: 'First Responder', icon: '🚑', desc: 'Active Emergency Unit', color: 'border-rose-500/10 hover:border-rose-500/20 text-rose-400' },
              { label: 'Key Verified', icon: '🔑', desc: 'Secure Encryption Set', color: 'border-elegant-gold/20 hover:border-elegant-gold/40 text-elegant-gold' },
              { label: 'Aid Pillar', icon: '🌟', desc: '15+ Resolved Tickets', color: 'border-amber-500/10 hover:border-amber-500/20 text-amber-400' },
              { label: 'Sys Admin', icon: '⚙️', desc: 'Platform Supervisor', color: 'border-blue-500/10 hover:border-blue-500/20 text-blue-400' },
            ].map((badge, idx) => (
              <div
                key={idx}
                className={`p-3 rounded-2xl border flex flex-col gap-1 items-start text-xs transition-colors bg-elegant-card/30 ${badge.color}`}
              >
                <div className="flex items-center gap-1.5 font-bold">
                  <span className="text-sm select-none">{badge.icon}</span>
                  <span>{badge.label}</span>
                </div>
                <span className="text-[9px] text-slate-400 leading-none">{badge.desc}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Operational statistics */}
        <div className={`p-4 rounded-3xl border ${
          isDarkMode ? 'bg-[#000000]/20 border-elegant-border/15 shadow-2xl shadow-elegant-gold/5' : 'bg-[#f5f2eb] border-[#dfd5c6]/45'
        }`}>
          <div className="flex justify-between items-baseline mb-3">
            <span className="text-[9.5px] font-mono text-slate-405 uppercase tracking-wider text-slate-400">Operational Mileage</span>
            <span className={`text-[10px] font-mono font-bold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>Top 5% Network Responders</span>
          </div>
          
          <div className="grid grid-cols-3 gap-2 text-center">
            <div className={`p-2.5 rounded-xl border ${isDarkMode ? 'bg-[#151210]/40 border-elegant-border/10' : 'bg-white border-[#dfd5c6]'}`}>
              <span className={`block text-lg font-bold font-mono ${isDarkMode ? 'text-white' : 'text-[#3c3730]'}`}>42</span>
              <span className="text-[8.5px] text-slate-500 uppercase tracking-tight font-mono">Dispatches</span>
            </div>
            <div className={`p-2.5 rounded-xl border ${isDarkMode ? 'bg-[#151210]/40 border-elegant-border/10' : 'bg-white border-[#dfd5c6]'}`}>
              <span className={`block text-lg font-bold font-mono ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>12</span>
              <span className="text-[8.5px] text-slate-500 uppercase tracking-tight font-mono">Vetted Cleared</span>
            </div>
            <div className={`p-2.5 rounded-xl border ${isDarkMode ? 'bg-[#151210]/40 border-elegant-border/10' : 'bg-white border-[#dfd5c6]'}`}>
              <span className={`block text-lg font-bold font-mono ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>920h</span>
              <span className="text-[8.5px] text-slate-500 uppercase tracking-tight font-mono">Uptime Connected</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
