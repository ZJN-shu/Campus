import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { studentApi } from '../services';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  // 应用启动时 / token 变化时，尝试拉取当前用户
  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    studentApi
      .getMe()
      .then((res) => {
        if (!cancelled) setUser(res?.data || null);
      })
      .catch(() => {
        // token 失效时清除
        if (!cancelled) {
          localStorage.removeItem('token');
          setToken(null);
          setUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [token]);

  const login = useCallback((newToken, userInfo) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    if (userInfo) {
      setUser(userInfo);
    }
    // 如果 userInfo 为空，useEffect 会自动通过 getMe 补拉
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    studentApi.logout().catch(() => {});
  }, []);

  const updateUser = useCallback((info) => {
    setUser((prev) => ({ ...(prev || {}), ...info }));
  }, []);

  const value = {
    user,
    token,
    loading,
    isAuthed: !!token,
    login,
    logout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth 必须在 AuthProvider 内使用');
  }
  return ctx;
}
