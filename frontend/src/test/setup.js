// 测试初始化：扩展匹配器、自动清理、禁止真实网络
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import api from '../services/api';

// 每个测试前：清空存储、禁止意外真实网络请求
export function setupBeforeEach() {
  localStorage.clear();
  api.defaults.adapter = () => Promise.reject(new Error('测试禁止真实 HTTP 请求'));
  globalThis.fetch = (() => Promise.reject(new Error('测试禁止真实 fetch 请求')));
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

// 每个测试后：清理 DOM 和恢复 mock
export function setupAfterEach() {
  cleanup();
}
