import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { studentApi } from '../services';

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const startCountdown = () => {
    setCountdown(60);
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleSendCode = async () => {
    setError('');
    if (!/^1\d{10}$/.test(phone)) {
      setError('请输入正确的 11 位手机号');
      return;
    }
    try {
      await studentApi.sendCode(phone);
      startCountdown();
    } catch (e) {
      // 后端未启动时也允许进入倒计时，方便演示
      startCountdown();
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!/^1\d{10}$/.test(phone)) {
      setError('请输入正确的 11 位手机号');
      return;
    }
    if (!code) {
      setError('请输入验证码');
      return;
    }
    setSubmitting(true);
    try {
      const res = await studentApi.login({ phone, code });
      const token = res?.data;
      if (!token) throw new Error('登录失败');
      let user = null;
      try {
        const me = await studentApi.getMe();
        user = me?.data || null;
      } catch (_) {
        /* 忽略获取用户信息失败 */
      }
      login(token, user);
      navigate('/');
    } catch (err) {
      setError(err?.message || '登录失败，请检查验证码');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-6 py-16">
      {/* 背景装饰 */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-16 left-16 text-6xl animate-float opacity-15">🎓</div>
        <div className="absolute top-1/4 right-24 text-5xl animate-float opacity-15" style={{ animationDelay: '0.6s' }}>📚</div>
        <div className="absolute bottom-24 left-1/4 text-4xl animate-float opacity-15" style={{ animationDelay: '1.2s' }}>✨</div>
        <div className="absolute bottom-1/3 right-1/3 text-5xl animate-float opacity-15" style={{ animationDelay: '1.8s' }}>🌟</div>
      </div>

      <div className="relative w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-10">
          <Link to="/" className="inline-flex items-center gap-3">
            <span className="text-5xl animate-float">🎓</span>
            <span className="text-3xl font-bold bg-gradient-to-r from-primary-500 to-pink-500 bg-clip-text text-transparent">
              Campus
            </span>
          </Link>
          <p className="text-gray-400 mt-3 text-sm">连接校园每一个角落</p>
        </div>

        {/* 登录卡片（毛玻璃仅限登录卡片） */}
        <div className="bg-white/80 backdrop-blur-lg shadow-xl shadow-primary-100/40 p-10 border border-white/60">
          <h2 className="text-2xl font-bold text-center text-gray-800 mb-8">欢迎回来 👋</h2>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2">手机号</label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="请输入手机号"
                maxLength={11}
                className="input"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2">验证码</label>
              <div className="flex gap-3">
                <input
                  type="text"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="6 位验证码"
                  maxLength={6}
                  className="input flex-1"
                />
                <button
                  type="button"
                  onClick={handleSendCode}
                  disabled={countdown > 0}
                  className={`shrink-0 px-5 text-sm font-medium transition-all whitespace-nowrap ${
                    countdown > 0
                      ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                      : 'bg-primary-50 text-primary-600 hover:bg-primary-100'
                  }`}
                >
                  {countdown > 0 ? `${countdown}s` : '获取验证码'}
                </button>
              </div>
            </div>

            {error && (
              <div className="text-sm text-red-500 bg-red-50 px-4 py-2.5 animate-fade-in">
                {error}
              </div>
            )}

            <button type="submit" disabled={submitting} className="btn-primary w-full py-3.5 text-base">
              {submitting ? '登录中...' : '登录 / 注册'}
            </button>
          </form>

          <p className="text-center text-xs text-gray-400 mt-6 leading-relaxed">
            未注册的手机号将自动创建账号 · 登录即表示同意
            <span className="text-primary-500 cursor-pointer"> 用户协议</span>
          </p>
        </div>
      </div>
    </div>
  );
}
