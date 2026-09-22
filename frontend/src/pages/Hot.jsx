import { useState } from 'react';
import { Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { hotApi } from '../services';
import { mockHotRank } from '../services/mockData';
import { Loading, Empty } from '../components/ui';

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

  const { data: list, loading } = useApiData(
    () => {
      if (activeTab === 'today') return hotApi.getTodayHot();
      if (activeTab === 'rising') return hotApi.getRisingHot();
      return hotApi.getHotRank(activeCategory === '全部' ? 'all' : activeCategory, 1);
    },
    mockHotRank,
    [activeTab, activeCategory],
    normalizeList
  );

  const items =
    activeTab === 'rank' && activeCategory !== '全部'
      ? list.filter((i) => !i.category || i.category === activeCategory)
      : list;

  const trendIcon = (trend) => {
    const t = Number(trend);
    if (t > 0) return <span className="text-red-400">↑</span>;
    if (t < 0) return <span className="text-campus-500">↓</span>;
    return <span className="text-gray-300">—</span>;
  };

  return (
    <div className="max-w-4xl 2xl:max-w-5xl mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* Header */}
      <div className="mb-12">
        <h1 className="text-2xl font-bold text-gray-800">热榜</h1>
        <p className="text-sm text-gray-400 mt-2">发现校园热门话题</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1.5 mb-8 bg-white p-1.5 w-fit border border-gray-100">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
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
        <div className="flex flex-wrap gap-2.5 mb-12">
          {categories.map((cat) => (
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

      {/* Rank List */}
      {loading ? (
        <Loading rows={5} />
      ) : items.length === 0 ? (
        <Empty icon="🔥" title="暂无榜单数据" />
      ) : (
        <div className="card overflow-hidden">
          <div className="bg-gradient-to-r from-primary-400 to-pink-400 px-9 py-6 text-white">
            <h2 className="text-base font-bold">
              {activeTab === 'today' ? '今日热榜' : activeTab === 'rising' ? '飙升榜' : '热门榜单'}
            </h2>
            <p className="text-xs opacity-85 mt-1">每 5 分钟更新一次</p>
          </div>

          <div className="divide-y divide-gray-50">
            {items.map((item, index) => (
              <Link
                key={item.id || index}
                to={`/posts/${item.id}`}
                className="flex items-center gap-6 px-9 py-6 hover:bg-gray-50/60 transition-colors group"
              >
                <span className={`w-9 h-9 flex items-center justify-center text-sm font-bold shrink-0 ${
                  index < 3 ? 'bg-gradient-to-br from-primary-400 to-pink-500 text-white' : 'bg-gray-100 text-gray-400'
                }`}>
                  {item.rank || index + 1}
                </span>

                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-700 truncate group-hover:text-primary-600 transition-colors">
                    {item.title}
                  </h3>
                  <div className="flex items-center gap-4 mt-2.5 text-xs text-gray-400">
                    {item.category && <span className="tag bg-gray-50 text-gray-500">{item.category}</span>}
                    <span>👀 {item.viewCount ?? 0}</span>
                    <span>❤️ {item.likeCount ?? 0}</span>
                  </div>
                </div>

                <div className="shrink-0 text-right">
                  <div className="text-base font-bold text-pink-500">
                    {Number(item.hotScore ?? 0).toLocaleString()}
                  </div>
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
