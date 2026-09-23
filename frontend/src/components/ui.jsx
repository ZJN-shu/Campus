import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';

// 共享 UI 组件：加载骨架、空状态、区块标题

export function Loading({ rows = 3, className = '' }) {
  return (
    <div role="status" aria-label="正在加载" className={`space-y-4 ${className}`}>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="card p-6">
          <div className="skeleton h-4 w-1/3 mb-4" />
          <div className="skeleton h-3 w-full mb-2" />
          <div className="skeleton h-3 w-2/3" />
        </div>
      ))}
    </div>
  );
}

export function GridLoading({ count = 8 }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="card overflow-hidden">
          <div className="skeleton aspect-square rounded-none" />
          <div className="p-5">
            <div className="skeleton h-3 w-full mb-3" />
            <div className="skeleton h-4 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function Empty({ icon = '📭', title = '暂无内容', desc = '' }) {
  return (
    <div role="status" className="card px-6 py-12 text-center animate-fade-in">
      <div className="text-5xl mb-4 opacity-60">{icon}</div>
      <p className="text-gray-500 font-medium">{title}</p>
      {desc && <p className="text-sm text-gray-400 mt-2">{desc}</p>}
    </div>
  );
}

export function PageHeader({ title, description, children }) {
  return (
    <header className="flex flex-wrap items-start justify-between gap-4 mb-6">
      <div className="min-w-0">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
        {description && <p className="text-sm text-gray-600 mt-2">{description}</p>}
      </div>
      {children}
    </header>
  );
}

export function ErrorNotice({ error, onRetry, stale = false }) {
  const [copyState, setCopyState] = useState('');
  if (!error) return null;
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(error.traceId);
      setCopyState('已复制');
    } catch {
      setCopyState('请选中编号手动复制');
    }
  };
  return (
    <div role="alert" className="border border-red-200 bg-red-50 p-4 text-sm text-red-800 mb-4 break-words">
      <p className="font-semibold">{stale ? '更新失败，当前为上次结果' : '暂时无法完成请求'}</p>
      <p className="mt-1">{typeof error === 'string' ? error : error.message || '请求失败，请重试'}</p>
      <div className="flex flex-wrap items-center gap-3 mt-3">
        {onRetry && <button type="button" onClick={onRetry} className="btn-ghost">重试</button>}
        {error.status === 401 && <Link to="/login" className="underline font-medium">重新登录</Link>}
        {error.traceId && (
          <>
            <span className="select-all break-all">请求编号：{error.traceId}</span>
            <button type="button" onClick={copy} className="underline">复制编号</button>
            <span role="status">{copyState}</span>
          </>
        )}
      </div>
    </div>
  );
}

export function DataStatus({ loading, error, lastUpdatedAt, refetch, count, label }) {
  return (
    <div className="mb-4">
      <div className="flex flex-wrap items-center justify-between gap-3 py-3 text-xs text-gray-600 border-b border-gray-200 mb-3">
        <div role="status" className="flex flex-wrap items-center gap-3">
          {label && <span className="font-medium text-gray-800">{label}</span>}
          {loading ? <span>{lastUpdatedAt ? '正在更新…' : '正在加载…'}</span>
            : lastUpdatedAt && count != null ? <span>已加载 {count} 条</span> : null}
          {lastUpdatedAt && <span>本页最近获取：{new Date(lastUpdatedAt).toLocaleTimeString('zh-CN')}</span>}
        </div>
        {refetch && <button type="button" disabled={loading} onClick={refetch} className="text-primary-600 font-semibold disabled:opacity-50">{loading ? '加载中' : '刷新'}</button>}
      </div>
      <ErrorNotice error={error} onRetry={loading ? undefined : refetch} stale={!!lastUpdatedAt} />
    </div>
  );
}

export function LoginPrompt({ description = '登录后查看你的真实数据和操作记录' }) {
  return (
    <div className="card p-8 text-center space-y-4">
      <h2 className="text-lg font-bold text-gray-800">请先登录</h2>
      <p className="text-sm text-gray-600">{description}</p>
      <Link to="/login" className="btn-primary">前往登录</Link>
    </div>
  );
}

export function Modal({ title, onClose, busy = false, children }) {
  return (
    <Dialog open onClose={() => { if (!busy) onClose(); }} className="relative z-[100]">
      <div className="fixed inset-0 bg-black/35" aria-hidden="true" />
      <div className="fixed inset-0 overflow-y-auto p-4 sm:p-6">
        <div className="flex min-h-full items-center justify-center">
          <DialogPanel className="bg-white w-full max-w-lg p-6 sm:p-8 shadow-xl max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between gap-4 mb-6">
              <DialogTitle className="text-xl font-bold text-gray-900">{title}</DialogTitle>
              <button type="button" aria-label="关闭弹窗" disabled={busy} onClick={onClose} className="btn-ghost px-3 py-2">✕</button>
            </div>
            {children}
          </DialogPanel>
        </div>
      </div>
    </Dialog>
  );
}

export function ProductImage({ src, alt = '商品图片', className = '' }) {
  const [failedSrc, setFailedSrc] = useState(null);
  return src && failedSrc !== src
    ? <img src={src} alt={alt} loading="lazy" onError={() => setFailedSrc(src)} className={`w-full h-full object-cover ${className}`} />
    : <div role="img" aria-label={`${alt}：暂无图片`} className="w-full h-full min-h-24 bg-gray-100 flex flex-col items-center justify-center gap-2 text-gray-500"><span className="text-3xl" aria-hidden="true">▧</span><span className="text-xs">暂无图片</span></div>;
}

export function SectionHeader({ title, icon, more, moreTo, accent = 'text-primary-500' }) {
  return (
    <div className="flex items-center justify-between mb-7">
      <h2 className="section-title flex items-center gap-2">
        {icon && <span>{icon}</span>}
        {title}
      </h2>
      {more && moreTo && (
        <Link to={moreTo} className={`text-sm font-medium hover:opacity-70 transition-opacity ${accent}`}>
          {more} →
        </Link>
      )}
    </div>
  );
}
