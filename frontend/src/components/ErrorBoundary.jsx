import { Component } from 'react';
import { Link } from 'react-router-dom';

/**
 * 全局错误边界：捕获子组件树中的 JS 错误，防止白屏
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[ErrorBoundary] 捕获到组件错误:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-6">
          <div className="text-center max-w-md">
            <div className="text-6xl mb-6">😵</div>
            <h1 className="text-2xl font-bold text-gray-800 mb-3">页面出了点问题</h1>
            <p className="text-gray-500 mb-6 text-sm leading-relaxed">
              抱歉，页面渲染时发生了错误。你可以尝试刷新页面或返回首页。
            </p>
            {this.state.error && (
              <details className="text-left mb-6 bg-gray-100 p-4 text-xs text-gray-500 max-h-32 overflow-y-auto">
                <summary className="cursor-pointer font-medium text-gray-600 mb-2">错误详情</summary>
                <p className="font-mono whitespace-pre-wrap break-all">{this.state.error.message}</p>
              </details>
            )}
            <div className="flex gap-3 justify-center">
              <button
                onClick={this.handleReset}
                className="px-6 py-2.5 text-sm font-medium text-primary-600 bg-primary-50 hover:bg-primary-100 transition-colors"
              >
                重试
              </button>
              <Link
                to="/"
                onClick={this.handleReset}
                className="px-6 py-2.5 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 transition-colors"
              >
                返回首页
              </Link>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
