import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * 通用请求状态：空数据是真实结果，失败可重试，不以演示数据替代。
 *
 * @param {Function} fetcher  返回 Promise<Result> 的函数（Result = { success, data, total }）
 * @param {*} initialData     列表使用 []，详情与统计使用 null
 * @param {Array} deps        依赖项，变化时重新请求
 * @param {Function} normalize 可选，把后端 data 归一化为页面需要的结构
 */
export default function useApiData(fetcher, initialData = null, deps = [], normalize) {
  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const requestId = useRef(0);

  const load = useCallback(async (reset = false) => {
    const current = ++requestId.current;
    setLoading(true);
    setError(null);
    if (reset) {
      setData(initialData);
      setLastUpdatedAt(null);
    }
    try {
      const res = await fetcher();
      if (res?.success === false) {
        throw new Error(res.errorMsg || '请求失败');
      }
      const payload = normalize ? normalize(res?.data, res) : res?.data;
      if (current !== requestId.current) return { success: false, cancelled: true };
      setData(payload ?? initialData);
      setLastUpdatedAt(new Date());
      return { success: true, data: payload };
    } catch (err) {
      if (current !== requestId.current) return { success: false, cancelled: true };
      setError(err instanceof Error ? err : new Error('请求失败，请重试'));
      return { success: false, error: err };
    } finally {
      if (current === requestId.current) setLoading(false);
    }
    // 查询依赖由调用者显式传入，避免内联 fetcher 导致重复请求。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    load(true);
    return () => { requestId.current += 1; };
  }, [load]);

  const refetch = useCallback(() => load(false), [load]);
  return { data, loading, error, refetch, setData, lastUpdatedAt };
}
