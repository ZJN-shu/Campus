import { useState, useRef } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { postApi, uploadApi } from '../services';
import { useAuth } from '../context/AuthContext';
import { Loading, Empty, DataStatus, PageHeader, Modal, ErrorNotice, LoginPrompt } from '../components/ui';

const categories = ['全部', '学习交流', '校园生活', '技术交流', '经验分享', '二手交易'];
const sortOptions = [
  { value: 'hot', label: '最热' },
  { value: 'time', label: '最新' },
  { value: 'essence', label: '精华' },
];

export default function Posts() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthed, token } = useAuth();
  const mine = searchParams.get('mine') === '1';
  const activeCategory = searchParams.get('category') || '全部';
  const [activeSort, setActiveSort] = useState('hot');
  const [showPublishModal, setShowPublishModal] = useState(false);

  const [notice, setNotice] = useState('');
  const state = useApiData(
    () =>
      mine
        ? (isAuthed ? postApi.getMy(1) : Promise.resolve({ data: [] }))
        : postApi.getByCategory(
            activeCategory === '全部' ? 'all' : activeCategory,
            1,
            activeSort
          ),
    [],
    [activeCategory, activeSort, mine, token],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const { data: posts, loading, refetch, error, lastUpdatedAt } = state;
  if (mine && !isAuthed) return <div className="page-container"><LoginPrompt /></div>;

  return (
    <div className="page-container">
      {/* Header */}
      <PageHeader title={mine ? '我的帖子' : '社区论坛'} description={mine ? <Link to="/posts">← 返回全部帖子</Link> : '分享校园故事，发现有用的讨论'}>
        <button onClick={() => isAuthed ? setShowPublishModal(true) : navigate('/login')} className="btn-primary">✏️ 发帖</button>
      </PageHeader>
      {notice && <p role="status" className="bg-green-50 text-green-800 p-3 mb-4 text-sm">{notice}</p>}

      {/* Filters */}
      {!mine && (
      <div className="card p-4 flex items-center justify-between gap-4 mb-4 flex-wrap">
        <div className="flex flex-wrap gap-2.5">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSearchParams(cat === '全部' ? {} : { category: cat })}
                            aria-pressed={activeCategory === cat}
              className={`px-4 py-2 text-sm font-medium transition-all ${
                activeCategory === cat
                  ? 'bg-gray-800 text-white shadow-sm'
                  : 'bg-white text-gray-500 hover:bg-gray-50 border border-gray-100'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
        <div className="flex gap-1 bg-white p-1.5 border border-gray-100">
          {sortOptions.map((opt) => (
            <button
              key={opt.value}
              onClick={() => setActiveSort(opt.value)}
                            aria-pressed={activeSort === opt.value}
              className={`px-3.5 py-1.5 text-xs font-semibold transition-colors ${
                activeSort === opt.value ? 'bg-primary-50 text-primary-600' : 'text-gray-400 hover:text-gray-600'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>
      )}

      {/* Post List */}
      <DataStatus {...state} count={posts.length} label={mine ? '我的帖子 · 当前页' : `${activeCategory} · ${sortOptions.find((s) => s.value === activeSort)?.label} · 当前页`} />
      {loading && !lastUpdatedAt ? (
        <Loading rows={4} />
      ) : error && !lastUpdatedAt ? null : posts.length === 0 ? (
        <Empty icon="💬" title={mine ? '你还没有发过帖子' : '还没有帖子'} desc={mine ? '点击右上角发帖，分享你的第一篇内容吧～' : '成为第一个发帖的人吧～'} />
      ) : (
        <div className="space-y-5">
          {posts.map((post) => {
            const tags = Array.isArray(post.tags) ? post.tags : (post.tags ? String(post.tags).split(',') : []);
            const author = post.authorName || post.userName || '匿名同学';
            return (
              <Link
                key={post.id}
                to={`/posts/${post.id}`}
                className="card card-hover block p-5 sm:p-6 group"
              >
                {/* Tags */}
                <div className="flex items-center gap-2 mb-4 flex-wrap">
                  {!!post.isTop && <span className="tag bg-red-50 text-red-500">📌 置顶</span>}
                  {!!post.isEssence && <span className="tag bg-amber-50 text-amber-600">⭐ 精华</span>}
                  {post.category && <span className="tag bg-gray-50 text-gray-500">{post.category}</span>}
                </div>

                {/* Title */}
                <h2 className="text-lg font-bold text-gray-800 group-hover:text-primary-600 transition-colors mb-3 leading-snug">
                  {post.title}
                </h2>

                {/* Content */}
                <p className="text-sm text-gray-400 leading-relaxed mb-6 line-clamp-2">
                  {post.content}
                </p>

                {/* Footer */}
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2.5 min-w-0">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-pink-400 flex items-center justify-center text-white text-xs font-bold">
                      {author[0]}
                    </div>
                    <span className="text-sm text-gray-600">{author}</span>
                    <span className="text-xs text-gray-300">·</span>
                    <span className="text-xs text-gray-400">{post.createTime}</span>
                  </div>
                  <div className="flex items-center gap-5 text-xs text-gray-400">
                    <span>👀 {post.viewCount ?? '—'}</span>
                    <span>❤️ {post.likeCount ?? '—'}</span>
                    <span>💬 {post.commentCount ?? '—'}</span>
                  </div>
                </div>
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-5 pt-5 border-t border-gray-50">
                    {tags.map((t) => (
                      <span key={t} className="text-xs text-gray-400">#{t}</span>
                    ))}
                  </div>
                )}
              </Link>
            );
          })}
        </div>
      )}

      {/* Publish Modal */}
      {showPublishModal && (
        <PublishModal
          categories={categories.filter((c) => c !== '全部')}
          onClose={() => setShowPublishModal(false)}
          onPublished={() => {
            setShowPublishModal(false);
            setNotice('发布成功；正在获取最新列表。');
            refetch().then((result) => setNotice(result.success ? '发布成功，列表已更新。' : '发布成功，但列表更新失败，请点击重试；无需再次发布。'));
          }}
        />
      )}
    </div>
  );
}

