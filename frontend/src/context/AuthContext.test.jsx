import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { createElement } from 'react';
import { setupBeforeEach, setupAfterEach } from '../test/setup';

beforeEach(() => { setupBeforeEach(); });
afterEach(() => { setupAfterEach(); });

// mock services：保留 api 实例，只替换业务方法
vi.mock('../services', () => {
  const actual = vi.importActual('../services');
  return {
    ...actual,
    studentApi: {
      ...actual.studentApi,
      getMe: vi.fn(),
      logout: vi.fn(),
      getStats: vi.fn(),
    },
  };
});

import { studentApi } from '../services';

const wrapper = ({ children }) => createElement(AuthProvider, null, children);

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    studentApi.getMe.mockReset();
    studentApi.logout.mockReset();
  });

  it('login 保存 token 并触发 getMe', async () => {
    studentApi.getMe.mockResolvedValue({ success: true, data: { id: 1, nickName: '测试' } });
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.isAuthed).toBe(false);
    await act(async () => {
      result.current.login('new-token', null);
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(localStorage.getItem('token')).toBe('new-token');
    expect(result.current.isAuthed).toBe(true);
    expect(studentApi.getMe).toHaveBeenCalled();
    expect(result.current.user?.nickName).toBe('测试');
  });

  // login 直接传入 userInfo 的场景已在页面集成中覆盖，此处不重复。

  it('logout 服务端失败仍清理本地状态', async () => {
    localStorage.setItem('token', 'existing');
    studentApi.getMe.mockResolvedValue({ success: true, data: { nickName: 'U' } });
    studentApi.logout.mockRejectedValueOnce(new Error('服务端异常'));
    const { result } = renderHook(() => useAuth(), { wrapper });
    await act(async () => {
      result.current.login('existing', { nickName: 'U' });
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(result.current.isAuthed).toBe(true);
    await act(async () => {
      try { await result.current.logout(); } catch { /* expected */ }
    });
    expect(localStorage.getItem('token')).toBeNull();
    expect(result.current.isAuthed).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('campus:session-expired 事件清理 token 和 user', async () => {
    studentApi.getMe.mockResolvedValue({ success: true, data: { nickName: 'U' } });
    const { result } = renderHook(() => useAuth(), { wrapper });
    await act(async () => {
      result.current.login('tok', { nickName: 'U' });
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(result.current.isAuthed).toBe(true);
    await act(async () => {
      localStorage.removeItem('token');
      window.dispatchEvent(new Event('campus:session-expired'));
    });
    expect(result.current.isAuthed).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('getMe 网络失败不清 token', async () => {
    localStorage.setItem('token', 'keep-me');
    studentApi.getMe.mockRejectedValueOnce(new Error('Network Error'));
    const { result } = renderHook(() => useAuth(), { wrapper });
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(localStorage.getItem('token')).toBe('keep-me');
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.user).toBeNull();
  });

  it('loading 仅在有 token 且 getMe 未完成时为 true', async () => {
    let resolveGetMe;
    studentApi.getMe.mockReturnValueOnce(new Promise((r) => { resolveGetMe = r; }));
    const { result, rerender } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.loading).toBe(false);
    await act(async () => {
      result.current.login('tok', null);
      await Promise.resolve();
    });
    rerender();
    expect(result.current.loading).toBe(true);
    await act(async () => {
      resolveGetMe({ success: true, data: { nickName: 'X' } });
    });
    rerender();
    expect(result.current.loading).toBe(false);
  });

  it('updateUser 合并用户信息', async () => {
    studentApi.getMe.mockResolvedValue({ success: true, data: { nickName: 'Old', college: 'CS' } });
    const { result } = renderHook(() => useAuth(), { wrapper });
    await act(async () => {
      result.current.login('tok', { nickName: 'Old', college: 'CS' });
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
    });
    act(() => { result.current.updateUser({ nickName: 'New' }); });
    expect(result.current.user.nickName).toBe('New');
    expect(result.current.user.college).toBe('CS');
  });
});
