import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { productApi, favoriteApi } from '../services';
import { useAuth } from '../context/AuthContext';
import { GridLoading, Empty, DataStatus, PageHeader, Modal, ErrorNotice, LoginPrompt, ProductImage } from '../components/ui';

const productCategories = ['全部', '数码', '教材', '生活'];

const resolveImage = (p) => {
  if (p.firstImage) return p.firstImage;
  if (Array.isArray(p.imageList)) return p.imageList[0];
  if (Array.isArray(p.images)) return p.images[0];
  if (typeof p.images === 'string') {
    try { const list = JSON.parse(p.images); return Array.isArray(list) ? list[0] : null; }
    catch { return p.images.startsWith('http') ? p.images : null; }
  }
  return null;
};

export default function Market() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthed, token } = useAuth();
  const [notice, setNotice] = useState('');
  const mine = searchParams.get('mine') === '1';
  const fav = searchParams.get('fav') === '1';
  const personal = mine || fav;
  const [activeCategory, setActiveCategory] = useState('全部');
  const keyword = searchParams.get('keyword') || '';
  const [showPublishModal, setShowPublishModal] = useState(false);

  const state = useApiData(
    async () => {
      if (personal && !isAuthed) return { data: [] };
      if (mine) return productApi.getMy(1);
      if (fav) {
        const favRes = await favoriteApi.getMyFavorites('product', 1);
        const ids = (favRes?.data || []).map((f) => f.targetId);
        const items = await Promise.all(
          ids.map((id) => productApi.getById(id).then((r) => r?.data))
        );
        return { data: items.filter(Boolean) };
      }
      return keyword
        ? productApi.search(keyword, 1)
        : productApi.getByCategory(activeCategory === '全部' ? 'all' : activeCategory, 1, 'time');
    },
    [],
    [activeCategory, keyword, mine, fav, token],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const handleSearch = (e) => {
    e.preventDefault();
    const value = String(new FormData(e.currentTarget).get('keyword') || '').trim();
    if (value) {
      setSearchParams({ keyword: value });
    } else {
      setSearchParams({});
    }
  };

  const { data: products, loading, refetch, error, lastUpdatedAt } = state;
  if (personal && !isAuthed) return <div className="page-container"><LoginPrompt /></div>;

  return (
    <div className="page-container">
      {/* Header */}
      <PageHeader title={mine ? '我的商品' : fav ? '我的收藏' : '二手市场'} description={personal ? <Link to="/market">← 返回二手市场</Link> : '让闲置找到新主人，交易前请确认成色和地点'}>
        <button onClick={() => isAuthed ? setShowPublishModal(true) : navigate('/login')} className="btn-primary">📦 发布商品</button>
      </PageHeader>
      {notice && <p role="status" className="bg-green-50 text-green-800 p-3 mb-4 text-sm">{notice}</p>}

      {!personal && (
        <div className="card p-4 mb-4">
          {/* Search */}
          <form onSubmit={handleSearch} className="relative mb-4 flex gap-3">
            <input
              type="text"
              key={keyword}
              name="keyword"
              aria-label="搜索二手商品"
              defaultValue={keyword}
              placeholder="搜索你想要的宝贝..."
              className="input pl-10 pr-16 min-w-0"
            />
            <span aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-base">🔍</span>
                        <button type="submit" className="btn-primary shrink-0">搜索</button>
            {keyword && (
              <button
                type="button"
                onClick={() => setSearchParams({})}
                className="absolute right-28 top-1/2 -translate-y-1/2 text-xs text-gray-600 hover:underline"
              >
                清除 ✕
              </button>
            )}
          </form>

          {/* Filters */}
          <div className="flex flex-wrap gap-2.5">
            {productCategories.map((cat) => (
              <button
                key={cat}
                aria-pressed={activeCategory === cat && !keyword}
                onClick={() => {
                  setActiveCategory(cat);
                  setSearchParams({});
                }}
                className={`px-5 py-2 text-sm font-medium transition-all ${
                  activeCategory === cat && !keyword
                    ? 'bg-gray-800 text-white shadow-sm'
                    : 'bg-white text-gray-500 hover:bg-gray-50 border border-gray-100'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Product Grid */}
      <DataStatus {...state} count={products.length} label={`${keyword ? `搜索：${keyword}` : personal ? '我的商品列表' : activeCategory} · 当前页`} />
      {loading && !lastUpdatedAt ? (
        <GridLoading count={8} />
      ) : error && !lastUpdatedAt ? null : products.length === 0 ? (
        <Empty
          icon={mine ? '📦' : fav ? '💜' : '🛍️'}
          title={mine ? '你还没有发布过商品' : fav ? '还没有收藏任何宝贝' : '没有找到相关宝贝'}
          desc={mine ? '点击右上角发布你的第一件闲置吧～' : fav ? '去二手市场淘一淘，遇到喜欢的就收藏吧' : '换个关键词或分类试试吧'}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {products.map((product) => (
            <Link key={product.id} to={`/market/${product.id}`} className="card card-hover overflow-hidden group">
              <div className="relative aspect-square overflow-hidden bg-gray-50">
                <ProductImage
                  src={resolveImage(product)}
                  alt={product.title}
                  className="group-hover:scale-105 transition-transform duration-500"
                />
                {product.quality && (
                  <span className="absolute top-3 left-3 px-2.5 py-1 bg-white/90 backdrop-blur text-xs text-gray-600 font-semibold">
                    {product.quality}
                  </span>
                )}
              </div>
              <div className="p-5">
                <h3 className="text-sm font-medium text-gray-700 line-clamp-2 mb-4 leading-snug min-h-[2.5rem] group-hover:text-purple-600 transition-colors">
                  {product.title}
                </h3>
                <div className="flex items-baseline gap-2 mb-3">
                  <span className="text-lg font-bold text-pink-500">¥{product.price ?? '—'}</span>
                  {product.originalPrice && (
                    <span className="text-xs text-gray-300 line-through">¥{product.originalPrice}</span>
                  )}
                </div>
                <p className="text-xs text-gray-600 mb-3 truncate">📍 {product.location || '地点未填写'}</p>
                <div className="flex items-center justify-between gap-2 text-xs text-gray-400">
                  <span className="truncate">{product.sellerName || '同学'}</span>
                  <span className="shrink-0">❤️ {product.favoriteCount ?? '—'}</span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Publish Modal */}
      {showPublishModal && (
        <PublishProductModal
          categories={productCategories.filter((c) => c !== '全部')}
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

function PublishProductModal({ categories, onClose, onPublished }) {
  const [form, setForm] = useState({
    title: '',
    category: categories[0],
    description: '',
    price: '',
    originalPrice: '',
    quality: '9成新',
    location: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async () => {
    if (submitting) return;
    setError('');
    if (!form.title.trim() || !Number.isFinite(Number(form.price)) || Number(form.price) <= 0) {
      setError('请填写商品标题和大于零的价格');
      return;
    }
    setSubmitting(true);
    try {
      await productApi.publish({
        title: form.title,
        category: form.category,
        description: form.description,
        price: Number(form.price),
        originalPrice: form.originalPrice ? Number(form.originalPrice) : null,
        quality: form.quality,
        location: form.location,
        stock: 1,
      });
      onPublished();
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  };

  return (
    <Modal title="发布二手商品" onClose={onClose} busy={submitting}>
        <div className="space-y-6">
          <div>
            <label htmlFor="product-title" className="block text-sm font-medium text-gray-600 mb-2.5">商品标题</label>
            <input id="product-title" type="text" value={form.title} onChange={update('title')} placeholder="例如：九成新 iPad Air 5" className="input" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="product-category" className="block text-sm font-medium text-gray-600 mb-2.5">分类</label>
              <select id="product-category" value={form.category} onChange={update('category')} className="input">
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="product-quality" className="block text-sm font-medium text-gray-600 mb-2.5">成色</label>
              <select id="product-quality" value={form.quality} onChange={update('quality')} className="input">
                {['全新', '95新', '9成新', '8成新', '7成新'].map((q) => (
                  <option key={q} value={q}>{q}</option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label htmlFor="product-description" className="block text-sm font-medium text-gray-600 mb-2.5">商品描述</label>
            <textarea id="product-description" rows={3} value={form.description} onChange={update('description')} placeholder="描述一下商品的成色、使用情况..." className="input resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="product-price" className="block text-sm font-medium text-gray-600 mb-2.5">售价 (元)</label>
              <input id="product-price" min="0.01" step="0.01" type="number" value={form.price} onChange={update('price')} placeholder="2800" className="input" />
            </div>
            <div>
              <label htmlFor="product-original-price" className="block text-sm font-medium text-gray-600 mb-2.5">原价 (元)</label>
              <input id="product-original-price" min="0" step="0.01" type="number" value={form.originalPrice} onChange={update('originalPrice')} placeholder="4799" className="input" />
            </div>
          </div>
          <div>
            <label htmlFor="product-location" className="block text-sm font-medium text-gray-600 mb-2.5">交易地点</label>
            <input id="product-location" type="text" value={form.location} onChange={update('location')} placeholder="例如：6号宿舍楼下" className="input" />
          </div>
          <ErrorNotice error={error} />
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} disabled={submitting} className="btn-ghost">取消</button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="btn-primary"
            >
              {submitting ? '发布中...' : '发布商品'}
            </button>
          </div>
        </div>
    </Modal>
  );
}
