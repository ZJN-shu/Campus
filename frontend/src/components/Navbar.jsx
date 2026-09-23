import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { messageApi } from '../services';
import useApiData from '../hooks/useApiData';

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
  const { user, token, isAuthed, logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const unreadState = useApiData(
    () => isAuthed ? messageApi.getUnreadCount() : Promise.resolve({ data: null }),
    null,
    [token, location.pathname]
  );
  const unread = unreadState.data == null ? null : Number(unreadState.data);
  const { refetch: refreshUnread } = unreadState;
  useEffect(() => {
    const update = () => refreshUnread();
    window.addEventListener('campus:messages-updated', update);
    return () => window.removeEventListener('campus:messages-updated', update);
  }, [refreshUnread]);

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await logout();
      navigate('/login');
    } catch {
      navigate('/login', { state: { notice: '本地已退出，服务端退出未确认。' } });
    } finally {
      setLoggingOut(false);
      setShowMobileMenu(false);
    }
  };

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
    <nav aria-label="主导航" className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-gray-200">
      <div className="nav-container">
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
                const isActive = location.pathname === item.path || (item.path !== '/' && location.pathname.startsWith(`${item.path}/`));
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    aria-current={isActive ? 'page' : undefined}
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
                  aria-label="搜索二手商品"
                  className="w-44 pl-9 pr-4 py-2 bg-gray-50 border-0 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:bg-white transition-all"
                />
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm">🔍</span>
              </div>
            </form>

            {isAuthed ? (
              <>
                <Link to="/messages" aria-label={unreadState.error ? '消息通知，未读数量暂不可用' : `消息通知${unread != null ? `，${unread} 条未读` : ''}`} title={unreadState.error ? '未读数量获取失败，请进入消息页重试' : '消息通知'} className="relative p-2 text-gray-600 transition-colors">
                  <span className="text-lg">🔔</span>
                  {unreadState.error ? <span className="absolute -top-1 right-0 text-xs text-red-600">!</span> : unread > 0 && (
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
              className="lg:hidden p-2 text-gray-600"
              aria-label={showMobileMenu ? '收起导航菜单' : '展开导航菜单'}
              aria-expanded={showMobileMenu}
              aria-controls="mobile-navigation"
              onClick={() => setShowMobileMenu(!showMobileMenu)}
            >
              <span className="text-xl">{showMobileMenu ? '✕' : '☰'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {showMobileMenu && (
        <div id="mobile-navigation" className="lg:hidden bg-white border-t border-gray-100 animate-slide-up">
          <div className="px-6 py-5 space-y-1">
            <form onSubmit={handleSearch} className="mb-4">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="搜索好物..."
                aria-label="搜索二手商品"
                className="input"
              />
            </form>
            {navItems.map((item) => {
              const isActive = location.pathname === item.path || (item.path !== '/' && location.pathname.startsWith(`${item.path}/`));
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  aria-current={isActive ? 'page' : undefined}
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
                onClick={handleLogout}
                disabled={loggingOut}
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
