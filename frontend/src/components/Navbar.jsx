import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { messageApi } from '../services';

const navItems = [
  { path: '/', label: '首页', icon: '🏠' },
  { path: '/posts', label: '社区', icon: '💬' },
  { path: '/errand', label: '跑腿', icon: '🏃' },
  { path: '/market', label: '二手', icon: '🛍️' },
  { path: '/hot', label: '热榜', icon: '🔥' },
];

export default function Navbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthed, logout } = useAuth();
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [unread, setUnread] = useState(0);

  // 拉取未读消息数（后端不可用时静默忽略）
  useEffect(() => {
    if (!isAuthed) {
      setUnread(0);
      return;
    }
    messageApi
      .getUnreadCount()
      .then((res) => setUnread(Number(res?.data) || 0))
      .catch(() => setUnread(0));
  }, [isAuthed, location.pathname]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/market?keyword=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery('');
      setShowMobileMenu(false);
    }
  };

  const avatarChar = user?.nickName?.[0] || 'C';

  return (
    <nav className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
      <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw]">
        <div className="flex items-center justify-between h-[68px]">
          {/* Left: Logo + Nav */}
          <div className="flex items-center gap-10">
            <Link to="/" className="flex items-center gap-2 shrink-0">
              <span className="text-2xl">🎓</span>
              <span className="text-lg font-bold bg-gradient-to-r from-primary-500 to-pink-500 bg-clip-text text-transparent hidden sm:block">
                Campus
              </span>
            </Link>

            <div className="hidden lg:flex items-center gap-1">
              {navItems.map((item) => {
                const isActive = location.pathname === item.path;
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`px-4 py-2 text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-primary-50 text-primary-600'
                        : 'text-gray-500 hover:text-gray-800 hover:bg-gray-50'
                    }`}
                  >
                    <span className="mr-1.5">{item.icon}</span>
                    {item.label}
                  </Link>
                );
              })}
            </div>
          </div>

          {/* Right: Search + Actions */}
          <div className="flex items-center gap-3">
            <form onSubmit={handleSearch} className="hidden md:block">
              <div className="relative">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="搜索好物..."
                  className="w-44 pl-9 pr-4 py-2 bg-gray-50 border-0 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:bg-white transition-all"
                />
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm">🔍</span>
              </div>
            </form>

            {isAuthed ? (
              <>
                <Link to="/messages" className="relative p-2 text-gray-400 hover:text-gray-600 transition-colors">
                  <span className="text-lg">🔔</span>
                  {unread > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 bg-pink-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                      {unread > 99 ? '99+' : unread}
                    </span>
                  )}
                </Link>

                <Link
                  to="/profile"
                  className="flex items-center gap-2 pl-1 pr-3 py-1 rounded-full hover:bg-gray-50 transition-colors"
                >
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-pink-400 flex items-center justify-center text-white text-xs font-bold">
                    {avatarChar}
                  </div>
                  <span className="hidden sm:block text-sm text-gray-600 max-w-[80px] truncate">
                    {user?.nickName || '我的'}
                  </span>
                </Link>
              </>
            ) : (
              <Link to="/login" className="btn-primary !py-2 !px-5 text-sm">
                登录
              </Link>
            )}

            <button
              className="lg:hidden p-2 text-gray-400"
              onClick={() => setShowMobileMenu(!showMobileMenu)}
            >
              <span className="text-xl">{showMobileMenu ? '✕' : '☰'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {showMobileMenu && (
        <div className="lg:hidden bg-white border-t border-gray-100 animate-slide-up">
          <div className="px-6 py-5 space-y-1">
            <form onSubmit={handleSearch} className="mb-4">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="搜索好物..."
                className="input"
              />
            </form>
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setShowMobileMenu(false)}
                  className={`flex items-center gap-3 px-4 py-3 text-sm font-medium transition-colors ${
                    isActive ? 'bg-primary-50 text-primary-600' : 'text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  <span>{item.icon}</span>
                  <span>{item.label}</span>
                </Link>
              );
            })}
            {isAuthed && (
              <button
                onClick={() => {
                  logout();
                  setShowMobileMenu(false);
                  navigate('/login');
                }}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm font-medium text-gray-500 hover:bg-gray-50"
              >
                <span>🚪</span>
                <span>退出登录</span>
              </button>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
