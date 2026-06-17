import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Phone, ArrowRight, ShieldCheck, Mail } from 'lucide-react';

interface LoginProps {
  onNext: (phone: string) => void;
  isDarkMode: boolean;
}

export default function LoginView({ onNext, isDarkMode }: LoginProps) {
  const [phone, setPhone] = useState('');
  const [countryCode, setCountryCode] = useState('+1');
  const [error, setError] = useState('');

  const handleDemoFill = () => {
    setPhone('5552345678');
    setCountryCode('+1');
    setError('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanPhone = phone.trim().replace(/\D/g, '');
    if (cleanPhone.length < 8) {
      setError('Please enter a valid phone number (at least 8 digits)');
      return;
    }
    setError('');
    onNext(`${countryCode} ${phone}`);
  };

  return (
    <div 
      id="login_container" 
      className={`flex flex-col h-full w-full p-6 justify-between select-none ${
        isDarkMode ? 'bg-elegant-bg text-white' : 'bg-[#faf9f5] text-[#2c2824]'
      }`}
    >
      {/* Decorative premium elements */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-elegant-gold/10 rounded-full blur-2xl pointer-events-none" />

      {/* Top Banner section */}
      <div className="pt-8">
        <div className="flex items-center gap-1.5 mb-2">
          <div className={`w-2 h-2 rounded-full animate-pulse ${isDarkMode ? 'bg-elegant-gold' : 'bg-elegant-gold-dark'}`} />
          <span className={`text-[11px] font-mono tracking-wider font-bold uppercase ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
            SECURE ACCESS
          </span>
        </div>
        <h2 className="text-2xl font-bold tracking-tight">
          Welcome to <span className="bg-gradient-to-r from-elegant-gold-dark via-elegant-gold to-[#d4c5b3] bg-clip-text text-transparent font-display font-medium tracking-wide">Nexus</span>
        </h2>
        <p className={`text-xs mt-1 leading-relaxed ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
          Enter your cellular number to sync direct chats, nearby volunteer grids and verified aid protocols.
        </p>
      </div>

      {/* Center Phone Form */}
      <form onSubmit={handleSubmit} className="flex-1 flex flex-col justify-center gap-6 py-6 z-10">
        <div className="flex flex-col gap-1.5">
          <label className={`text-[11px] font-mono uppercase tracking-wider ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
            Phone Number
          </label>
          <div className="flex gap-2">
            {/* Country Selector Mock */}
            <select
              id="select_country_code"
              value={countryCode}
              onChange={(e) => setCountryCode(e.target.value)}
              className={`px-3 py-3.5 rounded-2xl text-sm font-medium border duration-200 outline-none focus:ring-1 focus:ring-elegant-gold ${
                isDarkMode 
                  ? 'bg-elegant-card border-elegant-border text-white' 
                  : 'bg-[#fbfaf6] border-[#dfd5c6] text-[#3c3730] shadow-sm'
              }`}
            >
              <option value="+1">🇺🇸 +1</option>
              <option value="+44">🇬🇧 +44</option>
              <option value="+91">🇮🇳 +91</option>
              <option value="+49">🇩🇪 +49</option>
              <option value="+81">🇯🇵 +81</option>
              <option value="+55">🇧🇷 +55</option>
            </select>

            {/* Main Phone Input */}
            <div className="relative flex-1">
              <span className="absolute left-3.5 top-[15px] text-slate-400">
                <Phone className="w-4 h-4" />
              </span>
              <input
                id="input_phone_number"
                type="tel"
                placeholder="555-0100"
                value={phone}
                onChange={(e) => {
                  setPhone(e.target.value);
                  if (error) setError('');
                }}
                className={`w-full pl-10 pr-4 py-3.5 rounded-2xl text-sm border duration-250 outline-none focus:ring-1 focus:ring-elegant-gold ${
                  isDarkMode 
                    ? 'bg-elegant-card border-elegant-border text-white placeholder-slate-600' 
                    : 'bg-[#fbfaf6] border-[#dfd5c6] text-[#3c3730] placeholder-slate-400 shadow-sm'
                }`}
              />
            </div>
          </div>

          {error && (
            <span className="text-rose-500 text-xs mt-1 font-mono">{error}</span>
          )}
        </div>

        {/* Tactile Demo Fill Button */}
        <button
          id="btn_auto_fill_phone"
          type="button"
          onClick={handleDemoFill}
          className={`py-2 px-4 rounded-xl text-xs font-mono border self-start flex items-center gap-1.5 duration-200 cursor-pointer ${
            isDarkMode 
              ? 'bg-elegant-gold/10 hover:bg-elegant-gold/15 border-elegant-border/40 text-elegant-gold' 
              : 'bg-[#dfd5c6]/30 hover:bg-[#dfd5c6]/50 border-[#dfd5c6] text-elegant-gold-dark'
          }`}
        >
          <ShieldCheck className="w-3.5 h-3.5" />
          Auto Fill Demo Credentials
        </button>
      </form>

      {/* Button controls at bottom */}
      <div className="flex flex-col gap-4 pb-4">
        <button
          id="btn_send_otp"
          onClick={handleSubmit}
          className="w-full bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold py-4 rounded-2xl flex items-center justify-center gap-2 shadow-lg hover:brightness-110 active:scale-[0.98] transition-all cursor-pointer"
        >
          Send OTP Verification Code
          <ArrowRight className="w-4 h-4" />
        </button>

        <div className={`text-center text-[10px] uppercase tracking-widest leading-relaxed px-4 ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
          By continuing, you agree to secure peer encryption keys inside the sandboxed space.
        </div>
      </div>
    </div>
  );
}
