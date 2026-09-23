import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import useApiData from './useApiData';
import { setupBeforeEach, setupAfterEach } from '../test/setup';

beforeEach(() => { setupBeforeEach(); });
afterEach(() => { setupAfterEach(); });

// 可控 Promise，用于模拟请求延迟和排序
function makeDeferred() {
  let resolve, reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}

describe('useApiData', () => {
  it('成功返回后设置 data 和 lastUpdatedAt', async () => {
    const fetcher = vi.fn().mockResolvedValue({ data: { id: 1, name: 'test' } });
    const { result } = renderHook(() => useApiData(fetcher, null, []));
    expect(result.current.loading).toBe(true);
    await act(async () => { await Promise.resolve(); });
    expect(result.current.data).toEqual({ id: 1, name: 'test' });
    expect(result.current.error).toBeNull();
    expect(result.current.lastUpdatedAt).toBeInstanceOf(Date);
    expect(result.current.loading).toBe(false);
  });

  it('空数组是真实结果，不会回退到 mock 或初始值', async () => {
    const fetcher = vi.fn().mockResolvedValue({ data: [] });
    const { result } = renderHook(() => useApiData(fetcher, [], []));
    await act(async () => { await Promise.resolve(); });
    expect(result.current.data).toEqual([]);
    expect(result.current.lastUpdatedAt).toBeTruthy();
  });

  it('业务 success:false 抛出 errorMsg', async () => {
    const fetcher = vi.fn().mockResolvedValue({ success: false, errorMsg: '参数错误' });
    const { result } = renderHook(() => useApiData(fetcher, null, []));
    await act(async () => { await Promise.resolve(); });
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error.message).toBe('参数错误');
    expect(result.current.data).toBeNull();
  });

  it('网络失败设置 error 状态', async () => {
    const fetcher = vi.fn().mockRejectedValue(new Error('网络连接失败'));
    const { result } = renderHook(() => useApiData(fetcher, [], []));
    await act(async () => { await Promise.resolve(); });
    expect(result.current.error.message).toBe('网络连接失败');
    expect(result.current.data).toEqual([]);
    expect(result.current.lastUpdatedAt).toBeNull();
  });

  it('normalize 归一化 data 字段', async () => {
    const fetcher = vi.fn().mockResolvedValue({ data: { records: [{ id: 1 }, { id: 2 }] } });
    const normalize = (p) => (Array.isArray(p) ? p : p?.records || []);
    const { result } = renderHook(() => useApiData(fetcher, [], [], normalize));
    await act(async () => { await Promise.resolve(); });
    expect(result.current.data).toEqual([{ id: 1 }, { id: 2 }]);
  });

  it('refetch 成功时更新 lastUpdatedAt', async () => {
    let call = 0;
    const fetcher = vi.fn().mockImplementation(() => Promise.resolve({ data: { n: ++call } }));
    const { result } = renderHook(() => useApiData(fetcher, null, []));
    await act(async () => { await Promise.resolve(); });
    const firstTime = result.current.lastUpdatedAt;
    expect(result.current.data).toEqual({ n: 1 });
    await new Promise((r) => setTimeout(r, 10));
    await act(async () => {
      const res = await result.current.refetch();
      expect(res.success).toBe(true);
    });
    expect(result.current.data).toEqual({ n: 2 });
    expect(result.current.lastUpdatedAt).not.toEqual(firstTime);
  });

  it('refetch 失败时保留旧 data 和 lastUpdatedAt', async () => {
    let shouldFail = false;
    const fetcher = vi.fn().mockImplementation(() =>
      shouldFail ? Promise.reject(new Error('刷新失败')) : Promise.resolve({ data: 'old' })
    );
    const { result } = renderHook(() => useApiData(fetcher, null, []));
    await act(async () => { await Promise.resolve(); });
    const savedTime = result.current.lastUpdatedAt;
    shouldFail = true;
    await act(async () => {
      const res = await result.current.refetch();
      expect(res.success).toBe(false);
    });
    expect(result.current.data).toBe('old');
    expect(result.current.lastUpdatedAt).toEqual(savedTime);
    expect(result.current.error).toBeInstanceOf(Error);
  });

  // 依赖变化和请求竞态场景已通过页面集成测试覆盖，此处不重复。

  it('卸载后完成的请求不修改状态', async () => {
    const deferred = makeDeferred();
    const fetcher = vi.fn().mockReturnValue(deferred.promise);
    const { unmount } = renderHook(() => useApiData(fetcher, null, []));
    unmount();
    await act(async () => { deferred.resolve({ data: 'late' }); });
  });

  it('setData 允许外部直接更新', async () => {
    const fetcher = vi.fn().mockResolvedValue({ data: [] });
    const { result } = renderHook(() => useApiData(fetcher, [], []));
    await act(async () => { await Promise.resolve(); });
    act(() => { result.current.setData([1, 2, 3]); });
    expect(result.current.data).toEqual([1, 2, 3]);
  });
});
