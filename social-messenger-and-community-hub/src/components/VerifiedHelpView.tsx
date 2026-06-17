import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Building2, ShieldCheck, AlertTriangle, Plus, Send, 
  HelpingHand, CheckCircle, Flame, HeartHandshake, Info
} from 'lucide-react';
import { HelpRequest, UserProfile } from '../types';

interface VerifiedHelpProps {
  initialRequests: HelpRequest[];
  currentUser: UserProfile;
  isDarkMode: boolean;
}

export default function VerifiedHelpView({ initialRequests, currentUser, isDarkMode }: VerifiedHelpProps) {
  const [requests, setRequests] = useState<HelpRequest[]>(initialRequests);
  const [showAddModal, setShowAddModal] = useState(false);

  // Form variables
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [category, setCategory] = useState<'medical' | 'food' | 'utility' | 'shelter' | 'rescue'>('medical');
  const [urgency, setUrgency] = useState<'critical' | 'high' | 'medium'>('high');
  const [location, setLocation] = useState('');

  const handleSupportOffer = (id: string) => {
    setRequests((prev) =>
      prev.map((req) => {
        if (req.id === id) {
          const isUnresolved = req.status === 'unresolved';
          let nextStatus: 'unresolved' | 'investigating' | 'resolved' = 'investigating';
          let countOffset = 1;

          if (req.status === 'investigating') {
            nextStatus = 'resolved';
            countOffset = 1;
          } else if (req.status === 'resolved') {
            nextStatus = 'unresolved';
            countOffset = -2;
          }
          
          return {
            ...req,
            status: nextStatus,
            respondersCount: Math.max(0, req.respondersCount + countOffset)
          };
        }
        return req;
      })
    );
  };

  const submitRequest = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !desc.trim() || !location.trim()) {
      alert('Must fill in title, location coordinates, and primary description details.');
      return;
    }

    const newReq: HelpRequest = {
      id: `help_${Date.now()}`,
      title,
      description: desc,
      category,
      urgency,
      location,
      postedBy: currentUser,
      timestamp: 'Just Now',
      verifiedBy: null, // Self-reported initially
      status: 'unresolved',
      respondersCount: 0
    };

    setRequests([newReq, ...requests]);
    setShowAddModal(false);
    
    // Clear vars
    setTitle('');
    setDesc('');
    setLocation('');
  };

  const getUrgencyBadge = (level: string) => {
    switch (level) {
      case 'critical':
        return 'bg-rose-500/15 text-rose-400 border border-rose-500/30';
      case 'high':
        return isDarkMode 
          ? 'bg-elegant-gold/15 text-elegant-gold border border-elegant-gold/30'
          : 'bg-[#dfd5c6]/20 text-elegant-gold-dark border-elegant-gold-dark/30';
      default:
        return 'bg-[#dfd5c6]/10 text-slate-400 border border-slate-500/20';
    }
  };

  const getCategoryEmoji = (cat: string) => {
    switch (cat) {
      case 'medical': return '🚨';
      case 'food': return '🍞';
      case 'utility': return '⚡';
      case 'shelter': return '🏠';
      case 'rescue': return '🐾';
      default: return '🤝';
    }
  };

  return (
    <div id="verified_help_container" className="flex flex-col h-full w-full relative">
      
      {/* Intro Header */}
      <div className={`p-4 border-b ${isDarkMode ? 'border-elegant-border/10 bg-elegant-card/40' : 'border-[#dfd5c6]/30 bg-[#fbfaf6]'}`}>
        <div className="flex justify-between items-center">
          <div>
            <h3 className={`text-sm font-bold flex items-center gap-1.5 font-mono uppercase ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
              <HeartHandshake className="w-4 h-4" />
              Verified Mutual Aid
            </h3>
            <p className="text-[10px] text-slate-400 font-mono mt-1">
              Double-vetted humanitarian and infrastructure relief tickets.
            </p>
          </div>
          <button
            id="btn_open_request_ticket"
            onClick={() => setShowAddModal(true)}
            className="bg-gradient-to-r from-elegant-gold-dark to-elegant-gold shadow-md text-elegant-bg font-extrabold text-[10px] font-mono px-3 py-1.5 rounded-lg flex items-center gap-1 cursor-pointer transition-colors hover:brightness-110"
          >
            <Plus className="w-3.5 h-3.5" /> File Ticket
          </button>
        </div>
      </div>

      {/* Requests Core Board */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-thin">
        {requests.map((req) => (
          <div
            key={req.id}
            className={`p-4 rounded-2xl border flex flex-col gap-3 transition-colors ${
              req.status === 'resolved' 
                ? (isDarkMode ? 'bg-elegant-card/30 border-elegant-border/5 opacity-60 grayscale' : 'bg-emerald-50/20 border-emerald-200 grayscale opacity-75')
                : (isDarkMode ? 'bg-elegant-card border-elegant-border/10' : 'bg-white border-[#dfd5c6]/45 shadow-sm')
            }`}
          >
            {/* Upper Badge elements */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5">
                <span className="text-lg">{getCategoryEmoji(req.category)}</span>
                <span className={`text-[9px] font-mono tracking-wider font-extrabold uppercase px-2 py-0.5 rounded-full ${getUrgencyBadge(req.urgency)}`}>
                  {req.urgency}
                </span>

                {/* Secure / Vetted check */}
                {req.verifiedBy ? (
                  <span className={`border text-[8.5px] px-2 py-0.5 rounded-full flex items-center gap-1 font-mono ${
                    isDarkMode ? 'bg-elegant-gold/10 text-elegant-gold border-elegant-gold/25' : 'bg-[#dfd5c6]/30 text-elegant-gold-dark border-elegant-gold-dark/25'
                  }`}>
                    <ShieldCheck className="w-3 h-3" /> Vetted
                  </span>
                ) : (
                  <span className="bg-amber-500/10 text-amber-500 border border-amber-400/25 text-[8.5px] px-2 py-0.5 rounded-full flex items-center gap-1 font-mono">
                    <AlertTriangle className="w-3 h-3 text-amber-500" /> Awaiting Vet
                  </span>
                )}
              </div>

              {/* Status Indicator text badge */}
              <span className={`text-[10px] uppercase font-mono px-2 py-0.5 rounded-md ${
                req.status === 'resolved' ? 'bg-emerald-500/20 text-emerald-400 font-bold' :
                req.status === 'investigating' ? 'bg-blue-500/20 text-blue-400' : 'bg-rose-500/10 text-rose-450 animate-pulse'
              }`}>
                {req.status}
              </span>
            </div>

            {/* Title & Desc */}
            <div>
              <h4 className={`text-xs font-bold leading-tight ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>{req.title}</h4>
              <p className={`text-[11.5px] mt-1.5 leading-relaxed ${isDarkMode ? 'text-slate-350' : 'text-slate-700'}`}>{req.description}</p>
            </div>

            {/* Coordinates / Location */}
            <div className={`p-2 rounded-xl text-[10.5px] font-mono flex items-center gap-1.5 border ${
              isDarkMode ? 'bg-elegant-bg text-slate-400 border-elegant-border/10' : 'bg-[#f5f2eb] text-[#3c3730] border-[#dfd5c6]/40'
            }`}>
              📍 Location Details: <span className={`font-bold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>{req.location}</span>
            </div>

            {/* Vetted By note */}
            {req.verifiedBy && (
              <div className={`text-[10px] font-mono p-2 rounded-xl border flex items-center gap-1.5 ${
                isDarkMode ? 'bg-elegant-gold/10 text-elegant-gold border-elegant-gold/25' : 'bg-[#dfd5c6]/20 text-elegant-gold-dark border-elegant-gold-dark/20'
              }`}>
                <Building2 className="w-3.5 h-3.5" /> Checked By: <span className="font-semibold">{req.verifiedBy}</span>
              </div>
            )}

            {/* Support Controls / Progress */}
            <div className="flex items-center justify-between pt-2 border-t border-dashed border-slate-800/20 dark:border-white/5">
              <div className="flex items-center gap-1 text-[11px] font-mono">
                <span className="text-slate-500">Volunteers Responded:</span>
                <span className={`font-bold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>{req.respondersCount} active</span>
              </div>

              <button
                id={`btn_offer_support_${req.id}`}
                onClick={() => handleSupportOffer(req.id)}
                className={`text-[10.5px] font-mono font-bold px-3 py-1.5 rounded-xl border transition-all cursor-pointer ${
                  req.status === 'resolved'
                    ? isDarkMode ? 'bg-elegant-card border-elegant-border text-slate-350 hover:bg-elegant-bg' : 'bg-[#f5f2eb] border-[#dfd5c6]/60 text-[#3c3730]'
                    : req.status === 'investigating'
                    ? 'bg-blue-500 text-white border-blue-600'
                    : 'bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg border-transparent hover:brightness-110 shadow-sm'
                }`}
              >
                {req.status === 'resolved' ? '🔄 Reopen Ticket' : req.status === 'investigating' ? 'Mark Resolved' : 'Offer Support / Dispatch'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Add Help Request overlay modal */}
      <AnimatePresence>
        {showAddModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-elegant-bg/85 backdrop-blur-md z-30 p-6 flex flex-col justify-center animate-fade-in"
          >
            <form onSubmit={submitRequest} className={`p-5 rounded-3xl border flex flex-col gap-4 max-w-sm mx-auto w-full shadow-2xl ${
              isDarkMode ? 'bg-elegant-card border-elegant-border/15 text-white shadow-elegant-gold/5' : 'bg-[#fbfaf6] border-[#dfd5c6]/80 text-slate-800'
            }`}>
              <div className="flex justify-between items-center pb-2 border-b border-slate-800/10 dark:border-white/5">
                <h4 className={`text-xs font-bold uppercase font-mono tracking-wider flex items-center gap-1.5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
                  <HelpingHand className="w-4 h-4" /> Create Crisis Ticket
                </h4>
                <button
                  id="btn_close_add_ticket"
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="p-1 px-2 hover:bg-slate-500/15 rounded text-xs text-slate-400 cursor-pointer"
                >
                  Close
                </button>
              </div>

              {/* Title input */}
              <div className="flex flex-col gap-1">
                <label className="text-[10px] font-mono text-slate-400 uppercase">Need / Alert Title</label>
                <input
                  id="ticket_input_title"
                  type="text"
                  placeholder="e.g. Baby Food Ration Missing"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className={`w-full border focus:ring-1 focus:ring-elegant-gold rounded-xl p-2.5 text-xs outline-none ${
                    isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100 placeholder-slate-600' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                  }`}
                />
              </div>

              {/* Location Input */}
              <div className="flex flex-col gap-1">
                <label className="text-[10px] font-mono text-slate-400 uppercase">Sector / Landmark Coordinates</label>
                <input
                  id="ticket_input_location"
                  type="text"
                  placeholder="e.g. Sector 4 High School Hub"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  className={`w-full border focus:ring-1 focus:ring-elegant-gold rounded-xl p-2.5 text-xs outline-none ${
                    isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100 placeholder-slate-600' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                  }`}
                />
              </div>

              {/* Categories & Urgency Selector Grid */}
              <div className="grid grid-cols-2 gap-2">
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-mono text-slate-400 uppercase">Type</label>
                  <select
                    id="ticket_select_category"
                    value={category}
                    onChange={(e) => setCategory(e.target.value as any)}
                    className={`border rounded-xl p-2 text-xs outline-none ${
                      isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                    }`}
                  >
                    <option value="medical">🚨 Medical</option>
                    <option value="food">🍞 Provisions</option>
                    <option value="utility">⚡ Power/Utility</option>
                    <option value="shelter">🏠 Cabin Shelter</option>
                    <option value="rescue">🐾 Animal Care</option>
                  </select>
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-mono text-slate-400 uppercase">Severity</label>
                  <select
                    id="ticket_select_urgency"
                    value={urgency}
                    onChange={(e) => setUrgency(e.target.value as any)}
                    className={`border rounded-xl p-2 text-xs outline-none ${
                      isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                    }`}
                  >
                    <option value="critical">🔴 Critical Emergency</option>
                    <option value="high">🟠 High Alert</option>
                    <option value="medium">🔵 Info Support</option>
                  </select>
                </div>
              </div>

              {/* Description Input */}
              <div className="flex flex-col gap-1">
                <label className="text-[10px] font-mono text-slate-400 uppercase">Explain details of required assistance</label>
                <textarea
                  id="ticket_text_description"
                  rows={2.5}
                  placeholder="Please state count, specifications, contact options..."
                  value={desc}
                  onChange={(e) => setDesc(e.target.value)}
                  className={`w-full border focus:ring-1 focus:ring-elegant-gold rounded-xl p-2.5 text-xs resize-none outline-none ${
                    isDarkMode ? 'bg-elegant-bg border-elegant-border/20 text-slate-100 placeholder-slate-600' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
                  }`}
                />
              </div>

              <button
                id="btn_submit_crisis_ticket"
                type="submit"
                className="w-full bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg shadow-md font-bold py-3 rounded-2xl text-xs flex items-center justify-center gap-1.5 cursor-pointer hover:brightness-110 transition-all"
              >
                Broadcast Ticket to System Grid
                <Send className="w-3.5 h-3.5 stroke-[2.5]" />
              </button>
            </form>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
