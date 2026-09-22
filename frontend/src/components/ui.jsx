// 共享 UI 组件：加载骨架、空状态、区块标题

export function Loading({ rows = 3, className = '' }) {
  return (
    <div className={`space-y-4 ${className}`}>
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
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-6">
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
    <div className="py-24 text-center animate-fade-in">
      <div className="text-5xl mb-4 opacity-60">{icon}</div>
      <p className="text-gray-500 font-medium">{title}</p>
      {desc && <p className="text-sm text-gray-400 mt-2">{desc}</p>}
    </div>
  );
}

export function SectionHeader({ title, icon, more, moreTo, accent = 'text-primary-500' }) {
  return (
    <div className="flex items-center justify-between mb-7">
      <h2 className="section-title flex items-center gap-2">
        {icon && <span>{icon}</span>}
        {title}
      </h2>
      {more && moreTo && (
        <a href={moreTo} className={`text-sm font-medium hover:opacity-70 transition-opacity ${accent}`}>
          {more} →
        </a>
      )}
    </div>
  );
}
