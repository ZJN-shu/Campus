import { Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { postApi, errandApi } from '../services';
import { Loading, Empty, DataStatus } from '../components/ui';
import SEOHead from '../components/SEOHead';

const categories = [
  { name: '学习交流', icon: '📚', color: 'bg-blue-50' },
  { name: '校园生活', icon: '🌟', color: 'bg-pink-50' },
  { name: '技术交流', icon: '💻', color: 'bg-purple-50' },
  { name: '经验分享', icon: '💡', color: 'bg-campus-50' },
  { name: '二手交易', icon: '🔄', color: 'bg-amber-50' },
];

export default function Home() {
  // 热门帖子与任务分别维护请求状态。
  const hotState = useApiData(
    () => postApi.getHot('all', 5),
    [],
    [],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  // 最新跑腿任务
  const taskState = useApiData(
    () => errandApi.getNearbyTasks(),
    [],
    [],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const { data: hotPosts, loading: hotLoading } = hotState;
  const { data: tasks, loading: taskLoading } = taskState;

  return (
    <div className="page-container">
      <SEOHead
        title="校园平台 - 让校园生活更便捷"
        description="校园综合服务平台：论坛交流、二手交易、跑腿代办，一站式解决校园生活需求"
        keywords="校园平台,校园论坛,二手交易,跑腿代办,校园生活"
      />
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-orange-700 to-rose-700 px-6 sm:px-10 py-8 min-h-[230px] mb-6 flex items-center">
        <div className="relative z-10 max-w-2xl">
          <h1 className="text-2xl md:text-3xl font-bold text-white mb-3 leading-tight">
            欢迎来到 Campus ✨
          </h1>
          <p className="text-white/90 text-sm sm:text-base mb-5 leading-relaxed max-w-lg">
            连接每一个校园人，分享知识、结交朋友、互助成长
          </p>
          <div className="flex flex-wrap gap-3">
            <Link to="/posts" className="px-7 py-3 bg-white text-primary-600 text-sm font-semibold hover:shadow-xl hover:-translate-y-0.5 transition-all">
              浏览社区
            </Link>
            <Link to="/errand" className="px-7 py-3 bg-white/20 text-white border border-white/40 text-sm font-semibold hover:bg-white/30 transition-all backdrop-blur-sm">
              查看跑腿
            </Link>
          </div>
        </div>
        <div className="absolute right-16 top-1/2 -translate-y-1/2 text-9xl opacity-20 hidden md:block animate-float">🎓</div>
      </section>

      {/* 分类 */}
      <section className="mb-8" aria-label="板块分类">
        <div className="flex flex-wrap gap-3">
          {categories.map((cat) => (
            <Link
              key={cat.name}
              to={`/posts?category=${cat.name}`}
              className={`card card-hover inline-flex items-center gap-2 px-4 py-3 ${cat.color}`}
            >
              <span className="text-xl" aria-hidden="true">{cat.icon}</span>
              <span className="text-sm font-semibold text-gray-700">{cat.name}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* 主内容 */}
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_300px] gap-8">
        <div className="min-w-0 space-y-8">
          {/* 热门帖子 */}
          <section>
            <div className="flex items-center justify-between mb-4">
              <h2 className="section-title">🔥 热门讨论</h2>
              <Link to="/hot" className="text-sm font-medium text-primary-600 hover:underline">查看热榜 →</Link>
            </div>
            <DataStatus {...hotState} count={Math.min(hotPosts.length, 5)} label="热门讨论" />
            {hotLoading && !hotState.lastUpdatedAt ? (
              <Loading rows={3} />
            ) : hotState.error && !hotState.lastUpdatedAt ? null : hotPosts.length === 0 ? (
              <Empty icon="💬" title="暂无热门讨论" desc="去社区看看最新分享吧" />
            ) : (
              <div className="card divide-y divide-gray-50 overflow-hidden">
                {hotPosts.slice(0, 5).map((post, index) => (
                  <Link
                    key={post.id || index}
                    to={`/posts/${post.id}`}
                    className="flex items-center gap-4 px-4 sm:px-6 py-4 hover:bg-gray-50/60 transition-colors group"
                  >
                    <span className={`w-8 h-8 flex items-center justify-center text-xs font-bold shrink-0 ${
                      index < 3 ? 'bg-gradient-to-br from-primary-400 to-pink-500 text-white' : 'bg-gray-100 text-gray-400'
                    }`}>
                      {index + 1}
                    </span>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-gray-700 font-medium truncate group-hover:text-primary-600 transition-colors">
                        {post.title}
                      </h3>
                    </div>
                    <div className="hidden sm:flex items-center gap-4 text-xs text-gray-400 shrink-0">
                      {post.category && <span className="tag bg-gray-50 text-gray-500">{post.category}</span>}
                      <span>👀 {post.viewCount ?? '—'}</span>
                      <span>❤️ {post.likeCount ?? '—'}</span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </section>

          {/* 跑腿任务 */}
          <section>
            <div className="flex items-center justify-between mb-4">
              <h2 className="section-title">🏃 最新跑腿</h2>
              <Link to="/errand" className="text-sm font-medium text-campus-500 hover:text-campus-400">更多 →</Link>
            </div>
            <DataStatus {...taskState} count={Math.min(tasks.length, 4)} label="最新跑腿" />
            {taskLoading && !taskState.lastUpdatedAt ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[0, 1].map((i) => (
                  <div key={i} className="card p-7">
                    <div className="skeleton h-4 w-2/3 mb-4" />
                    <div className="skeleton h-3 w-1/2" />
                  </div>
                ))}
              </div>
            ) : taskState.error && !taskState.lastUpdatedAt ? null : tasks.length === 0 ? (
              <Empty icon="🏃" title="暂无跑腿任务" desc="有新的需求时会展示在这里" />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {tasks.slice(0, 4).map((task) => (
                  <Link key={task.id} to={`/errand/${task.id}`} className="card card-hover p-5 group">
                    <div className="flex items-start justify-between mb-4 gap-4">
                      <h3 className="font-semibold text-gray-800 group-hover:text-campus-500 transition-colors line-clamp-1">
                        {task.title}
                      </h3>
                      <span className="text-lg font-bold text-primary-500 shrink-0">¥{task.reward ?? '—'}</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-gray-400">
                      {task.category && <span className="tag bg-campus-50 text-campus-500">{task.category}</span>}
                      <span>📍 {task.pickupLocation || '地点未填写'}</span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>

        {/* 侧边栏 */}
        <aside className="space-y-6">
          {/* 快捷入口 */}
          <section>
            <h2 className="section-title mb-4">快捷入口</h2>
            <div className="grid grid-cols-2 gap-4">
              {[
                { to: '/posts', icon: '✏️', label: '社区广场', bg: 'bg-primary-50' },
                { to: '/errand', icon: '📋', label: '任务广场', bg: 'bg-campus-50' },
                { to: '/market', icon: '🛍️', label: '逛二手', bg: 'bg-purple-50' },
                { to: '/profile', icon: '👤', label: '我的', bg: 'bg-amber-50' },
              ].map((item) => (
                <Link
                  key={item.label}
                  to={item.to}
                  className={`card card-hover flex flex-col items-center gap-2 py-4 ${item.bg} !border-transparent`}
                >
                  <span className="text-3xl">{item.icon}</span>
                  <span className="text-sm font-semibold text-gray-600">{item.label}</span>
                </Link>
              ))}
            </div>
          </section>

          {/* 校园公告 */}
          <section className="card p-5 bg-orange-50/40">
            <h2 className="text-sm font-bold text-gray-700 mb-5 flex items-center gap-2">📢 校园公告 · 示例</h2>
            <div className="space-y-4 text-sm text-gray-600">
              <p className="flex gap-3 items-start"><span>🎉</span><span className="leading-relaxed">校园歌手大赛报名开始啦！</span></p>
              <p className="flex gap-3 items-start"><span>📚</span><span className="leading-relaxed">图书馆开放时间调整通知</span></p>
              <p className="flex gap-3 items-start"><span>🏃</span><span className="leading-relaxed">春季运动会即将举行</span></p>
            </div>
            <p className="text-xs text-gray-500 mt-4">以下内容为界面示例，非实时校园通知。</p>
          </section>
        </aside>
      </div>
    </div>
  );
}
