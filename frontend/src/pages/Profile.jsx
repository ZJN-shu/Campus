import { useState, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import useApiData from '../hooks/useApiData';
import { studentApi, uploadApi } from '../services';
import { Loading, Empty, DataStatus, ErrorNotice, LoginPrompt, Modal } from '../components/ui';

const statCards = [
  { key: 'postCount', label: '帖子', icon: '📝' },
  { key: 'productCount', label: '商品', icon: '📦' },
  { key: 'favoriteCount', label: '收藏', icon: '⭐' },
  { key: 'followerCount', label: '粉丝', icon: '👥' },
];

const menuItems = [
  { icon: '📝', label: '我的帖子', desc: '查看发布的内容', to: '/posts?mine=1' },
  { icon: '⭐', label: '我的收藏', desc: '收藏的宝贝', to: '/market?fav=1' },
  { icon: '📋', label: '发布的任务', desc: '我发布的跑腿', to: '/errand?tab=published' },
  { icon: '🏃', label: '接取的任务', desc: '我正在做的', to: '/errand?tab=accepted' },
  { icon: '📦', label: '我的商品', desc: '发布的二手', to: '/market?mine=1' },
  { icon: '🔔', label: '消息通知', desc: '互动提醒', to: '/messages' },
];

export default function Profile() {
  const navigate = useNavigate();
  const { user, token, isAuthed, logout, updateUser, loading: userLoading, error: userError, refetchUser } = useAuth();
  const [showSettings, setShowSettings] = useState(false);

  const statsState = useApiData(() => isAuthed ? studentApi.getStats() : Promise.resolve({ data: null }), null, [token]);
  const { data: stats } = statsState;
  const [loggingOut, setLoggingOut] = useState(false);
  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await logout();
      navigate('/login');
    } catch {
      navigate('/login', { state: { notice: '本地已退出，服务端退出未确认。' } });
    } finally {
      setLoggingOut(false);
    }
  };
  if (!isAuthed) return <div className="page-container"><LoginPrompt /></div>;
  if (userLoading && !user) return <div className="page-container"><Loading rows={2} /></div>;
  if (!user) return <div className="page-container"><ErrorNotice error={userError} onRetry={refetchUser} />{!userError && <><Empty title="个人资料暂不可用" /><button onClick={refetchUser} className="btn-ghost mt-4">重试</button></>}</div>;
  const profile = user;
  const avatarChar = profile.nickName?.[0] || 'C';

  return (
    <div className="page-container">
      {/* User Card */}
      <div className="relative overflow-hidden bg-gradient-to-br from-orange-700 to-rose-700 p-6 sm:p-8 mb-6 text-white">
        <div className="relative z-10 flex flex-wrap items-center gap-5">
          <div className="w-24 h-24 bg-white/20 backdrop-blur flex items-center justify-center text-5xl shrink-0 border border-white/30">
            {profile.avatar ? (
              <img src={profile.avatar} alt="avatar" className="w-full h-full object-cover" />
            ) : (
              <span className="font-bold text-3xl">{avatarChar}</span>
            )}
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl font-bold mb-2 truncate">{profile.nickName}</h1>
            <p className="text-white/85 text-sm mb-4 truncate">
              {[profile.college, profile.major].filter(Boolean).join(' · ') || '校园同学'}
            </p>
            <div className="flex flex-wrap gap-2.5">
              {profile.grade && <span className="px-3.5 py-1.5 bg-white/15 backdrop-blur text-xs font-medium">{profile.grade}级</span>}
              {profile.credit != null && <span className="px-3.5 py-1.5 bg-white/15 backdrop-blur text-xs font-medium">⭐ 信用 {profile.credit}</span>}
            </div>
          </div>
        </div>
        <div className="absolute -right-6 -bottom-6 text-9xl opacity-10">🎓</div>
      </div>

      <ErrorNotice error={userError} onRetry={refetchUser} stale />
      {/* Stats */}
      <DataStatus {...statsState} label="个人统计" />
      <div className="grid grid-cols-2 md:grid-cols-4 gap-5 mb-6">
        {statCards.map((s) => (
          <div key={s.key} className="card card-hover p-6 text-center">
            <div className="text-2xl mb-2">{s.icon}</div>
            <div className="text-2xl font-bold text-gray-800">{stats?.[s.key] ?? '—'}</div>
            <div className="text-xs text-gray-400 mt-1.5">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Menu */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4 mb-6">
        {menuItems.map((item, index) => (
          <Link
            key={index}
            to={item.to}
            className="card card-hover flex items-center gap-4 px-7 py-5 group"
          >
            <span className="w-10 h-10 bg-gray-50 flex items-center justify-center text-lg shrink-0">{item.icon}</span>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold text-gray-700">{item.label}</div>
              <div className="text-xs text-gray-400 mt-0.5">{item.desc}</div>
            </div>
            <span className="text-gray-300 group-hover:text-gray-400 group-hover:translate-x-0.5 transition-all">→</span>
          </Link>
        ))}
      </div>

      {/* Actions */}
      <div className="flex gap-4">
        <button onClick={() => setShowSettings(true)} className="btn-ghost flex-1">⚙️ 账号设置</button>
        {isAuthed && (
          <button onClick={handleLogout} disabled={loggingOut} className="flex-1 px-6 py-2.5 text-sm font-medium text-red-500 bg-white border border-gray-100 hover:bg-red-50 hover:border-red-100 transition-colors">
            {loggingOut ? '正在退出…' : '🚪 退出登录'}
          </button>
        )}
      </div>

      {showSettings && (
        <SettingsModal
          profile={profile}
          isAuthed={isAuthed}
          onClose={() => setShowSettings(false)}
          onSaved={(info) => {
            updateUser(info);
            setShowSettings(false);
          }}
        />
      )}
    </div>
  );
}

function SettingsModal({ profile, isAuthed, onClose, onSaved }) {
  const [form, setForm] = useState({
    nickName: profile.nickName || '',
    avatar: profile.avatar || '',
    college: profile.college || '',
    major: profile.major || '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  // 头像上传
  const handleAvatarUpload = async (e) => {
    if (uploading || submitting) return;
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    setUploading(true);
    try {
      const res = await uploadApi.upload(file, 'avatar');
      if (!res?.data) throw new Error('上传未返回头像地址');
      setForm((prev) => ({ ...prev, avatar: res.data }));
    } catch (err) {
      setError(err);
    } finally {
      setUploading(false);
      // 清空 input 以便重复选择同一文件
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async () => {
    if (uploading || submitting) return;
    setError('');
    if (!isAuthed) {
      setError('请先登录后再修改资料');
      return;
    }
    if (!form.nickName.trim()) {
      setError('昵称不能为空');
      return;
    }
    setSubmitting(true);
    try {
      const res = await studentApi.updateInfo(form);
      onSaved(res?.data || form);
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  };

  return (
    <Modal title="账号设置" onClose={onClose} busy={submitting || uploading}>
        <div className="space-y-6">
          <div>
            <label htmlFor="profile-name" className="block text-sm font-medium text-gray-600 mb-2.5">昵称</label>
            <input id="profile-name" type="text" value={form.nickName} onChange={update('nickName')} placeholder="你的昵称" className="input" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">头像</label>
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-gray-100 flex items-center justify-center text-2xl shrink-0 border border-gray-200 overflow-hidden">
                {form.avatar ? (
                  <img src={form.avatar} alt="avatar" className="w-full h-full object-cover" />
                ) : (
                  <span className="text-gray-400">📷</span>
                )}
              </div>
              <div className="flex-1">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  onChange={handleAvatarUpload}
                  disabled={submitting || uploading}
                  className="hidden"
                  id="avatar-upload"
                />
                <label
                  htmlFor="avatar-upload"
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-primary-600 bg-primary-50 hover:bg-primary-100 cursor-pointer transition-colors"
                >
                  {uploading ? (
                    <>
                      <span className="animate-spin">⏳</span> 上传中...
                    </>
                  ) : (
                    <>
                      <span>📤</span> 选择图片
                    </>
                  )}
                </label>
                <p className="text-xs text-gray-400 mt-1.5">支持 JPG/PNG/GIF/WebP，最大 10MB</p>
              </div>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="profile-college" className="block text-sm font-medium text-gray-600 mb-2.5">学院</label>
              <input id="profile-college" type="text" value={form.college} onChange={update('college')} placeholder="计算机学院" className="input" />
            </div>
            <div>
              <label htmlFor="profile-major" className="block text-sm font-medium text-gray-600 mb-2.5">专业</label>
              <input id="profile-major" type="text" value={form.major} onChange={update('major')} placeholder="软件工程" className="input" />
            </div>
          </div>
          <ErrorNotice error={error} />
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} disabled={submitting || uploading} className="btn-ghost">取消</button>
            <button onClick={handleSubmit} disabled={submitting || uploading} className="btn-primary px-8">
              {submitting ? '保存中...' : '保存'}
            </button>
          </div>
        </div>
    </Modal>
  );
}
