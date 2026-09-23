import { useState, useRef } from 'react';
import useApiData from '../hooks/useApiData';
import { messageApi } from '../services';
import { useAuth } from '../context/AuthContext';
import { Loading, Empty, DataStatus, ErrorNotice, PageHeader, LoginPrompt } from '../components/ui';

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
  const { isAuthed, token } = useAuth();
  const [activeFilter, setActiveFilter] = useState('all');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const actionLock = useRef(false);
  const activeType = filterTabs.find((t) => t.key === activeFilter)?.type;

  const state = useApiData(
    () => isAuthed ? messageApi.getMessages(1, activeType) : Promise.resolve({ data: [] }),
    [],
    [activeType, token],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const { data: messages, loading, error, lastUpdatedAt, setData } = state;
  const unreadCount = messages.filter((m) => m.isRead === false || m.isRead === 0).length;

  const markRead = async (id) => {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusy(true);
    setActionError(null);
    try {
      if (id == null) await messageApi.markAllAsRead();
      else await messageApi.markAsRead(id);
      setData((prev) => prev.map((m) => id == null || m.id === id ? { ...m, isRead: true } : m));
      window.dispatchEvent(new Event('campus:messages-updated'));
    } catch (err) {
      setActionError(err);
    } finally {
      actionLock.current = false;
      setBusy(false);
    }
  };
  if (!isAuthed) return <div className="page-container"><LoginPrompt /></div>; 

  return (
    <div className="page-container">
      {/* Header */}
      <PageHeader title="消息通知" description={lastUpdatedAt ? `当前已加载列表有 ${unreadCount} 条确认未读消息（非全站总数）` : '互动提醒与系统消息'}>
        {unreadCount > 0 && <button disabled={busy} onClick={() => markRead(null)} className="btn-ghost">{busy ? '处理中…' : '全部标记已读'}</button>}
      </PageHeader>
      <ErrorNotice error={actionError} />

      {/* Filters */}
      <div className="flex flex-wrap gap-1.5 mb-4 bg-white p-1.5 w-fit border border-gray-100">
        {filterTabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveFilter(tab.key)}
                          disabled={busy}
                          aria-pressed={activeFilter === tab.key}
            className={`px-5 py-2 text-sm font-semibold transition-all ${
              activeFilter === tab.key ? 'bg-gray-800 text-white shadow-sm' : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Message List */}
      <DataStatus {...state} count={messages.length} label="消息当前页" />
      {loading && !lastUpdatedAt ? (
        <Loading rows={4} />
      ) : error && !lastUpdatedAt ? null : messages.length === 0 ? (
        <Empty icon="🔔" title="暂无消息" desc="有新动态时会第一时间通知你" />
      ) : (
        <div className="card divide-y divide-gray-50 overflow-hidden">
          {messages.map((msg) => {
            const config = typeConfig[msg.type] || typeConfig[5];
            return (
              <button
                type="button"
                key={msg.id}
                disabled={busy || !!msg.isRead}
                onClick={() => markRead(msg.id)}
                aria-label={`${msg.content}，${msg.isRead ? '已读' : '标记已读'}`}
                className={`w-full text-left flex items-start gap-4 px-4 sm:px-6 py-5 hover:bg-gray-50/60 transition-colors ${
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
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
