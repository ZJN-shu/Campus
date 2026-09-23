import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import api from './api';
import { setupBeforeEach, setupAfterEach } from '../test/setup';

beforeEach(() => { setupBeforeEach(); });
afterEach(() => { setupAfterEach(); });

// 构造 axios 兼容的 mock adapter
function mockAdapter({ status = 200, data = {}, headers = {}, config } = {}) {
  const res = {
    data,
    status,
    statusText: 'OK',
    headers: { 'content-type': 'application/json', ...headers },
    config: config || { headers: { authorization: undefined } },
  };
  return status >= 200 && status < 300
    ? Promise.resolve(res)
    : Promise.reject(Object.assign(new Error(`Request failed with status code ${status}`), {
      response: res,
      isAxiosError: true,
      code: undefined,
      config: res.config,
    }));
}

describe('api 请求链路', () => {
  it('请求拦截器附加原始 token（不加 Bearer）', async () => {
    localStorage.setItem('token', 'raw-token-123');
    let capturedConfig;
    api.defaults.adapter = (config) => {
      capturedConfig = config;
      return mockAdapter({ data: { success: true, data: null }, config });
    };
    await api.get('/test');
    expect(capturedConfig.headers.authorization).toBe('raw-token-123');
  });

  it('success:true 返回原始响应 data', async () => {
    api.defaults.adapter = (config) => mockAdapter({
      data: { success: true, data: { id: 1 }, total: 10 },
      config,
    });
    const result = await api.get('/test');
    expect(result).toEqual({ success: true, data: { id: 1 }, total: 10 });
  });

  it('success:false 抛出 errorMsg', async () => {
    api.defaults.adapter = (config) => mockAdapter({
      data: { success: false, errorMsg: '帖子不存在' },
      config,
    });
    await expect(api.get('/test')).rejects.toThrow('帖子不存在');
  });

  it('success:false 携带 traceId', async () => {
    api.defaults.adapter = (config) => mockAdapter({
      data: { success: false, errorMsg: '失败' },
      headers: { 'x-trace-id': 'trace-abc-123' },
      config,
    });
    try {
      await api.get('/test');
      expect.unreachable('应抛出错误');
    } catch (e) {
      expect(e.traceId).toBe('trace-abc-123');
    }
  });
});

describe('会话同步边界', () => {
  it('401 且 token 匹配时清理会话并广播事件', async () => {
    localStorage.setItem('token', 'current-token');
    const handler = vi.fn();
    window.addEventListener('campus:session-expired', handler);
    api.defaults.adapter = (config) => mockAdapter({
      status: 401,
      data: { success: false, errorMsg: '未登录' },
      config: { ...config, headers: { authorization: 'current-token' } },
    });
    try { await api.get('/test'); } catch { /* expected */ }
    expect(localStorage.getItem('token')).toBeNull();
    expect(handler).toHaveBeenCalledTimes(1);
    window.removeEventListener('campus:session-expired', handler);
  });

  it('401 但 token 不匹配时不清理会话', async () => {
    localStorage.setItem('token', 'new-token');
    const handler = vi.fn();
    window.addEventListener('campus:session-expired', handler);
    api.defaults.adapter = (config) => mockAdapter({
      status: 401,
      data: { success: false, errorMsg: '未登录' },
      config: { ...config, headers: { authorization: 'old-token' } },
    });
    try { await api.get('/test'); } catch { /* expected */ }
    expect(localStorage.getItem('token')).toBe('new-token');
    expect(handler).not.toHaveBeenCalled();
    window.removeEventListener('campus:session-expired', handler);
  });

  it('403 保留会话', async () => {
    localStorage.setItem('token', 'keep-me');
    api.defaults.adapter = (config) => mockAdapter({
      status: 403,
      data: { success: false, errorMsg: '无权限' },
      config,
    });
    try { await api.get('/test'); } catch { /* expected */ }
    expect(localStorage.getItem('token')).toBe('keep-me');
  });

  it('5xx 不暴露内部错误', async () => {
    api.defaults.adapter = (config) => mockAdapter({
      status: 500,
      data: { success: false, errorMsg: 'java.lang.NullPointerException' },
      config,
    });
    try {
      await api.get('/test');
      expect.unreachable('应抛出错误');
    } catch (e) {
      expect(e.message).toBe('服务暂时不可用，请稍后重试');
      expect(e.message).not.toContain('NullPointer');
    }
  });

  it('网络错误保留 token', async () => {
    localStorage.setItem('token', 'keep-me');
    api.defaults.adapter = () => Promise.reject(Object.assign(new Error('Network Error'), {
      isAxiosError: true,
      code: undefined,
      response: undefined,
      config: { headers: {} },
    }));
    try { await api.get('/test'); } catch { /* expected */ }
    expect(localStorage.getItem('token')).toBe('keep-me');
  });

  it('超时返回专用提示', async () => {
    api.defaults.adapter = () => Promise.reject(Object.assign(new Error('timeout'), {
      isAxiosError: true,
      code: 'ECONNABORTED',
      response: undefined,
      config: { headers: {} },
    }));
    await expect(api.get('/test')).rejects.toThrow('请求超时，请重试');
  });
});
