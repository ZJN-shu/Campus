import { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { errandApi } from '../services';
import { mockTaskDetail, mockTasks } from '../services/mockData';
import { useAuth } from '../context/AuthContext';
import { Loading } from '../components/ui';

// 后端任务状态：1-待接单 2-进行中（已接单） 3-已完成 5-已取消
const statusMap = {
  1: { label: '待接单', color: 'bg-campus-50 text-campus-500' },
  2: { label: '进行中', color: 'bg-blue-50 text-blue-600' },
  3: { label: '已完成', color: 'bg-gray-100 text-gray-500' },
  5: { label: '已取消', color: 'bg-gray-100 text-gray-400' },
};

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '—');

export default function ErrandDetail() {
  const { id } = useParams();
  const { user, isAuthed } = useAuth();
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [score, setScore] = useState(5);
  const [evalText, setEvalText] = useState('');
  const [evaluated, setEvaluated] = useState(false);

  const { data: task, loading, refetch } = useApiData(
    () => errandApi.getTaskDetail(id),
    { ...mockTaskDetail, id: Number(id) },
    [id],
    (payload) => payload || null
  );

  // 轮询实时刷新：任务处于活跃状态（待接单/进行中）时每 5 秒拉取最新状态
  const pollingRef = useRef(null);
  useEffect(() => {
    if (!task) return;
    const isActive = task.status === 1 || task.status === 2;
    if (!isActive) {
      if (pollingRef.current) clearInterval(pollingRef.current);
      return;
    }
    pollingRef.current = setInterval(() => refetch(), 5000);
    return () => { if (pollingRef.current) clearInterval(pollingRef.current); };
  }, [task?.status, refetch]);

  // 其它可接任务（用于底部推荐）
  const { data: others } = useApiData(
    () => errandApi.getNearbyTasks(),
    mockTasks,
    [],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const run = async (fn, okText) => {
    setMsg('');
    setBusy(true);
    try {
      await fn();
      setMsg(okText || '操作成功');
      refetch();
    } catch (e) {
      setMsg(e?.message || '操作失败，请先登录或稍后再试');
    } finally {
      setBusy(false);
    }
  };

  const handleAccept = () => run(() => errandApi.acceptTask(id), '接单成功，请尽快完成～');
  const handleComplete = () => run(() => errandApi.completeTask(id), '任务已标记完成');
  const handleCancel = () => run(() => errandApi.cancelTask(id), '任务已取消');
  const handleEvaluate = () =>
    run(async () => {
      await errandApi.evaluate(id, score, evalText);
      setEvaluated(true);
    }, '评价成功，感谢反馈！');

  if (loading) {
    return (
      <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
        <Loading rows={2} />
      </div>
    );
  }

  const st = statusMap[task.status] || statusMap[1];
  const publisher = task.publisherName || '同学';
  const isPublisher = isAuthed && user?.id != null && Number(task.publisherId) === Number(user.id);
  const isAcceptor = isAuthed && user?.id != null && task.acceptorId != null && Number(task.acceptorId) === Number(user.id);
  const related = others.filter((t) => String(t.id) !== String(id)).slice(0, 3);

  return (
    <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-12">
      <Link to="/errand" className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 mb-8 transition-colors">
        ← 返回跑腿
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        {/* 主内容 */}
        <div className="lg:col-span-2 space-y-8">
          <section className="card p-8 md:p-10">
            <div className="flex items-center gap-2.5 mb-5 flex-wrap">
              <span className={`tag ${st.color}`}>{st.label}</span>
              {task.category && <span className="tag bg-gray-50 text-gray-500">{task.category}</span>}
            </div>
            <h1 className="text-2xl md:text-3xl font-bold text-gray-800 leading-tight mb-6">{task.title}</h1>
            <div className="flex items-baseline gap-3 mb-8">
              <span className="text-sm text-gray-400">赏金</span>
              <span className="text-4xl font-bold text-primary-500">¥{task.reward}</span>
            </div>

            <div className="text-gray-600 leading-loose whitespace-pre-line text-[15px] mb-8">
              {task.description || '发布者没有填写更多描述。'}
            </div>

            {/* 路线 */}
            <div className="bg-gray-50/70 p-6 flex items-center gap-5">
              <div className="flex-1 min-w-0">
                <div className="text-xs text-gray-400 mb-1.5">📍 取件地点</div>
                <div className="text-sm font-semibold text-gray-700 truncate">{task.pickupLocation || '—'}</div>
              </div>
              <span className="text-gray-300 text-xl shrink-0">→</span>
              <div className="flex-1 min-w-0">
                <div className="text-xs text-gray-400 mb-1.5">🏁 送达地点</div>
                <div className="text-sm font-semibold text-gray-700 truncate">{task.deliveryLocation || '—'}</div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-5 mt-6 text-sm">
              <div>
                <span className="text-gray-400">截止时间：</span>
                <span className="text-gray-600 font-medium">{fmtTime(task.deadline)}</span>
              </div>
              <div>
                <span className="text-gray-400">发布时间：</span>
                <span className="text-gray-600 font-medium">{fmtTime(task.createTime)}</span>
              </div>
            </div>
          </section>

          {/* 评价区（已完成时可评价） */}
          {task.status === 3 && (
            <section className="card p-8">
              <h2 className="text-base font-bold text-gray-800 mb-6">⭐ 评价本次服务</h2>
              {evaluated ? (
                <p className="text-sm text-campus-500">已评价，感谢你的反馈！</p>
              ) : (
                <>
                  <div className="flex items-center gap-2 mb-5">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button
                        key={n}
                        onClick={() => setScore(n)}
                        className={`text-2xl transition-transform ${n <= score ? 'opacity-100 scale-110' : 'opacity-30 grayscale'}`}
                      >
                        ⭐
                      </button>
                    ))}
                    <span className="text-sm text-gray-400 ml-2">{score} 分</span>
                  </div>
                  <textarea
                    rows={3}
                    value={evalText}
                    onChange={(e) => setEvalText(e.target.value)}
                    placeholder="说说这次的体验吧（选填）..."
                    className="input resize-none mb-4"
                  />
                  <button onClick={handleEvaluate} disabled={busy} className="btn-primary px-7">
                    {busy ? '提交中...' : '提交评价'}
                  </button>
                </>
              )}
            </section>
          )}

          {/* 相关推荐 */}
          {related.length > 0 && (
            <section>
              <h2 className="section-title mb-6">你可能也想接</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                {related.map((t) => (
                  <Link key={t.id} to={`/errand/${t.id}`} className="card card-hover p-6 group">
                    <h3 className="font-semibold text-gray-800 group-hover:text-campus-500 transition-colors line-clamp-1 mb-3">{t.title}</h3>
                    <div className="flex items-center justify-between">
                      <span className="text-lg font-bold text-primary-500">¥{t.reward}</span>
                      {t.category && <span className="tag bg-gray-50 text-gray-500">{t.category}</span>}
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          )}
        </div>

        {/* 侧栏 */}
        <div className="space-y-6">
          <div className="card p-7 lg:sticky lg:top-24">
            <h2 className="text-sm font-bold text-gray-700 mb-5">发布者</h2>
            <div className="flex items-center gap-3.5 mb-7">
              <div className="w-12 h-12 rounded-full bg-gradient-to-br from-campus-400 to-sky-400 flex items-center justify-center text-white font-bold shrink-0">
                {task.publisherAvatar ? (
                  <img src={task.publisherAvatar} alt="" className="w-full h-full object-cover rounded-full" />
                ) : (
                  publisher[0]
                )}
              </div>
              <div className="min-w-0">
                <div className="text-sm font-semibold text-gray-700 truncate">{publisher}</div>
                <div className="text-xs text-gray-400 mt-0.5">校园认证用户</div>
              </div>
            </div>

            {task.acceptorName && (
              <div className="text-sm text-gray-500 bg-gray-50 px-4 py-3 mb-6">
                🏃 接单人：<span className="font-medium text-gray-700">{task.acceptorName}</span>
              </div>
            )}

            {msg && (
              <div className="text-sm px-4 py-3 mb-5 bg-primary-50 text-primary-600">{msg}</div>
            )}

            <div className="space-y-3">
              {task.status === 1 && !isPublisher && (
                <button onClick={handleAccept} disabled={busy} className="btn-primary w-full">
                  {busy ? '处理中...' : '🙋 立即接单'}
                </button>
              )}
              {task.status === 2 && isAcceptor && (
                <button onClick={handleComplete} disabled={busy} className="btn-primary w-full">
                  {busy ? '处理中...' : '✅ 完成任务'}
                </button>
              )}
              {task.status === 1 && isPublisher && (
                <button onClick={handleCancel} disabled={busy} className="btn-ghost w-full text-red-500 hover:!text-red-600 hover:!bg-red-50">
                  {busy ? '处理中...' : '取消任务'}
                </button>
              )}
              {!isAuthed && task.status === 1 && (
                <Link to="/login" className="btn-primary w-full">登录后接单</Link>
              )}
              {task.status !== 1 && (
                <div className="text-center text-sm text-gray-400 py-2">该任务当前为「{st.label}」状态</div>
              )}
            </div>

            <div className="mt-7 pt-6 border-t border-gray-50 text-xs text-gray-400 leading-relaxed">
              💡 安全提示：接单后请及时沟通，贵重物品建议当面交接，完成后再确认支付。
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
