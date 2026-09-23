import { useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { productApi } from '../services';
import { useAuth } from '../context/AuthContext';
import { Loading, Empty, DataStatus, ErrorNotice, ProductImage } from '../components/ui';

const resolveImages = (p) => {
  const raw = p?.images ?? p?.imageList;
  let arr = [];
  if (Array.isArray(raw)) arr = raw;
  else if (typeof raw === 'string') {
    try { const parsed = JSON.parse(raw); arr = Array.isArray(parsed) ? parsed : [raw]; }
    catch { arr = raw.startsWith('http') ? [raw] : []; }
  }
  if (p?.firstImage) arr = [p.firstImage, ...arr.filter((u) => u !== p.firstImage)];
  return arr.filter((url) => typeof url === 'string' && url.length > 0);
};

export default function MarketDetail() {
  const { id } = useParams();
  const { isAuthed, token } = useAuth();
  const [activeImg, setActiveImg] = useState(0);
  const actionLock = useRef(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);

  const state = useApiData(
    () => productApi.getById(id),
    null,
    [id, token],
    (payload) => payload || null
  );

  const { data: product, loading, lastUpdatedAt } = state;
  const relatedState = useApiData(
    () => product ? productApi.getByCategory(product.category || 'all', 1, 'time') : Promise.resolve({ data: [] }),
    [],
    [product?.category, product?.id],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const favoriteState = useApiData(
    () => isAuthed ? productApi.isFavorited(id) : Promise.resolve({ data: null }),
    null,
    [id, token],
    (value) => typeof value === 'boolean' ? value : null
  );
  const favorited = favoriteState.data === true;
  const images = resolveImages(product);
  const count = product?.favoriteCount ?? '—';

  const toggleFavorite = async () => {
    if (actionLock.current) return;
    if (!isAuthed) {
      setActionError(Object.assign(new Error('请先登录后收藏'), { status: 401 }));
      return;
    }
    actionLock.current = true;
    setActionError(null);
    const next = !favorited;
    setBusy(true);
    try {
      if (next) await productApi.favorite(id);
      else await productApi.unfavorite(id);
      favoriteState.setData(next);
      state.setData((prev) => prev && ({ ...prev, favoriteCount: prev.favoriteCount == null ? null : Math.max(0, Number(prev.favoriteCount) + (next ? 1 : -1)) }));
    } catch (e) {
      setActionError(e);
    } finally {
      actionLock.current = false;
      setBusy(false);
    }
  };

  if (loading && !lastUpdatedAt) {
    return (
      <div className="page-container">
        <Loading rows={2} />
      </div>
    );
  }

  if (!product) return <div className="page-container"><DataStatus {...state} />{!state.error && <Empty title="商品不存在或已下架" />}</div>;

  const seller = product.sellerName || '同学';
  const relatedList = relatedState.data.filter((p) => String(p.id) !== String(id)).slice(0, 4);

  return (
    <div className="page-container">
      <Link to="/market" className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 mb-8 transition-colors">
        ← 返回二手市场
      </Link>

      <DataStatus {...state} label="商品详情" />
      <ErrorNotice error={actionError} />
      <ErrorNotice error={favoriteState.error} onRetry={favoriteState.refetch} />
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* 图集 */}
        <div>
          <div className="card overflow-hidden aspect-square bg-gray-50">
            <ProductImage src={images[activeImg] || images[0]} alt={product.title} />
          </div>
          {images.length > 1 && (
            <div className="flex flex-wrap gap-3 mt-4">
              {images.map((img, i) => (
                <button
                  key={i}
                  onClick={() => setActiveImg(i)}
                  aria-label={`查看第 ${i + 1} 张图片`}
                  aria-pressed={activeImg === i}
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
            <span className="text-4xl font-bold text-pink-500">¥{product.price ?? '—'}</span>
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
              <span className="text-gray-600">{product.viewCount ?? '—'} 次 · ❤️ {count} 人收藏</span>
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
              <div className="text-xs text-gray-400 mt-0.5">⭐ 信用分 {product.sellerCredit ?? '—'}</div>
            </div>
          </div>

          <div className="flex gap-4 mt-auto">
            <button
              onClick={toggleFavorite}
              disabled={busy || (isAuthed && (favoriteState.loading || favoriteState.data == null))}
              aria-pressed={favorited}
              className={`flex-1 py-3.5 text-sm font-semibold border transition-all ${
                favorited
                  ? 'bg-pink-50 text-pink-500 border-pink-200'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-pink-200 hover:text-pink-500'
              }`}
            >
              {busy ? '处理中…' : isAuthed && favoriteState.data == null ? '收藏状态未知' : favorited ? '❤️ 已收藏' : '🤍 收藏'}
            </button>
            {isAuthed ? (
              <button
                disabled
                title="联系卖家的消息接口尚未接入，不会发送通知"
                className="btn-ghost flex-[2]"
              >
                联系卖家 · 暂未接入
              </button>
            ) : (
              <Link to="/login" className="btn-primary flex-[2] text-center">登录后收藏</Link>
            )}
          </div>
        </div>
      </div>

      {/* 描述 */}
      <section className="card p-5 sm:p-6 mb-8">
        <h2 className="section-title mb-6">宝贝描述</h2>
        <p className="text-gray-600 leading-loose whitespace-pre-line text-[15px]">
          {product.description || '卖家没有填写更多描述。'}
        </p>
      </section>

      {/* 相关推荐 */}
      <DataStatus {...relatedState} count={relatedList.length} label="相似好物 · 已加载范围" />
      {relatedList.length > 0 && (
        <section>
          <h2 className="section-title mb-7">相似好物</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {relatedList.map((p) => (
              <Link key={p.id} to={`/market/${p.id}`} className="card card-hover overflow-hidden group">
                <div className="aspect-square overflow-hidden bg-gray-50">
                  <ProductImage
                    src={resolveImages(p)[0]}
                    alt={p.title}
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
