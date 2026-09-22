import { useState } from 'react';
import useApiData from '../hooks/useApiData';
import { messageApi } from '../services';
import { mockMessages } from '../services/mockData';
import { Loading, Empty } from '../components/ui';

// 后端 MessageDTO.type 为整数：1评论 2点赞 3收藏 4关注 5系统
const typeConfig = {
  1: { icon: '💬', label: '评论' },
  2: { icon: '👍', label: '点赞' },
  3: { icon: '⭐', label: '收藏' },
  4: { icon: '👥', label: '关注' },
  5: { icon: '📢', label: '系统' },
};

const filterTabs = [
  { key: 'all', label: '全部', type: undefined },
  { key: 'comment', label: '评论', type: 1 },
  { key: 'like', label: '点赞', type: 2 },
  { key: 'follow', label: '关注', type: 4 },
];

export default function Messages() {
  const [activeFilter, setActiveFilter] = useState('all');
  const activeType = filterTabs.find((t) => t.key === activeFilter)?.type;

  const { data: messages, loading, refetch, setData } = useApiData(
    () => messageApi.getMessages(1, activeType),
    activeType === undefined ? mockMessages : mockMessages.filter((m) => m.type === activeType),
    [activeType],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const unreadCount = messages.filter((m) => !m.isRead).length;

  const handleMarkAllRead = async () => {
    setData((prev) => prev.map((m) => ({ ...m, isRead: true })));
    try {
      await messageApi.markAllAsRead();
    } catch (_) {
      /* 后端不可用时仅本地更新 */
    }
  };

  const handleMarkRead = async (msg) => {
    if (msg.isRead) return;
    setData((prev) => prev.map((m) => (m.id === msg.id ? { ...m, isRead: true } : m)));
    try {
      await messageApi.markAsRead(msg.id);
    } catch (_) {}
  };

  return (
    <div className="max-w-3xl 2xl:max-w-4xl mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* Header */}
      <div className="flex items-center justify-between mb-12">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">消息通知</h1>
          <p className="text-sm text-gray-400 mt-2">
            {unreadCount > 0 ? `你有 ${unreadCount} 条未读消息` : '暂无未读消息'}
          </p>
        </div>
        {unreadCount > 0 && (
          <button onClick={handleMarkAllRead} className="text-sm text-primary-500 hover:text-primary-600 font-semibold">
            全部已读
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="flex gap-1.5 mb-10 bg-white p-1.5 w-fit border border-gray-100">
        {filterTabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveFilter(tab.key)}
            className={`px-5 py-2 text-sm font-semibold transition-all ${
              activeFilter === tab.key ? 'bg-gray-800 text-white shadow-sm' : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Message List */}
      {loading ? (
        <Loading rows={4} />
      ) : messages.length === 0 ? (
        <Empty icon="🔔" title="暂无消息" desc="有新动态时会第一时间通知你" />
      ) : (
        <div className="card divide-y divide-gray-50 overflow-hidden">
          {messages.map((msg) => {
            const config = typeConfig[msg.type] || typeConfig[5];
            return (
              <div
                key={msg.id}
                onClick={() => handleMarkRead(msg)}
                className={`flex items-start gap-4 px-7 py-6 hover:bg-gray-50/60 transition-colors cursor-pointer ${
                  !msg.isRead ? 'bg-primary-50/40' : ''
                }`}
              >
                <div className="w-11 h-11 rounded-full bg-white border border-gray-100 flex items-center justify-center text-lg shrink-0">
                  {config.icon}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className="text-sm font-semibold text-gray-700">{msg.fromUserName || '系统'}</span>
                    {!msg.isRead && <span className="w-2 h-2 bg-pink-500 rounded-full shrink-0"></span>}
                  </div>
                  <p className="text-sm text-gray-500 leading-relaxed">{msg.content}</p>
                  <span className="text-xs text-gray-400 mt-2 block">{msg.createTime}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
