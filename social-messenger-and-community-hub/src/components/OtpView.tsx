import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, Loader2, RefreshCw, KeyRound } from 'lucide-react';

interface OtpProps {
  phone: string;
  onVerified: () => void;
  onBack: () => void;
  isDarkMode: boolean;
}

export default function OtpView({ phone, onVerified, onBack, isDarkMode }: OtpProps) {
  const [code, setCode] = useState<string[]>(['', '', '', '']);
  const [resendTimer, setResendTimer] = useState(25);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Ticker for resend countdown
  useEffect(() => {
    if (resendTimer > 0) {
      const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendTimer]);

  const handleChange = (index: number, val: string) => {
    if (isNaN(Number(val))) return;
    const newCode = [...code];
    newCode[index] = val.substring(val.length - 1);
    setCode(newCode);

    // Auto focus next input
    if (val && index < 3) {
      const nextInput = document.getElementById(`otp_input_${index + 1}`);
      nextInput?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !code[index] && index > 0) {
      const prevInput = document.getElementById(`otp_input_${index - 1}`);
      prevInput?.focus();
    }
  };

  const verifyCode = (fullCode: string) => {
    setLoading(true);
    setError('');
    
    // Deliberate realistic premium delay animation
    setTimeout(() => {
      setLoading(false);
      if (fullCode === '7799' || fullCode === '1234') {
        onVerified();
      } else {
        setError('Incorrect security code. Tip: Use "7799" or "1234" for instant sandbox pass.');
      }
    }, 1200);
  };

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    const fullCode = code.join('');
    if (fullCode.length < 4) {
      setError('Please fill in all 4 digits');
      return;
    }
    verifyCode(fullCode);
  };

  const handleShortcutCode = () => {
    const defaultCode = ['7', '7', '9', '9'];
    setCode(defaultCode);
    verifyCode('7799');
  };

  return (
    <div 
      id="otp_container" 
      className={`flex flex-col h-full w-full p-6 justify-between select-none ${
        isDarkMode ? 'bg-elegant-bg text-white' : 'bg-[#faf9f5] text-[#2c2824]'
      }`}
    >
      {/* Top action and Title */}
      <div>
        <button
          id="btn_back_to_login"
          onClick={onBack}
          className={`p-2 rounded-xl border flex items-center justify-center cursor-pointer transition-all ${
            isDarkMode ? 'border-elegant-border bg-elegant-card/50 hover:bg-elegant-card text-elegant-gold' : 'border-[#dfd5c6] bg-white hover:bg-[#e9e4d9] text-[#3c3730] shadow-sm'
          }`}
        >
          <ArrowLeft className="w-4 h-4" />
        </button>

        <div className="pt-6">
          <div className="flex items-center gap-1.5 mb-2">
            <KeyRound className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
            <span className={`text-[11px] font-mono tracking-wider font-bold uppercase ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
              OTP Verification
            </span>
          </div>
          <h2 className="text-2xl font-bold font-display tracking-tight">Security Check</h2>
          <p className={`text-xs mt-1.5 leading-relaxed ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
            We sent a secure validation token to <span className={`font-semibold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>{phone}</span>.
          </p>
        </div>
      </div>

      {/* Center Pin Entry Cells */}
      <div className="flex-1 flex flex-col justify-center gap-8 py-4 z-10">
        <form onSubmit={handleSubmit} className="flex flex-col items-center gap-4">
          <div className="flex justify-center gap-4">
            {code.map((num, i) => (
              <input
                key={i}
                id={`otp_input_${i}`}
                type="text"
                pattern="[0-9]*"
                inputMode="numeric"
                value={num}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                disabled={loading}
                className={`w-14 h-15 rounded-2xl text-center text-xl font-bold border outline-none font-mono transition-all ${
                  isDarkMode 
                    ? 'bg-elegant-card border-elegant-border/30 text-elegant-gold focus:ring-1 focus:ring-elegant-gold' 
                    : 'bg-[#fbfaf6] border-[#dfd5c6] text-[#3c3730] shadow-sm focus:ring-1 focus:ring-elegant-gold'
                }`}
              />
            ))}
          </div>

          {error && (
            <p className="text-rose-500 text-xs text-center max-w-xs font-mono">{error}</p>
          )}

          {/* Shortcut sandbox credentials helper for testing ease */}
          {!loading && (
            <button
              id="btn_autofill_otp"
              type="button"
              onClick={handleShortcutCode}
              className={`text-xs border px-4 py-2.5 rounded-xl font-mono flex items-center gap-2 transition-all cursor-pointer ${
                isDarkMode 
                  ? 'bg-elegant-gold/10 hover:bg-elegant-gold/15 border-elegant-border/40 text-elegant-gold' 
                  : 'bg-[#dfd5c6]/30 hover:bg-[#dfd5c6]/50 border-[#dfd5c6] text-elegant-gold-dark'
              }`}
            >
              🔑 Sandbox Autofill & Fast Pass (7799)
            </button>
          )}
        </form>

        {/* Resend details */}
        <div className="flex items-center justify-center gap-2">
          {resendTimer > 0 ? (
            <span className={`text-xs font-mono ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
              Resend code in {resendTimer}s
            </span>
          ) : (
            <button
              id="btn_resend_otp_code"
              onClick={() => {
                setResendTimer(30);
                setError('');
              }}
              className={`text-xs hover:underline font-mono flex items-center gap-1.5 cursor-pointer ${
                isDarkMode ? 'text-elegant-gold hover:text-elegant-gold-light' : 'text-elegant-gold-dark hover:text-elegant-gold'
              }`}
            >
              <RefreshCw className="w-3 h-3" /> Resend Code
            </button>
          )}
        </div>
      </div>

      {/* Lower button panel */}
      <div className="pb-4">
        <button
          id="btn_verify_otp"
          onClick={handleSubmit}
          disabled={loading}
          className="w-full bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold py-4 rounded-2xl flex items-center justify-center gap-2 shadow-lg hover:brightness-110 active:scale-[0.98] transition-all cursor-pointer disabled:opacity-50"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin text-elegant-bg" />
              Verifying Key Signature...
            </>
          ) : (
            'Verify & Sign In'
          )}
        </button>
      </div>
    </div>
  );
}
