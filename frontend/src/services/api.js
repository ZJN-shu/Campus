import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器 - 添加 token
// 后端 RefreshTokenInterceptor 直接取 authorization 头的原始值作为 token，
// 因此这里发送原始 token（不加 Bearer 前缀），保证前后端联调一致
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.authorization = token;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data.success === false) {
      return Promise.reject(new Error(data.errorMsg || '请求失败'));
    }
    return data;
  },
  (error) => {
    // 401 时清除本地 token，但不强制跳转，交给页面/路由自行处理，
    // 同时让请求 reject，使得 useApiData 能回退到 mock 数据保证 UI 可用
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
    }
    return Promise.reject(error);
  }
);

export default api;