function PublishModal({ categories, onClose, onPublished }) {
  const [form, setForm] = useState({ title: '', category: categories[0], content: '', images: [] });
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const handleSubmit = async () => {
    if (submitting || uploading) return;
    setError('');
    if (!form.title.trim() || !form.content.trim()) {
      setError('标题和内容不能为空');
      return;
    }
    setSubmitting(true);
    try {
      await postApi.create({
        title: form.title,
        category: form.category,
        content: form.content,
        images: form.images,
      });
      onPublished();
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  };

  // 图片上传
  const handleImageUpload = async (e) => {
    if (uploading || submitting) return;
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    if (form.images.length + files.length > 9) {
      setError('最多上传 9 张图片');
      return;
    }
    setError('');
    setUploading(true);
    try {
      const res = await uploadApi.uploadBatch(files, 'post');
      const urls = res?.data;
      if (!Array.isArray(urls) || urls.length === 0) throw new Error('上传未返回图片地址，请重试');
      setForm((prev) => ({ ...prev, images: [...prev.images, ...urls] }));
    } catch (err) {
      setError(err);
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // 移除图片
  const removeImage = (index) => {
    setForm({ ...form, images: form.images.filter((_, i) => i !== index) });
  };

  return (
    <Modal title="发布新帖子" onClose={onClose} busy={submitting || uploading}>
        <div className="space-y-6">
          <div>
            <label htmlFor="post-title" className="block text-sm font-medium text-gray-600 mb-2.5">标题</label>
            <input
              id="post-title"
              type="text"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="请输入标题..."
              className="input"
            />
          </div>
          <div>
            <label htmlFor="post-category" className="block text-sm font-medium text-gray-600 mb-2.5">板块</label>
            <select
              id="post-category"
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
              className="input"
            >
              {categories.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="post-content" className="block text-sm font-medium text-gray-600 mb-2.5">内容</label>
            <textarea
              id="post-content"
              rows={5}
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              placeholder="分享你的想法..."
              className="input resize-none"
            />
          </div>
          {/* 图片上传 */}
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">图片（可选，最多 9 张）</label>
            <div className="flex flex-wrap gap-3">
              {form.images.map((url, index) => (
                <div key={index} className="relative w-20 h-20 bg-gray-100 border border-gray-200 overflow-hidden group">
                  <img src={url} alt="" className="w-full h-full object-cover" />
                  <button
                    type="button"
                    aria-label={`移除第 ${index + 1} 张图片`}
                    disabled={uploading || submitting}
                    onClick={() => removeImage(index)}
                    className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center text-white text-lg transition-opacity"
                  >
                    ✕
                  </button>
                </div>
              ))}
              {form.images.length < 9 && (
                <>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    multiple
                    disabled={uploading || submitting}
                    onChange={handleImageUpload}
                    className="hidden"
                    id="post-image-upload"
                  />
                  <label
                    htmlFor="post-image-upload"
                    className="w-20 h-20 border-2 border-dashed border-gray-300 flex flex-col items-center justify-center text-gray-400 hover:border-primary-400 hover:text-primary-500 cursor-pointer transition-colors"
                  >
                    {uploading ? (
                      <span className="animate-spin text-lg">⏳</span>
                    ) : (
                      <>
                        <span className="text-2xl">+</span>
                        <span className="text-[10px] mt-0.5">添加图片</span>
                      </>
                    )}
                  </label>
                </>
              )}
            </div>
          </div>
          <ErrorNotice error={error} />
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} disabled={submitting || uploading} className="btn-ghost">取消</button>
            <button onClick={handleSubmit} disabled={submitting || uploading} className="btn-primary px-8">
              {submitting ? '发布中...' : '发布'}
            </button>
          </div>
        </div>
    </Modal>
  );
}
