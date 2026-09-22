import api from './api';

// ===== 用户模块 =====
export const studentApi = {
  sendCode: (phone) => api.post('/student/code', null, { params: { phone } }),
  login: (data) => api.post('/student/login', data),
  getMe: () => api.get('/student/me'),
  getStats: () => api.get('/student/stats'),
  getInfo: (id) => api.get(`/student/${id}`),
  updateInfo: (data) => api.put('/student/info', data),
  logout: () => api.post('/student/logout')
};

// ===== 帖子模块 =====
export const postApi = {
  getById: (id) => api.get(`/post/${id}`),
  getByCategory: (category, current = 1, sort = 'hot') => 
    api.get('/post/category', { params: { category, current, sort } }),
  create: (data) => api.post('/post', data),
  update: (data) => api.put('/post', data),
  delete: (id) => api.delete(`/post/${id}`),
  getHot: (category = 'all', limit = 20) => 
    api.get('/post/hot', { params: { category, limit } }),
  getRecommend: (current = 1) => api.get('/post/recommend', { params: { current } }),
  getMy: (current = 1) => api.get('/post/my', { params: { current } }),
  getComments: (id, current = 1) => api.get(`/post/${id}/comments`, { params: { current } }),
  like: (id) => api.post(`/post/like/${id}`),
  unlike: (id) => api.delete(`/post/like/${id}`),
  addComment: (data) => api.post('/post/comment', data),
  deleteComment: (id) => api.delete(`/post/comment/${id}`)
};

// ===== 跑腿模块 =====
export const errandApi = {
  publishTask: (data) => api.post('/errand/task', data),
  getTaskDetail: (id) => api.get(`/errand/task/${id}`),
  getNearbyTasks: (latitude, longitude, current = 1) => 
    api.get('/errand/tasks/nearby', { params: { latitude, longitude, current } }),
  acceptTask: (id) => api.post(`/errand/task/${id}/accept`),
  completeTask: (id) => api.put(`/errand/task/${id}/complete`),
  cancelTask: (id) => api.delete(`/errand/task/${id}`),
  getMyPublished: (current = 1) => api.get('/errand/tasks/published', { params: { current } }),
  getMyAccepted: (current = 1) => api.get('/errand/tasks/accepted', { params: { current } }),
  payOrder: (id) => api.post(`/errand/order/${id}/pay`),
  getOrderDetail: (id) => api.get(`/errand/order/${id}`),
  // 评价
  evaluate: (taskId, score, content) => api.post('/errand/evaluation', null, { params: { taskId, score, content } }),
  getUserEvaluations: (userId, current = 1) => api.get(`/errand/evaluation/user/${userId}`, { params: { current } })
};

// ===== 商品模块 =====
export const productApi = {
  getById: (id) => api.get(`/product/${id}`),
  getByCategory: (category, current = 1, sort = 'time', minPrice, maxPrice) => 
    api.get('/product/category', { params: { category, current, sort, minPrice, maxPrice } }),
  publish: (data) => api.post('/product', data),
  update: (data) => api.put('/product', data),
  delete: (id) => api.delete(`/product/${id}`),
  search: (keyword, current = 1) => api.get('/product/search', { params: { keyword, current } }),
  getMy: (current = 1) => api.get('/product/my', { params: { current } }),
  favorite: (id) => api.post(`/product/favorite/${id}`),
  unfavorite: (id) => api.delete(`/product/favorite/${id}`),
  isFavorited: (id) => api.get(`/product/favorite/check/${id}`)
};

// ===== 热榜模块 =====
export const hotApi = {
  getHotRank: (category = 'all', current = 1) => 
    api.get('/hot/rank', { params: { category, current } }),
  getTodayHot: () => api.get('/hot/today'),
  getRisingHot: () => api.get('/hot/rising')
};

// ===== 搜索模块（ES） =====
export const searchApi = {
  searchProducts: (keyword, category, page = 1, size = 10) => 
    api.get('/search/products', { params: { keyword, category, page, size } })
};

// ===== 消息模块 =====
export const messageApi = {
  getMessages: (current = 1, type) => 
    api.get('/message/list', { params: { current, type } }),
  markAsRead: (id) => api.put(`/message/${id}/read`),
  markAllAsRead: () => api.put('/message/read-all'),
  getUnreadCount: () => api.get('/message/unread-count')
};

// ===== 收藏模块 =====
export const favoriteApi = {
  favorite: (targetType, targetId) => api.post('/favorite', { targetType, targetId }),
  unfavorite: (targetType, targetId) => api.delete('/favorite', { data: { targetType, targetId } }),
  isFavorited: (targetType, targetId) => api.get('/favorite/check', { params: { targetType, targetId } }),
  getMyFavorites: (targetType, current = 1) => 
    api.get('/favorite/my', { params: { targetType, current } })
};

// ===== 关注模块 =====
export const followApi = {
  follow: (followeeId) => api.post('/follow', { followeeId }),
  unfollow: (followeeId) => api.delete('/follow', { data: { followeeId } }),
  isFollowed: (followeeId) => api.get('/follow/check', { params: { followeeId } }),
  getFollowers: (userId, current = 1) => api.get(`/follow/followers/${userId}`, { params: { current } }),
  getFollowees: (userId, current = 1) => api.get(`/follow/followees/${userId}`, { params: { current } }),
  getCommonFollows: (userId) => api.get(`/follow/common/${userId}`)
};

// ===== 文件上传模块 =====
export const uploadApi = {
  /**
   * 上传单个文件
   * @param {File} file 文件
   * @param {string} dir 存储目录（avatar / post / product / common）
   * @returns {Promise} 返回文件 URL
   */
  upload: (file, dir = 'common') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('dir', dir);
    return api.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  /**
   * 批量上传文件
   * @param {File[]} files 文件数组
   * @param {string} dir 存储目录
   * @returns {Promise} 返回文件 URL 列表
   */
  uploadBatch: (files, dir = 'common') => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    formData.append('dir', dir);
    return api.post('/upload/batch', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  /**
   * 删除文件
   * @param {string} url 文件 URL
   */
  delete: (url) => api.delete('/upload', { params: { url } })
};
