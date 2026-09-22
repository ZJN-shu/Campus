// 集中管理兜底 mock 数据。
// 前端优先请求后端真实接口，当后端不可用（未启动 / 网络错误 / 无数据）时，
// 使用这里的 mock 数据保证 UI 正常展示，便于纯前端预览。

export const mockPosts = [
  { id: 1, title: '期末考试复习攻略分享', content: '分享一下我的复习方法和资料，希望对大家有帮助。整理了高数、线代、英语的复习计划，亲测有效～', category: '学习', tags: ['考试', '复习'], authorName: '学霸小明', viewCount: 2341, likeCount: 156, commentCount: 23, createTime: '2小时前', isTop: true, isEssence: true },
  { id: 2, title: '校园美食地图 | 食堂隐藏菜单大揭秘', content: '吃了四年食堂，终于整理出这份隐藏菜单。每个食堂都有值得一试的宝藏窗口，快来收藏！', category: '生活', tags: ['美食', '食堂'], authorName: '吃货大王', viewCount: 1892, likeCount: 234, commentCount: 45, createTime: '3小时前', isTop: false, isEssence: true },
  { id: 3, title: '社团招新啦！总有一个适合你', content: '各大社团开始招新了，来看看有没有你感兴趣的。从学术到文艺，从运动到科技，总有一款适合你。', category: '社团', tags: ['社团', '招新'], authorName: '社团达人', viewCount: 1567, likeCount: 89, commentCount: 12, createTime: '5小时前', isTop: false, isEssence: false },
  { id: 4, title: '考研经验分享：从双非到985的逆袭', content: '备考一年，终于上岸了！分享一下我的经验，包括时间规划、资料选择、心态调整，希望能帮到学弟学妹。', category: '学习', tags: ['考研', '经验'], authorName: '考研学长', viewCount: 3421, likeCount: 567, commentCount: 89, createTime: '1天前', isTop: false, isEssence: true },
];

export const mockPostDetail = {
  id: 1,
  title: '期末考试复习攻略分享',
  content: `分享一下我的复习方法和资料，希望对大家有帮助！

高数复习
1. 先把课本过一遍，重点看例题
2. 做历年真题，至少做3年的
3. 整理错题本，考前重点看

英语复习
1. 每天背50个单词
2. 阅读理解每天2篇
3. 作文模板提前准备

线代复习
1. 重点掌握矩阵运算
2. 向量组、特征值必须熟练
3. 多做综合题

祝大家考试顺利！🎉`,
  category: '学习',
  tags: ['考试', '复习', '攻略'],
  authorName: '学霸小明',
  viewCount: 2341,
  likeCount: 156,
  commentCount: 23,
  favoriteCount: 45,
  createTime: '2024-01-15 14:30',
  isEssence: true,
};

export const mockComments = [
  { id: 1, authorName: '考研学姐', content: '太有用了！收藏了', createTime: '1小时前', likeCount: 12 },
  { id: 2, authorName: '大一新生', content: '请问有具体的资料推荐吗？', createTime: '2小时前', likeCount: 3 },
  { id: 3, authorName: '学霸小王', content: '补充一下，高数可以看看汤家凤的视频', createTime: '3小时前', likeCount: 8 },
];

export const mockTasks = [
  { id: 1, title: '帮取快递 - 菜鸟驿站', description: '菜鸟驿站取一个中号快递，取件码稍后发送', category: '取件', reward: 5, pickupLocation: '菜鸟驿站', deliveryLocation: '6号宿舍楼', deadline: '今天 18:00', status: 0, publisherName: '小明', createTime: '10分钟前' },
  { id: 2, title: '带一份食堂午饭', description: '二食堂黄焖鸡米饭，不要太辣', category: '带饭', reward: 8, pickupLocation: '第二食堂', deliveryLocation: '图书馆一楼', deadline: '今天 12:30', status: 0, publisherName: '小红', createTime: '30分钟前' },
  { id: 3, title: '图书馆占座', description: '帮忙在图书馆三楼靠窗位置占个座', category: '其他', reward: 10, pickupLocation: '图书馆', deliveryLocation: '三楼靠窗位置', deadline: '明天 08:00', status: 0, publisherName: '学长', createTime: '1小时前' },
  { id: 4, title: '代取外卖', description: '美团外卖，校门口取', category: '取件', reward: 3, pickupLocation: '校门口', deliveryLocation: '3号宿舍楼', deadline: '今天 19:00', status: 0, publisherName: '小李', createTime: '2小时前' },
];

