import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { studentApi } from '../services';
import useApiData from '../hooks/useApiData';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const { data: user, setData: setUser, loading, error, refetch } = useApiData(
    () => token ? studentApi.getMe() : Promise.resolve({ data: null }),
    null,
    [token]
  );

  useEffect(() => {
    const syncSession = () => {
      setToken(localStorage.getItem('token'));
      setUser(null);
    };
    const onStorage = (event) => {
      if (event.key === 'token' || event.key === null) syncSession();
    };
    window.addEventListener('campus:session-expired', syncSession);
    window.addEventListener('storage', onStorage);
    return () => {
      window.removeEventListener('campus:session-expired', syncSession);
      window.removeEventListener('storage', onStorage);
    };
  }, [setUser]);

  const login = useCallback((newToken, userInfo) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    if (userInfo) {
      setUser(userInfo);
    }
    // 如果 userInfo 为空，useEffect 会自动通过 getMe 补拉
  }, [setUser]);

  const logout = useCallback(async () => {
    try {
      await studentApi.logout();
    } finally {
      localStorage.removeItem('token');
      setToken(null);
      setUser(null);
    }
  }, [setUser]);

  const updateUser = useCallback((info) => {
    setUser((prev) => ({ ...(prev || {}), ...info }));
  }, [setUser]);

  const value = {
    user,
    token,
    loading: !!token && loading,
    error,
    refetchUser: refetch,
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
