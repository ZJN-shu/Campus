import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { errandApi } from '../services';
import { mockTasks } from '../services/mockData';
import { Empty } from '../components/ui';

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
  const tab = searchParams.get('tab');
  const personal = tab === 'published' || tab === 'accepted';
  const [activeCategory, setActiveCategory] = useState('全部');
  const [showPublishModal, setShowPublishModal] = useState(false);

  const { data: tasks, loading, refetch } = useApiData(
    () => {
      if (tab === 'published') return errandApi.getMyPublished(1);
      if (tab === 'accepted') return errandApi.getMyAccepted(1);
      return errandApi.getNearbyTasks();
    },
    personal ? [] : mockTasks,
    [tab],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const filtered =
    activeCategory === '全部'
      ? tasks
      : tasks.filter((t) => t.category === activeCategory);

  return (
    <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* Header */}
      <div className="flex items-center justify-between mb-12">
        <div>
          {personal && (
            <Link to="/errand" className="inline-flex items-center gap-1 text-sm text-gray-400 hover:text-gray-600 mb-3 transition-colors">
              ← 返回全部任务
            </Link>
          )}
          <h1 className="text-2xl font-bold text-gray-800">
            {tab === 'published' ? '我发布的任务' : tab === 'accepted' ? '我接取的任务' : '跑腿任务'}
          </h1>
          <p className="text-sm text-gray-400 mt-2">
            {tab === 'published' ? '管理你发布的跑腿需求' : tab === 'accepted' ? '查看你正在处理的任务' : '发布任务，找人帮忙'}
          </p>
        </div>
        <button
          onClick={() => setShowPublishModal(true)}
          className="px-6 py-2.5 bg-gradient-to-r from-campus-400 to-sky-400 text-white text-sm font-semibold hover:shadow-lg hover:-translate-y-0.5 transition-all"
        >
          📋 发布任务
        </button>
      </div>

      {/* Filters */}
      {!personal && (
        <div className="flex flex-wrap gap-2.5 mb-12">
          {taskCategories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
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
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-6">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="card p-8">
              <div className="skeleton h-4 w-1/2 mb-5" />
              <div className="skeleton h-3 w-full mb-3" />
              <div className="skeleton h-3 w-2/3" />
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <Empty
          icon="🏃"
          title={tab === 'published' ? '你还没有发布过任务' : tab === 'accepted' ? '你还没有接取任务' : '暂无任务'}
          desc={tab === 'published' ? '点击右上角发布你的第一个跑腿需求吧～' : tab === 'accepted' ? '去任务广场看看，接一单赚零花钱' : '换个分类看看，或发布一个新任务'}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-6">
          {filtered.map((task) => {
            const st = statusMap[task.status] || statusMap[1];
            const publisher = task.publisherName || '同学';
            return (
              <Link key={task.id} to={`/errand/${task.id}`} className="card card-hover p-8 group">
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
                  <span className="text-2xl font-bold text-primary-500 shrink-0">¥{task.reward}</span>
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
                  {task.deadline && (
                    <span className="text-xs text-primary-500 font-medium">⏰ {String(task.deadline).slice(5, 16).replace('T', ' ')}</span>
                  )}
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
            refetch();
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
    setError('');
    if (!form.title.trim() || !form.reward) {
      setError('请填写任务标题和赏金');
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
      setError(e?.message || '发布失败，请先登录或稍后再试');
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/30 backdrop-blur-sm animate-fade-in">
      <div className="bg-white w-full max-w-lg p-9 shadow-2xl animate-slide-up max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-8">
          <h3 className="text-xl font-bold text-gray-800">发布跑腿任务</h3>
          <button onClick={onClose} className="w-9 h-9 text-gray-300 hover:text-gray-500 hover:bg-gray-50 text-lg transition-colors">✕</button>
        </div>
        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">任务标题</label>
            <input type="text" value={form.title} onChange={update('title')} placeholder="例如：帮取快递" className="input" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">任务分类</label>
            <select value={form.category} onChange={update('category')} className="input">
              {categories.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">任务描述</label>
            <textarea rows={3} value={form.description} onChange={update('description')} placeholder="详细描述你的需求..." className="input resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">取件地点</label>
              <input type="text" value={form.pickupLocation} onChange={update('pickupLocation')} placeholder="菜鸟驿站" className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">送达地点</label>
              <input type="text" value={form.deliveryLocation} onChange={update('deliveryLocation')} placeholder="6号宿舍楼" className="input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">赏金 (元)</label>
              <input type="number" value={form.reward} onChange={update('reward')} placeholder="5" className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">截止时间</label>
              <input type="datetime-local" value={form.deadline} onChange={update('deadline')} className="input" />
            </div>
          </div>
          {error && (
            <div className="text-sm text-red-500 bg-red-50 px-4 py-2.5">{error}</div>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} className="btn-ghost">取消</button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="px-8 py-2.5 bg-gradient-to-r from-campus-400 to-sky-400 text-white text-sm font-semibold hover:shadow-lg transition-all disabled:opacity-60"
            >
              {submitting ? '发布中...' : '发布任务'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