export const mockProducts = [
  { id: 1, title: '九成新 iPad Air 5', price: 2800, originalPrice: 4799, category: '数码', quality: '95新', sellerName: '数码达人', favoriteCount: 45, firstImage: 'https://picsum.photos/400/400?random=1' },
  { id: 2, title: '考研英语全套资料', price: 50, originalPrice: 280, category: '书籍', quality: '9成新', sellerName: '考研学姐', favoriteCount: 23, firstImage: 'https://picsum.photos/400/400?random=2' },
  { id: 3, title: '小米台灯', price: 30, originalPrice: 149, category: '生活', quality: '8成新', sellerName: '毕业生小李', favoriteCount: 12, firstImage: 'https://picsum.photos/400/400?random=3' },
  { id: 4, title: '山地自行车', price: 350, originalPrice: 1200, category: '运动', quality: '8成新', sellerName: '运动少年', favoriteCount: 34, firstImage: 'https://picsum.photos/400/400?random=4' },
  { id: 5, title: 'AirPods Pro 2', price: 900, originalPrice: 1899, category: '数码', quality: '9成新', sellerName: '音乐爱好者', favoriteCount: 67, firstImage: 'https://picsum.photos/400/400?random=5' },
  { id: 6, title: '宿舍小桌子', price: 25, originalPrice: 69, category: '生活', quality: '9成新', sellerName: '整理达人', favoriteCount: 8, firstImage: 'https://picsum.photos/400/400?random=6' },
  { id: 7, title: '高等数学教材', price: 15, originalPrice: 80, category: '书籍', quality: '8成新', sellerName: '数学课代表', favoriteCount: 19, firstImage: 'https://picsum.photos/400/400?random=7' },
  { id: 8, title: '电风扇', price: 45, originalPrice: 159, category: '生活', quality: '9成新', sellerName: '怕热的小王', favoriteCount: 15, firstImage: 'https://picsum.photos/400/400?random=8' },
];

export const mockHotRank = [
  { id: 1, title: '考研经验分享：从双非到985的逆袭', category: '学习', hotScore: 9999, trend: 1, viewCount: 3421, likeCount: 567 },
  { id: 2, title: '期末考试复习攻略分享', category: '学习', hotScore: 8765, trend: 1, viewCount: 2341, likeCount: 156 },
  { id: 3, title: '校园美食地图 | 食堂隐藏菜单大揭秘', category: '生活', hotScore: 7654, trend: -1, viewCount: 1892, likeCount: 234 },
  { id: 4, title: '社团招新啦！总有一个适合你', category: '社团', hotScore: 6543, trend: 0, viewCount: 1567, likeCount: 89 },
  { id: 5, title: '校园跑步路线推荐', category: '运动', hotScore: 5432, trend: 1, viewCount: 876, likeCount: 45 },
  { id: 6, title: '二手教材转让，白菜价！', category: '二手', hotScore: 4321, trend: -1, viewCount: 543, likeCount: 23 },
];

export const mockMessages = [
  { id: 1, type: 1, fromUserName: '考研学姐', content: '评论了你的帖子「期末考试复习攻略」', createTime: '10分钟前', isRead: false },
  { id: 2, type: 2, fromUserName: '吃货大王', content: '点赞了你的帖子「校园美食地图」', createTime: '30分钟前', isRead: false },
  { id: 3, type: 4, fromUserName: '运动达人', content: '关注了你', createTime: '1小时前', isRead: false },
  { id: 4, type: 3, fromUserName: '大一新生', content: '收藏了你的帖子「考研经验分享」', createTime: '2小时前', isRead: true },
  { id: 5, type: 5, fromUserName: '系统通知', content: '你的帖子「社团招新」已被设为精华', createTime: '3小时前', isRead: true },
];

export const mockUser = {
  id: 1,
  nickName: 'Campus 同学',
  college: '计算机学院',
  major: '软件工程',
  grade: 2022,
  credit: 98,
};

export const mockTaskDetail = {
  id: 1,
  title: '帮取菜鸟驿站快递（中件）',
  description: '菜鸟驿站取一个中号快递，取件码我接单后私发给你，麻烦轻拿轻放，谢谢！',
  category: '取件',
  reward: 5,
  pickupLocation: '菜鸟驿站（南门）',
  deliveryLocation: '6号宿舍楼 305',
  deadline: '2026-09-23T18:00:00',
  status: 0,
  publisherName: '张三同学',
  publisherAvatar: '',
  acceptorName: null,
  createTime: '2026-09-22 09:10:00',
};

export const mockProductDetail = {
  id: 1,
  title: '九成新 iPad Air 5',
  description: '自用一年，成色很新，无磕碰无划痕，电池健康度 92%。原装充电器、保护壳一起送。因为换了新款所以出，支持当面验货。',
  price: 2800,
  originalPrice: 4799,
  category: '数码',
  quality: '95新',
  location: '6号宿舍楼下当面交易',
  images: ['https://picsum.photos/600/600?random=31', 'https://picsum.photos/600/600?random=32'],
  viewCount: 320,
  favoriteCount: 45,
  sellerId: 1,
  sellerName: '数码达人',
  sellerCredit: 98,
  createTime: '2026-09-20 14:30:00',
};
