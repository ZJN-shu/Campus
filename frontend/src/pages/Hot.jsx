import { useState } from 'react';
import { Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { hotApi } from '../services';
import { Loading, Empty, DataStatus, PageHeader } from '../components/ui';

const categories = ['全部', '学习', '生活', '社团', '运动', '二手'];
const tabs = [
  { key: 'rank', label: '🔥 热榜' },
  { key: 'today', label: '📅 今日' },
  { key: 'rising', label: '📈 飙升' },
];

const normalizeList = (payload) =>
  Array.isArray(payload) ? payload : payload?.records || payload?.list || [];

export default function Hot() {
  const [activeCategory, setActiveCategory] = useState('全部');
  const [activeTab, setActiveTab] = useState('rank');

  const state = useApiData(
    () => {
      if (activeTab === 'today') return hotApi.getTodayHot();
      if (activeTab === 'rising') return hotApi.getRisingHot();
      return hotApi.getHotRank(activeCategory === '全部' ? 'all' : activeCategory, 1);
    },
    [],
    [activeTab, activeCategory],
    normalizeList
  );

  const { data: list, loading, error, lastUpdatedAt } = state;
  const items =
    activeTab === 'rank' && activeCategory !== '全部'
      ? list.filter((i) => !i.category || i.category === activeCategory)
      : list;

  const trendIcon = (trend) => {
    if (trend == null || trend === '' || !Number.isFinite(Number(trend))) return null;
    const t = Number(trend);
    if (t > 0) return <span className="text-red-600" aria-label={`上升 ${t} 位`}>↑ {t}</span>;
    if (t < 0) return <span className="text-green-700" aria-label={`下降 ${Math.abs(t)} 位`}>↓ {Math.abs(t)}</span>;
    return <span className="text-gray-500">持平</span>;
  };

  return (
    <div className="page-container">
      {/* Header */}
      <PageHeader title="校园热榜" description="发现正在被讨论的话题 · 热度为相对排序分数，不是浏览次数" />

      {/* Tabs */}
      <div className="flex flex-wrap gap-1.5 mb-4 bg-white p-1.5 w-fit border border-gray-200">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
                        aria-pressed={activeTab === tab.key}
            className={`px-6 py-2.5 text-sm font-semibold transition-all ${
              activeTab === tab.key ? 'bg-gray-800 text-white shadow-sm' : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Categories */}
      {activeTab === 'rank' && (
        <div className="flex flex-wrap gap-2.5 mb-4">
          {categories.map((cat) => (
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

      {/* Rank List */}
      <DataStatus {...state} count={items.length} label={activeTab === 'rank' ? `${activeCategory} · 热榜当前页` : activeTab === 'today' ? '今日榜' : '飙升榜'} />
      {loading && !lastUpdatedAt ? (
        <Loading rows={5} />
      ) : error && !lastUpdatedAt ? null : items.length === 0 ? (
        <Empty icon="🔥" title="暂无榜单数据" />
      ) : (
        <div className="card overflow-hidden">
          <div className="bg-gray-50 px-5 sm:px-6 py-4 text-gray-800 border-b border-gray-200">
            <h2 className="text-base font-bold">
              {activeTab === 'today' ? '今日热榜' : activeTab === 'rising' ? '飙升榜' : '热门榜单'}
            </h2>
            <p className="text-xs text-gray-600 mt-1">按接口返回顺序展示；点击刷新获取最新结果</p>
          </div>

          <div className="divide-y divide-gray-50">
            {items.map((item, index) => (
              <Link
                key={item.id || index}
                to={`/posts/${item.id}`}
                className="flex items-center gap-3 sm:gap-5 px-4 sm:px-6 py-5 hover:bg-gray-50/60 transition-colors group"
              >
                <span className={`w-9 h-9 flex items-center justify-center text-sm font-bold shrink-0 ${
                  index < 3 ? 'bg-gradient-to-br from-primary-400 to-pink-500 text-white' : 'bg-gray-100 text-gray-400'
                }`}>
                  {item.rank || index + 1}
                </span>

                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-800 line-clamp-2 group-hover:text-primary-600 transition-colors">
                    {item.title}
                  </h3>
                  <div className="flex flex-wrap items-center gap-3 mt-2.5 text-xs text-gray-400">
                    {item.category && <span className="tag bg-gray-50 text-gray-500">{item.category}</span>}
                    <span>👀 {item.viewCount ?? '—'}</span>
                    <span>❤️ {item.likeCount ?? '—'}</span>
                  </div>
                </div>

                <div className="shrink-0 text-right">
                  <div className="text-sm sm:text-base font-bold text-orange-700 tabular-nums">
                    {item.hotScore != null && item.hotScore !== '' && Number.isFinite(Number(item.hotScore)) ? Number(item.hotScore).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '—'}
                  </div>
                  <span className="text-xs text-gray-500">相对热度</span>
                  <div className="text-sm mt-1">{trendIcon(item.trend)}</div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
