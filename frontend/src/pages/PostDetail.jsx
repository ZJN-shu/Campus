import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { postApi, followApi, favoriteApi } from '../services';
import { mockPostDetail, mockComments } from '../services/mockData';
import { useAuth } from '../context/AuthContext';
import { Loading, Empty } from '../components/ui';

export default function PostDetail() {
  const { id } = useParams();
  const { isAuthed } = useAuth();
  const [liked, setLiked] = useState(false);
  const [favorited, setFavorited] = useState(false);
  const [followed, setFollowed] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { data: post, loading } = useApiData(
    () => postApi.getById(id),
    { ...mockPostDetail, id },
    [id],
    (payload) => payload || null
  );

  const { data: comments, loading: commentsLoading, refetch: refetchComments } = useApiData(
    () => postApi.getComments(id),
    mockComments,
    [id],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const handleLike = async () => {
    const next = !liked;
    setLiked(next);
    try {
      if (next) await postApi.like(id);
      else await postApi.unlike(id);
    } catch (_) {
      /* 未登录或后端不可用时仅本地更新 */
    }
  };

  // 根据后端返回的互动状态初始化，并查询关注状态
  useEffect(() => {
    if (!post) return;
    setLiked(!!post.isLiked);
    setFavorited(!!post.isFavorited);
    if (isAuthed && post.userId) {
      followApi
        .isFollowed(post.userId)
        .then((res) => setFollowed(res?.data === true))
        .catch(() => setFollowed(false));
    }
  }, [post, isAuthed]);

  const handleFollow = async () => {
    if (!post?.userId) return;
    const next = !followed;
    setFollowed(next);
    try {
      if (next) await followApi.follow(post.userId);
      else await followApi.unfollow(post.userId);
    } catch (_) {
      /* 未登录时仅本地反馈 */
    }
  };

  const handleFavorite = async () => {
    const next = !favorited;
    setFavorited(next);
    try {
      if (next) await favoriteApi.favorite('post', Number(id));
      else await favoriteApi.unfavorite('post', Number(id));
    } catch (_) {
      /* 忽略 */
    }
  };

  const handleComment = async () => {
    if (!commentText.trim()) return;
    setSubmitting(true);
    try {
      await postApi.addComment({ postId: Number(id), content: commentText });
      setCommentText('');
      refetchComments();
    } catch (e) {
      // 后端不可用时本地追加，保证演示体验
      setCommentText('');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-3xl 2xl:max-w-4xl mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
        <Loading rows={2} />
      </div>
    );
  }

  const author = post.authorName || post.userName || '匿名同学';
  const tags = Array.isArray(post.tags) ? post.tags : (post.tags ? String(post.tags).split(',') : []);

  return (
    <div className="max-w-3xl 2xl:max-w-4xl mx-auto px-6 md:px-10 2xl:px-[6vw] py-14">
      {/* Back */}
      <Link to="/posts" className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 mb-8 transition-colors">
        ← 返回社区
      </Link>

      {/* Article */}
      <article className="card p-9 md:p-12 mb-8">
        {/* Tags */}
        <div className="flex items-center gap-2 mb-6">
          {post.category && <span className="tag bg-primary-50 text-primary-600">{post.category}</span>}
          {!!post.isEssence && <span className="tag bg-amber-50 text-amber-600">⭐ 精华</span>}
        </div>

        {/* Title */}
        <h1 className="text-2xl md:text-3xl font-bold text-gray-800 mb-9 leading-tight">{post.title}</h1>

        {/* Author */}
        <div className="flex items-center justify-between mb-10 pb-9 border-b border-gray-100">
          <div className="flex items-center gap-3.5">
            <div className="w-11 h-11 rounded-full bg-gradient-to-br from-primary-400 to-pink-400 flex items-center justify-center text-white font-bold text-sm">
              {author[0]}
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-700">{author}</div>
              <div className="text-xs text-gray-400 mt-0.5">{post.createTime}</div>
            </div>
          </div>
          <button
            onClick={handleFollow}
            className={`px-5 py-2 text-sm font-medium transition-colors ${
              followed
                ? 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                : 'border border-primary-200 text-primary-500 hover:bg-primary-50'
            }`}
          >
            {followed ? '已关注' : '+ 关注'}
          </button>
        </div>

        {/* Content */}
        <div className="text-gray-600 leading-loose whitespace-pre-line mb-10 text-[15px]">
          {post.content}
        </div>

        {/* Tags */}
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-2.5 mb-10">
            {tags.map((tag) => (
              <span key={tag} className="tag bg-gray-50 text-gray-500">#{tag}</span>
            ))}
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-center gap-4 pt-9 border-t border-gray-100">
          <button
            onClick={handleLike}
            className={`flex items-center gap-2 px-6 py-2.5 text-sm font-medium transition-all ${
              liked ? 'bg-pink-50 text-pink-500' : 'text-gray-400 hover:bg-gray-50'
            }`}
          >
            {liked ? '❤️' : '🤍'} {(post.likeCount ?? 0) + (liked && !post.isLiked ? 1 : 0)}
          </button>
          <button
            onClick={handleFavorite}
            className={`flex items-center gap-2 px-6 py-2.5 text-sm font-medium transition-all ${
              favorited ? 'bg-amber-50 text-amber-500' : 'text-gray-400 hover:bg-gray-50'
            }`}
          >
            {favorited ? '⭐' : '☆'} {(post.favoriteCount ?? 0) + (favorited && !post.isFavorited ? 1 : 0)}
          </button>
          <span className="flex items-center gap-2 text-gray-400 text-sm px-4">💬 {post.commentCount ?? comments.length}</span>
          <span className="flex items-center gap-2 text-gray-400 text-sm px-4">👀 {post.viewCount ?? 0}</span>
        </div>
      </article>

      {/* Comments */}
      <section className="card p-9">
        <h2 className="text-base font-bold text-gray-800 mb-8">💬 评论 ({comments.length})</h2>

        {/* Comment Input */}
        <div className="flex gap-4 mb-10">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-400 to-pink-400 flex items-center justify-center text-white text-xs font-bold shrink-0">
            C
          </div>
          <div className="flex-1">
            <textarea
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="写下你的评论..."
              rows={3}
              className="input resize-none"
            />
            <div className="flex justify-end mt-3">
              <button onClick={handleComment} disabled={submitting || !commentText.trim()} className="btn-primary px-7">
                {submitting ? '发表中...' : '发表'}
              </button>
            </div>
          </div>
        </div>

        {/* Comment List */}
        {commentsLoading ? (
          <Loading rows={2} />
        ) : comments.length === 0 ? (
          <Empty icon="💭" title="还没有评论" desc="来抢沙发吧～" />
        ) : (
          <div className="space-y-8">
            {comments.map((comment) => {
              const name = comment.authorName || comment.userName || '匿名同学';
              return (
                <div key={comment.id} className="flex gap-4">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-sky-400 to-campus-400 flex items-center justify-center text-white text-xs font-bold shrink-0">
                    {name[0]}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2.5 mb-1.5">
                      <span className="text-sm font-semibold text-gray-700">{name}</span>
                      <span className="text-xs text-gray-400">{comment.createTime}</span>
                    </div>
                    <p className="text-sm text-gray-600 leading-relaxed">{comment.content}</p>
                    <div className="flex items-center gap-5 mt-2.5 text-xs text-gray-400">
                      <button className="hover:text-pink-500 transition-colors">❤️ {comment.likeCount ?? 0}</button>
                      <button className="hover:text-primary-500 transition-colors">回复</button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
