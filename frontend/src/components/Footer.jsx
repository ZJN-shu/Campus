import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="bg-white border-t border-gray-100 mt-auto">
      <div className="nav-container py-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="md:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-xl">🎓</span>
              <span className="text-base font-bold bg-gradient-to-r from-primary-500 to-pink-500 bg-clip-text text-transparent">
                Campus
              </span>
            </div>
            <p className="text-sm text-gray-500 leading-relaxed max-w-sm">
              连接校园每一个角落，让大学生活更加精彩。
            </p>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-gray-700 mb-4">快速链接</h3>
            <ul className="space-y-2.5 text-sm">
              <li><Link to="/posts" className="text-gray-500 hover:text-gray-600 transition-colors">社区论坛</Link></li>
              <li><Link to="/errand" className="text-gray-500 hover:text-gray-600 transition-colors">跑腿任务</Link></li>
              <li><Link to="/market" className="text-gray-500 hover:text-gray-600 transition-colors">二手市场</Link></li>
              <li><Link to="/hot" className="text-gray-500 hover:text-gray-600 transition-colors">热门榜单</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-gray-700 mb-4">服务说明</h3>
            <ul className="space-y-2.5 text-sm">
              <li className="text-gray-500">遇到请求错误时，可复制请求编号反馈。</li>
              <li className="text-gray-500">请勿在公开内容中发布个人隐私。</li>
            </ul>
          </div>
        </div>

        <div className="mt-10 pt-8 border-t border-gray-100 text-center text-xs text-gray-500">
          © {new Date().getFullYear()} Campus 校园平台 · 让校园生活更精彩
        </div>
      </div>
    </footer>
  );
}
