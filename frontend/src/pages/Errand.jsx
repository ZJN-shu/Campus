import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { errandApi } from '../services';
import { useAuth } from '../context/AuthContext';
import { Empty, DataStatus, PageHeader, Modal, ErrorNotice, LoginPrompt } from '../components/ui';

const taskCategories = ['全部', '取件', '带饭', '代购', '其他'];
// 后端任务状态：1-待接单 2-进行中（已接单） 3-已完成 5-已取消
const statusMap = {
  1: { label: '待接单', color: 'bg-campus-50 text-campus-500' },
  2: { label: '进行中', color: 'bg-blue-50 text-blue-600' },
  3: { label: '已完成', color: 'bg-gray-100 text-gray-500' },
  5: { label: '已取消', color: 'bg-gray-100 text-gray-400' },
};

export default function Errand() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthed, token } = useAuth();
  const [notice, setNotice] = useState('');
  const tab = searchParams.get('tab');
  const personal = tab === 'published' || tab === 'accepted';
  const [activeCategory, setActiveCategory] = useState('全部');
  const [showPublishModal, setShowPublishModal] = useState(false);

  const state = useApiData(
    () => {
      if (personal && !isAuthed) return Promise.resolve({ data: [] });
      if (tab === 'published') return errandApi.getMyPublished(1);
      if (tab === 'accepted') return errandApi.getMyAccepted(1);
      return errandApi.getNearbyTasks();
    },
    [],
    [tab, token],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const { data: tasks, loading, refetch, error, lastUpdatedAt } = state;
  const filtered =
    personal || activeCategory === '全部'
      ? tasks
      : tasks.filter((t) => t.category === activeCategory);

  if (personal && !isAuthed) return <div className="page-container"><LoginPrompt /></div>;

  return (
    <div className="page-container">
      {/* Header */}
      <PageHeader title={tab === 'published' ? '我发布的任务' : tab === 'accepted' ? '我接取的任务' : '跑腿任务'} description="看清任务状态、路线和截止时间，再安排你的校园行程">
        <button onClick={() => isAuthed ? setShowPublishModal(true) : navigate('/login')} className="btn-primary">📋 发布任务</button>
      </PageHeader>
      <nav aria-label="跑腿视图" className="flex flex-wrap gap-3 mb-4">
        {[['/errand', '全部任务', !personal], ['/errand?tab=published', '我发布的', tab === 'published'], ['/errand?tab=accepted', '我接取的', tab === 'accepted']].map(([to, label, active]) => (
          <Link key={to} to={to} aria-current={active ? 'page' : undefined} className={`px-4 py-2 text-sm border ${active ? 'bg-gray-800 text-white border-gray-800' : 'bg-white border-gray-200 text-gray-600'}`}>{label}</Link>
        ))}
      </nav>
      {notice && <p role="status" className="bg-green-50 text-green-800 p-3 mb-4 text-sm">{notice}</p>}

      {/* Filters */}
      {!personal && (
        <div className="card p-4 flex flex-wrap gap-2.5 mb-4">
          {taskCategories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
                            aria-pressed={activeCategory === cat}
              className={`px-5 py-2 text-sm font-medium transition-all ${
                activeCategory === cat
                  ? 'bg-gray-800 text-white shadow-sm'
                  : 'bg-white text-gray-500 hover:bg-gray-50 border border-gray-100'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      )}

      {/* Task Grid */}
      <DataStatus {...state} count={filtered.length} label={personal ? '我的任务 · 已加载范围' : `${activeCategory} · 已加载范围`} />
      {loading && !lastUpdatedAt ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="card p-8">
              <div className="skeleton h-4 w-1/2 mb-5" />
              <div className="skeleton h-3 w-full mb-3" />
              <div className="skeleton h-3 w-2/3" />
            </div>
          ))}
        </div>
      ) : error && !lastUpdatedAt ? null : filtered.length === 0 ? (
        <Empty
          icon="🏃"
          title={tab === 'published' ? '你还没有发布过任务' : tab === 'accepted' ? '你还没有接取任务' : '暂无任务'}
          desc={tab === 'published' ? '点击右上角发布你的第一个跑腿需求吧～' : tab === 'accepted' ? '去任务广场看看，接一单赚零花钱' : '换个分类看看，或发布一个新任务'}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {filtered.map((task) => {
            const st = statusMap[task.status] || { label: '状态未知', color: 'bg-gray-100 text-gray-600' };
            const publisher = task.publisherName || '同学';
            return (
              <Link key={task.id} to={`/errand/${task.id}`} className="card card-hover p-5 sm:p-6 group">
                <div className="flex items-start justify-between mb-5 gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-3 flex-wrap">
                      <span className={`tag ${st.color}`}>{st.label}</span>
                      {task.category && <span className="tag bg-gray-50 text-gray-500">{task.category}</span>}
                    </div>
                    <h3 className="font-bold text-gray-800 group-hover:text-campus-500 transition-colors line-clamp-1">
                      {task.title}
                    </h3>
                  </div>
                  <span className="text-2xl font-bold text-primary-500 shrink-0">¥{task.reward ?? '—'}</span>
                </div>

                <p className="text-sm text-gray-400 leading-relaxed mb-5 line-clamp-2">{task.description}</p>

                <div className="flex items-center gap-2 text-xs text-gray-400 mb-6">
                  <span className="truncate">📍 {task.pickupLocation || '—'}</span>
                  <span className="text-gray-300 shrink-0">→</span>
                  <span className="truncate">{task.deliveryLocation || '—'}</span>
                </div>

                <div className="flex items-center justify-between pt-5 border-t border-gray-50">
                  <div className="flex items-center gap-2.5">
                    <div className="w-7 h-7 rounded-full bg-gradient-to-br from-campus-400 to-sky-400 flex items-center justify-center text-white text-xs font-bold">
                      {publisher[0]}
                    </div>
                    <span className="text-sm text-gray-500">{publisher}</span>
                  </div>
                  <span className="text-xs text-primary-600 font-medium">截止：{task.deadline ? String(task.deadline).slice(5, 16).replace('T', ' ') : '未填写'}</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {/* Publish Modal */}
      {showPublishModal && (
        <PublishTaskModal
          categories={taskCategories.filter((c) => c !== '全部')}
          onClose={() => setShowPublishModal(false)}
          onPublished={() => {
            setShowPublishModal(false);
            setNotice('发布成功；正在获取最新列表。');
            refetch().then((result) => setNotice(result.success ? '发布成功，列表已更新。' : '发布成功，但列表更新失败，请重试刷新；无需再次发布。'));
          }}
        />
      )}
    </div>
  );
}

function PublishTaskModal({ categories, onClose, onPublished }) {
  const [form, setForm] = useState({
    title: '',
    category: categories[0],
    description: '',
    pickupLocation: '',
    deliveryLocation: '',
    reward: '',
    deadline: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async () => {
    if (submitting) return;
    setError('');
    if (!form.title.trim() || !Number.isFinite(Number(form.reward)) || Number(form.reward) <= 0) {
      setError('请填写任务标题和大于零的赏金');
      return;
    }
    setSubmitting(true);
    try {
      await errandApi.publishTask({
        title: form.title,
        category: form.category,
        description: form.description,
        pickupLocation: form.pickupLocation,
        deliveryLocation: form.deliveryLocation,
        reward: Number(form.reward),
        deadline: form.deadline ? new Date(form.deadline).toISOString() : null,
      });
      onPublished();
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  };

  return (
    <Modal title="发布跑腿任务" onClose={onClose} busy={submitting}>
        <div className="space-y-6">
          <div>
            <label htmlFor="task-title" className="block text-sm font-medium text-gray-600 mb-2.5">任务标题</label>
            <input id="task-title" type="text" value={form.title} onChange={update('title')} placeholder="例如：帮取快递" className="input" />
          </div>
          <div>
            <label htmlFor="task-category" className="block text-sm font-medium text-gray-600 mb-2.5">任务分类</label>
            <select id="task-category" value={form.category} onChange={update('category')} className="input">
              {categories.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="task-description" className="block text-sm font-medium text-gray-600 mb-2.5">任务描述</label>
            <textarea id="task-description" rows={3} value={form.description} onChange={update('description')} placeholder="详细描述你的需求..." className="input resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="task-pickup" className="block text-sm font-medium text-gray-600 mb-2.5">取件地点</label>
              <input id="task-pickup" type="text" value={form.pickupLocation} onChange={update('pickupLocation')} placeholder="菜鸟驿站" className="input" />
            </div>
            <div>
              <label htmlFor="task-delivery" className="block text-sm font-medium text-gray-600 mb-2.5">送达地点</label>
              <input id="task-delivery" type="text" value={form.deliveryLocation} onChange={update('deliveryLocation')} placeholder="6号宿舍楼" className="input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="task-reward" className="block text-sm font-medium text-gray-600 mb-2.5">赏金 (元)</label>
              <input id="task-reward" min="0.01" step="0.01" type="number" value={form.reward} onChange={update('reward')} placeholder="5" className="input" />
            </div>
            <div>
              <label htmlFor="task-deadline" className="block text-sm font-medium text-gray-600 mb-2.5">截止时间</label>
              <input id="task-deadline" type="datetime-local" value={form.deadline} onChange={update('deadline')} className="input" />
            </div>
          </div>
          <ErrorNotice error={error} />
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} disabled={submitting} className="btn-ghost">取消</button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="btn-primary"
            >
              {submitting ? '发布中...' : '发布任务'}
            </button>
          </div>
        </div>
    </Modal>
  );
}
