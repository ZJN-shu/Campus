import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * 通用数据请求 Hook：优先调用后端真实接口，失败或空数据时回退到 mock。
 *
 * @param {Function} fetcher  返回 Promise<Result> 的函数（Result = { success, data, total }）
 * @param {*} fallback        兜底数据（后端不可用时使用）
 * @param {Array} deps        依赖项，变化时重新请求
 * @param {Function} normalize 可选，把后端 data 归一化为页面需要的结构
 */
export default function useApiData(fetcher, fallback, deps = [], normalize) {
  const [data, setData] = useState(fallback);
  const [loading, setLoading] = useState(true);
  const [usingMock, setUsingMock] = useState(false);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetcher();
      let payload = res?.data;
      if (normalize) payload = normalize(payload, res);
      const empty =
        payload == null ||
        (Array.isArray(payload) && payload.length === 0);
      if (!mounted.current) return;
      if (empty) {
        setData(fallback);
        setUsingMock(true);
      } else {
        setData(payload);
        setUsingMock(false);
      }
    } catch (err) {
      if (!mounted.current) return;
      // 后端未启动 / 网络错误时静默回退到 mock，保证 UI 可用
      setData(fallback);
      setUsingMock(true);
    } finally {
      if (mounted.current) setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, usingMock, refetch: load, setData };
}
