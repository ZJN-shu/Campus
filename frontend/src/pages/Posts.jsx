import { useState, useEffect, useRef } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { postApi, uploadApi } from '../services';
import { mockPosts } from '../services/mockData';
import { Loading, Empty } from '../components/ui';

const categories = ['全部', '学习交流', '校园生活', '技术交流', '经验分享', '二手交易'];
const sortOptions = [
  { value: 'hot', label: '最热' },
  { value: 'time', label: '最新' },
  { value: 'essence', label: '精华' },
];

export default function Posts() {
  const [searchParams] = useSearchParams();
  const mine = searchParams.get('mine') === '1';
  const initialCategory = searchParams.get('category') || '全部';
  const [activeCategory, setActiveCategory] = useState(initialCategory);
  const [activeSort, setActiveSort] = useState('hot');
  const [showPublishModal, setShowPublishModal] = useState(false);

  // 当 URL 上的 category 变化时同步
  useEffect(() => {
    setActiveCategory(searchParams.get('category') || '全部');
  }, [searchParams]);

  const { data: posts, loading, refetch } = useApiData(
    () =>
      mine
        ? postApi.getMy(1)
        : postApi.getByCategory(
            activeCategory === '全部' ? 'all' : activeCategory,
            1,
            activeSort
          ),
    mine ? [] : mockPosts,
    [activeCategory, activeSort, mine],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  return (
    <div className="max-w-4xl 2xl:max-w-5xl mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* Header */}
      <div className="flex items-center justify-between mb-12">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">{mine ? '我的帖子' : '社区论坛'}</h1>
          <p className="text-sm text-gray-400 mt-2">
            {mine ? (
              <><Link to="/posts" className="text-primary-500 hover:underline">← 返回全部帖子</Link></>
            ) : '分享你的校园故事'}
          </p>
        </div>
        <button onClick={() => setShowPublishModal(true)} className="btn-primary">
          ✏️ 发帖
        </button>
      </div>

      {/* Filters */}
      {!mine && (
      <div className="flex items-center justify-between gap-4 mb-10 flex-wrap">
        <div className="flex flex-wrap gap-2.5">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
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
      {loading ? (
        <Loading rows={4} />
      ) : posts.length === 0 ? (
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
                className="card card-hover block p-8 group"
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
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-pink-400 flex items-center justify-center text-white text-xs font-bold">
                      {author[0]}
                    </div>
                    <span className="text-sm text-gray-600">{author}</span>
                    <span className="text-xs text-gray-300">·</span>
                    <span className="text-xs text-gray-400">{post.createTime}</span>
                  </div>
                  <div className="flex items-center gap-5 text-xs text-gray-400">
                    <span>👀 {post.viewCount ?? 0}</span>
                    <span>❤️ {post.likeCount ?? 0}</span>
                    <span>💬 {post.commentCount ?? 0}</span>
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
            refetch();
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
      setError(e?.message || '发布失败，请先登录或稍后再试');
      setSubmitting(false);
    }
  };

  // 图片上传
  const handleImageUpload = async (e) => {
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
      const urls = res?.data || [];
      setForm({ ...form, images: [...form.images, ...urls] });
    } catch (err) {
      setError('图片上传失败: ' + (err?.message || '请重试'));
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/30 backdrop-blur-sm animate-fade-in">
      <div className="bg-white w-full max-w-lg p-9 shadow-2xl animate-slide-up">
        <div className="flex items-center justify-between mb-8">
          <h3 className="text-xl font-bold text-gray-800">发布新帖子</h3>
          <button onClick={onClose} className="w-9 h-9 text-gray-300 hover:text-gray-500 hover:bg-gray-50 text-lg transition-colors">✕</button>
        </div>
        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">标题</label>
            <input
              type="text"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="请输入标题..."
              className="input"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">板块</label>
            <select
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
            <label className="block text-sm font-medium text-gray-600 mb-2.5">内容</label>
            <textarea
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
          {error && (
            <div className="text-sm text-red-500 bg-red-50 rounded-xl px-4 py-2.5">{error}</div>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} className="btn-ghost">取消</button>
            <button onClick={handleSubmit} disabled={submitting} className="btn-primary px-8">
              {submitting ? '发布中...' : '发布'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
