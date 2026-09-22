import { useState, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import useApiData from '../hooks/useApiData';
import { studentApi, uploadApi } from '../services';
import { mockUser } from '../services/mockData';

const fallbackStats = {
  postCount: 23,
  productCount: 6,
  favoriteCount: 45,
  followerCount: 128,
  followeeCount: 56,
};

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
  const { user, isAuthed, logout, updateUser } = useAuth();
  const [showSettings, setShowSettings] = useState(false);

  const { data: stats } = useApiData(() => studentApi.getStats(), fallbackStats, [], (p) => p || fallbackStats);

  const profile = user || mockUser;
  const avatarChar = profile.nickName?.[0] || 'C';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="max-w-4xl 2xl:max-w-6xl mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* User Card */}
      <div className="relative overflow-hidden bg-gradient-to-br from-primary-400 via-pink-400 to-sky-400 p-12 mb-10 text-white shadow-lg shadow-primary-100/50">
        <div className="relative z-10 flex items-center gap-7">
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

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-5 mb-10">
        {statCards.map((s) => (
          <div key={s.key} className="card card-hover p-6 text-center">
            <div className="text-2xl mb-2">{s.icon}</div>
            <div className="text-2xl font-bold text-gray-800">{stats[s.key] ?? 0}</div>
            <div className="text-xs text-gray-400 mt-1.5">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Menu */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4 mb-10">
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
          <button onClick={handleLogout} className="flex-1 px-6 py-2.5 text-sm font-medium text-red-500 bg-white border border-gray-100 hover:bg-red-50 hover:border-red-100 transition-colors">
            🚪 退出登录
          </button>
        )}
      </div>

      {!isAuthed && (
        <p className="text-center text-sm text-gray-400 mt-8">
          当前为游客预览 · <Link to="/login" className="text-primary-500 font-medium hover:underline">登录</Link> 查看你的真实数据
        </p>
      )}

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
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    setUploading(true);
    try {
      const res = await uploadApi.upload(file, 'avatar');
      setForm({ ...form, avatar: res?.data });
    } catch (err) {
      setError('头像上传失败: ' + (err?.message || '请重试'));
    } finally {
      setUploading(false);
      // 清空 input 以便重复选择同一文件
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async () => {
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
      setError(e?.message || '保存失败，请稍后再试');
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/30 backdrop-blur-sm animate-fade-in">
      <div className="bg-white w-full max-w-lg p-9 shadow-2xl animate-slide-up max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-8">
          <h3 className="text-xl font-bold text-gray-800">账号设置</h3>
          <button onClick={onClose} className="w-9 h-9 text-gray-300 hover:text-gray-500 hover:bg-gray-50 text-lg transition-colors">✕</button>
        </div>
        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">昵称</label>
            <input type="text" value={form.nickName} onChange={update('nickName')} placeholder="你的昵称" className="input" />
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
              <label className="block text-sm font-medium text-gray-600 mb-2.5">学院</label>
              <input type="text" value={form.college} onChange={update('college')} placeholder="计算机学院" className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">专业</label>
              <input type="text" value={form.major} onChange={update('major')} placeholder="软件工程" className="input" />
            </div>
          </div>
          {error && (
            <div className="text-sm text-red-500 bg-red-50 px-4 py-2.5">{error}</div>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} className="btn-ghost">取消</button>
            <button onClick={handleSubmit} disabled={submitting} className="btn-primary px-8">
              {submitting ? '保存中...' : '保存'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
