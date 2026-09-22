import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="bg-white border-t border-gray-100 mt-auto">
      <div className="w-full mx-auto px-6 md:px-10 2xl:px-[6vw] py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
          <div className="md:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-xl">🎓</span>
              <span className="text-base font-bold bg-gradient-to-r from-primary-500 to-pink-500 bg-clip-text text-transparent">
                Campus
              </span>
            </div>
            <p className="text-sm text-gray-400 leading-relaxed max-w-sm">
              连接校园每一个角落，让大学生活更加精彩。
            </p>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-gray-700 mb-4">快速链接</h3>
            <ul className="space-y-2.5 text-sm">
              <li><Link to="/posts" className="text-gray-400 hover:text-gray-600 transition-colors">社区论坛</Link></li>
              <li><Link to="/errand" className="text-gray-400 hover:text-gray-600 transition-colors">跑腿任务</Link></li>
              <li><Link to="/market" className="text-gray-400 hover:text-gray-600 transition-colors">二手市场</Link></li>
              <li><Link to="/hot" className="text-gray-400 hover:text-gray-600 transition-colors">热门榜单</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-gray-700 mb-4">关于</h3>
            <ul className="space-y-2.5 text-sm">
              <li><span className="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer">帮助中心</span></li>
              <li><span className="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer">用户协议</span></li>
              <li><span className="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer">隐私政策</span></li>
            </ul>
          </div>
        </div>

        <div className="mt-10 pt-8 border-t border-gray-100 text-center text-xs text-gray-400">
          © 2024 Campus 校园平台 · 让校园生活更精彩
        </div>
      </div>
    </footer>
  );
}
