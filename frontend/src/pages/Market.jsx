import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { productApi, favoriteApi } from '../services';
import { mockProducts } from '../services/mockData';
import { GridLoading, Empty } from '../components/ui';

const productCategories = ['全部', '数码', '教材', '生活'];

const resolveImage = (p) =>
  p.firstImage || p.imageList?.[0] || (typeof p.images === 'string' && p.images.startsWith('http') ? p.images : null) ||
  `https://picsum.photos/400/400?random=${p.id || 1}`;

export default function Market() {
  const [searchParams, setSearchParams] = useSearchParams();
  const mine = searchParams.get('mine') === '1';
  const fav = searchParams.get('fav') === '1';
  const personal = mine || fav;
  const [activeCategory, setActiveCategory] = useState('全部');
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [inputValue, setInputValue] = useState(searchParams.get('keyword') || '');
  const [showPublishModal, setShowPublishModal] = useState(false);

  useEffect(() => {
    const kw = searchParams.get('keyword') || '';
    setKeyword(kw);
    setInputValue(kw);
  }, [searchParams]);

  const { data: products, loading, refetch } = useApiData(
    async () => {
      if (mine) return productApi.getMy(1);
      if (fav) {
        const favRes = await favoriteApi.getMyFavorites('product', 1);
        const ids = (favRes?.data || []).map((f) => f.targetId);
        const items = await Promise.all(
          ids.map((id) => productApi.getById(id).then((r) => r?.data).catch(() => null))
        );
        return { data: items.filter(Boolean) };
      }
      return keyword
        ? productApi.search(keyword, 1)
        : productApi.getByCategory(activeCategory === '全部' ? 'all' : activeCategory, 1, 'time');
    },
    personal ? [] : mockProducts,
    [activeCategory, keyword, mine, fav],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const handleSearch = (e) => {
    e.preventDefault();
    if (inputValue.trim()) {
      setSearchParams({ keyword: inputValue.trim() });
    } else {
      setSearchParams({});
    }
  };

  return (
    <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* Header */}
      <div className="flex items-center justify-between mb-12">
        <div>
          {personal && (
            <Link to="/market" className="inline-flex items-center gap-1 text-sm text-gray-400 hover:text-gray-600 mb-3 transition-colors">
              ← 返回二手市场
            </Link>
          )}
          <h1 className="text-2xl font-bold text-gray-800">
            {mine ? '我的商品' : fav ? '我的收藏' : '二手市场'}
          </h1>
          <p className="text-sm text-gray-400 mt-2">
            {mine ? '你发布的所有宝贝' : fav ? '你收藏的心仪好物' : '淘好物，享实惠'}
          </p>
        </div>
        <button
          onClick={() => setShowPublishModal(true)}
          className="px-6 py-2.5 bg-gradient-to-r from-purple-400 to-pink-400 text-white text-sm font-semibold hover:shadow-lg hover:-translate-y-0.5 transition-all"
        >
          📦 发布商品
        </button>
      </div>

      {!personal && (
        <>
          {/* Search */}
          <form onSubmit={handleSearch} className="relative mb-10">
            <input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="搜索你想要的宝贝..."
              className="input !py-4 !pl-13 "
              style={{ paddingLeft: '3.25rem' }}
            />
            <span className="absolute left-5 top-1/2 -translate-y-1/2 text-base">🔍</span>
            {keyword && (
              <button
                type="button"
                onClick={() => setSearchParams({})}
                className="absolute right-5 top-1/2 -translate-y-1/2 text-xs text-gray-400 hover:text-gray-600"
              >
                清除 ✕
              </button>
            )}
          </form>

          {/* Filters */}
          <div className="flex flex-wrap gap-2.5 mb-12">
            {productCategories.map((cat) => (
              <button
                key={cat}
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
        </>
      )}

      {/* Product Grid */}
      {loading ? (
        <GridLoading count={8} />
      ) : products.length === 0 ? (
        <Empty
          icon={mine ? '📦' : fav ? '💜' : '🛍️'}
          title={mine ? '你还没有发布过商品' : fav ? '还没有收藏任何宝贝' : '没有找到相关宝贝'}
          desc={mine ? '点击右上角发布你的第一件闲置吧～' : fav ? '去二手市场淘一淘，遇到喜欢的就收藏吧' : '换个关键词或分类试试吧'}
        />
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-6">
          {products.map((product) => (
            <Link key={product.id} to={`/market/${product.id}`} className="card card-hover overflow-hidden group">
              <div className="relative aspect-square overflow-hidden bg-gray-50">
                <img
                  src={resolveImage(product)}
                  alt={product.title}
                  loading="lazy"
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
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
                  <span className="text-lg font-bold text-pink-500">¥{product.price}</span>
                  {product.originalPrice && (
                    <span className="text-xs text-gray-300 line-through">¥{product.originalPrice}</span>
                  )}
                </div>
                <div className="flex items-center justify-between text-xs text-gray-400">
                  <span className="truncate">{product.sellerName || '同学'}</span>
                  <span className="shrink-0">❤️ {product.favoriteCount ?? 0}</span>
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
            refetch();
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
    setError('');
    if (!form.title.trim() || !form.price) {
      setError('请填写商品标题和价格');
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
      setError(e?.message || '发布失败，请先登录或稍后再试');
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/30 backdrop-blur-sm animate-fade-in">
      <div className="bg-white w-full max-w-lg p-9 shadow-2xl animate-slide-up max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-8">
          <h3 className="text-xl font-bold text-gray-800">发布二手商品</h3>
          <button onClick={onClose} className="w-9 h-9 text-gray-300 hover:text-gray-500 hover:bg-gray-50 text-lg transition-colors">✕</button>
        </div>
        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">商品标题</label>
            <input type="text" value={form.title} onChange={update('title')} placeholder="例如：九成新 iPad Air 5" className="input" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">分类</label>
              <select value={form.category} onChange={update('category')} className="input">
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">成色</label>
              <select value={form.quality} onChange={update('quality')} className="input">
                {['全新', '95新', '9成新', '8成新', '7成新'].map((q) => (
                  <option key={q} value={q}>{q}</option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">商品描述</label>
            <textarea rows={3} value={form.description} onChange={update('description')} placeholder="描述一下商品的成色、使用情况..." className="input resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">售价 (元)</label>
              <input type="number" value={form.price} onChange={update('price')} placeholder="2800" className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2.5">原价 (元)</label>
              <input type="number" value={form.originalPrice} onChange={update('originalPrice')} placeholder="4799" className="input" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2.5">交易地点</label>
            <input type="text" value={form.location} onChange={update('location')} placeholder="例如：6号宿舍楼下" className="input" />
          </div>
          {error && (
            <div className="text-sm text-red-500 bg-red-50 px-4 py-2.5">{error}</div>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={onClose} className="btn-ghost">取消</button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="px-8 py-2.5 bg-gradient-to-r from-purple-400 to-pink-400 text-white text-sm font-semibold hover:shadow-lg transition-all disabled:opacity-60"
            >
              {submitting ? '发布中...' : '发布商品'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
