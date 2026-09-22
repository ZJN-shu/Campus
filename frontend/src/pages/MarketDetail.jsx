import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { productApi } from '../services';
import { mockProductDetail, mockProducts } from '../services/mockData';
import { useAuth } from '../context/AuthContext';
import { Loading } from '../components/ui';

const resolveImages = (p) => {
  const raw = p?.images ?? p?.imageList;
  let arr = [];
  if (Array.isArray(raw)) arr = raw;
  else if (typeof raw === 'string') {
    try { const parsed = JSON.parse(raw); arr = Array.isArray(parsed) ? parsed : [raw]; }
    catch { arr = raw.startsWith('http') ? [raw] : []; }
  }
  if (p?.firstImage) arr = [p.firstImage, ...arr.filter((u) => u !== p.firstImage)];
  if (arr.length === 0) arr = [`https://picsum.photos/600/600?random=${p?.id || 1}`];
  return arr;
};

export default function MarketDetail() {
  const { id } = useParams();
  const { isAuthed } = useAuth();
  const [activeImg, setActiveImg] = useState(0);
  const [favorited, setFavorited] = useState(false);
  const [favCount, setFavCount] = useState(0);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const { data: product, loading } = useApiData(
    () => productApi.getById(id),
    { ...mockProductDetail, id: Number(id) },
    [id],
    (payload) => payload || null
  );

  const { data: related } = useApiData(
    () => productApi.getByCategory(product?.category || 'all', 1, 'time'),
    mockProducts,
    [product?.category],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const images = resolveImages(product);
  const count = favCount || product?.favoriteCount || 0;

  const toggleFavorite = async () => {
    setMsg('');
    const next = !favorited;
    setFavorited(next);
    setFavCount(Math.max(0, count + (next ? 1 : -1)));
    setBusy(true);
    try {
      if (next) await productApi.favorite(id);
      else await productApi.unfavorite(id);
    } catch (e) {
      setMsg(e?.message || '请先登录后再收藏');
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
        <Loading rows={2} />
      </div>
    );
  }

  const seller = product.sellerName || '同学';
  const relatedList = related.filter((p) => String(p.id) !== String(id)).slice(0, 4);

  return (
    <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-12">
      <Link to="/market" className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 mb-8 transition-colors">
        ← 返回二手市场
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mb-16">
        {/* 图集 */}
        <div>
          <div className="card overflow-hidden aspect-square bg-gray-50">
            <img src={images[activeImg]} alt={product.title} className="w-full h-full object-cover" />
          </div>
          {images.length > 1 && (
            <div className="flex gap-3 mt-4">
              {images.map((img, i) => (
                <button
                  key={i}
                  onClick={() => setActiveImg(i)}
                  className={`w-20 h-20 overflow-hidden border-2 transition-all ${
                    i === activeImg ? 'border-primary-400' : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 信息 */}
        <div className="flex flex-col">
          <div className="flex items-center gap-2.5 mb-5 flex-wrap">
            {product.category && <span className="tag bg-purple-50 text-purple-500">{product.category}</span>}
            {product.quality && <span className="tag bg-gray-50 text-gray-500">{product.quality}</span>}
          </div>

          <h1 className="text-2xl md:text-3xl font-bold text-gray-800 leading-tight mb-6">{product.title}</h1>

          <div className="flex items-baseline gap-3 mb-8 pb-8 border-b border-gray-100">
            <span className="text-4xl font-bold text-pink-500">¥{product.price}</span>
            {product.originalPrice && (
              <span className="text-base text-gray-300 line-through">¥{product.originalPrice}</span>
            )}
            {product.originalPrice && (
              <span className="tag bg-pink-50 text-pink-500 ml-1">
                省 ¥{Math.max(0, Number(product.originalPrice) - Number(product.price)).toFixed(0)}
              </span>
            )}
          </div>

          <div className="space-y-4 text-sm mb-8">
            <div className="flex gap-3">
              <span className="text-gray-400 shrink-0 w-20">📍 交易地点</span>
              <span className="text-gray-600">{product.location || '校内当面交易'}</span>
            </div>
            <div className="flex gap-3">
              <span className="text-gray-400 shrink-0 w-20">🕒 发布时间</span>
              <span className="text-gray-600">{product.createTime || '—'}</span>
            </div>
            <div className="flex gap-3">
              <span className="text-gray-400 shrink-0 w-20">👀 浏览</span>
              <span className="text-gray-600">{product.viewCount ?? 0} 次 · ❤️ {count} 人收藏</span>
            </div>
          </div>

          {/* 卖家 */}
          <div className="card p-6 flex items-center gap-4 mb-8">
            <div className="w-12 h-12 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-white font-bold shrink-0">
              {product.sellerAvatar ? (
                <img src={product.sellerAvatar} alt="" className="w-full h-full object-cover rounded-full" />
              ) : (
                seller[0]
              )}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold text-gray-700 truncate">{seller}</div>
              <div className="text-xs text-gray-400 mt-0.5">⭐ 信用分 {product.sellerCredit ?? 100}</div>
            </div>
            <span className="tag bg-campus-50 text-campus-500 shrink-0">已认证</span>
          </div>

          {msg && <div className="text-sm px-4 py-3 mb-5 bg-primary-50 text-primary-600">{msg}</div>}

          <div className="flex gap-4 mt-auto">
            <button
              onClick={toggleFavorite}
              disabled={busy}
              className={`flex-1 py-3.5 text-sm font-semibold border transition-all ${
                favorited
                  ? 'bg-pink-50 text-pink-500 border-pink-200'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-pink-200 hover:text-pink-500'
              }`}
            >
              {favorited ? '❤️ 已收藏' : '🤍 收藏'}
            </button>
            {isAuthed ? (
              <button
                onClick={() => setMsg('已提醒卖家，请留意站内消息～')}
                className="btn-primary flex-[2]"
              >
                💬 联系卖家 · 我想要
              </button>
            ) : (
              <Link to="/login" className="btn-primary flex-[2] text-center">登录后购买</Link>
            )}
          </div>
        </div>
      </div>

      {/* 描述 */}
      <section className="card p-8 md:p-10 mb-16">
        <h2 className="section-title mb-6">宝贝描述</h2>
        <p className="text-gray-600 leading-loose whitespace-pre-line text-[15px]">
          {product.description || '卖家没有填写更多描述。'}
        </p>
      </section>

      {/* 相关推荐 */}
      {relatedList.length > 0 && (
        <section>
          <h2 className="section-title mb-7">相似好物</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {relatedList.map((p) => (
              <Link key={p.id} to={`/market/${p.id}`} className="card card-hover overflow-hidden group">
                <div className="aspect-square overflow-hidden bg-gray-50">
                  <img
                    src={resolveImages(p)[0]}
                    alt={p.title}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
                <div className="p-5">
                  <h3 className="text-sm font-medium text-gray-700 line-clamp-2 mb-3 leading-snug min-h-[2.5rem] group-hover:text-purple-600 transition-colors">
                    {p.title}
                  </h3>
                  <span className="text-lg font-bold text-pink-500">¥{p.price}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
