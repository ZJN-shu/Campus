import { useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import useApiData from '../hooks/useApiData';
import { postApi, followApi, favoriteApi } from '../services';
import { useAuth } from '../context/AuthContext';
import { Loading, Empty, DataStatus, ErrorNotice } from '../components/ui';

export default function PostDetail() {
  const { id } = useParams();
  const { isAuthed, token } = useAuth();
  const [actionError, setActionError] = useState(null);
  const [notice, setNotice] = useState('');
  const [busyAction, setBusyAction] = useState('');
  const actionLock = useRef(false);
  const [commentText, setCommentText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const postState = useApiData(
    () => postApi.getById(id),
    null,
    [id, token],
    (payload) => payload || null
  );

  const commentsState = useApiData(
    () => postApi.getComments(id),
    [],
    [id],
    (payload) => (Array.isArray(payload) ? payload : payload?.records || payload?.list || [])
  );

  const { data: post, loading, lastUpdatedAt, setData: setPost } = postState;
  const { data: comments, loading: commentsLoading, refetch: refetchComments } = commentsState;
  const followState = useApiData(
    () => isAuthed && post?.userId ? followApi.isFollowed(post.userId) : Promise.resolve({ data: null }),
    null,
    [post?.userId, token]
  );
  const liked = !!post?.isLiked;
  const favorited = !!post?.isFavorited;
  const followed = followState.data === true;

  const runAction = async (name, action, update) => {
    if (actionLock.current) return;
    if (!isAuthed) {
      setActionError(Object.assign(new Error('请先登录后操作'), { status: 401 }));
      return;
    }
    actionLock.current = true;
    setBusyAction(name);
    setActionError(null);
    try {
      await action();
      update();
    } catch (error) {
      setActionError(error);
    } finally {
      actionLock.current = false;
      setBusyAction('');
    }
  };

  const updateInteraction = (flag, count, next) => setPost((prev) => prev && ({
    ...prev,
    [flag]: next,
    [count]: prev[count] == null ? null : Math.max(0, Number(prev[count]) + (next ? 1 : -1)),
  }));
  const handleLike = () => runAction('like',
    () => liked ? postApi.unlike(id) : postApi.like(id),
    () => updateInteraction('isLiked', 'likeCount', !liked));
  const handleFavorite = () => runAction('favorite',
    () => favorited ? favoriteApi.unfavorite('post', Number(id)) : favoriteApi.favorite('post', Number(id)),
    () => updateInteraction('isFavorited', 'favoriteCount', !favorited));
  const handleFollow = () => runAction('follow',
    () => followed ? followApi.unfollow(post.userId) : followApi.follow(post.userId),
    () => followState.setData(!followed));

  const handleComment = async () => {
    if (!commentText.trim() || submitting) return;
    if (!isAuthed) {
      setActionError(Object.assign(new Error('请先登录后发表评论'), { status: 401 }));
      return;
    }
    setActionError(null);
    setNotice('');
    setSubmitting(true);
    try {
      await postApi.addComment({ postId: Number(id), content: commentText });
      setCommentText('');
      setNotice('评论发表成功。');
      const result = await refetchComments();
      if (!result.success) setNotice('评论发表成功，但评论列表更新失败；无需再次提交。');
    } catch (e) {
      setActionError(e);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading && !lastUpdatedAt) {
    return (
      <div className="page-container">
        <Loading rows={2} />
      </div>
    );
  }

  if (!post) return <div className="page-container"><DataStatus {...postState} />{!postState.error && <Empty title="帖子不存在或已删除" />}</div>;

  const author = post.authorName || post.userName || '匿名同学';
  const tags = Array.isArray(post.tags) ? post.tags : (post.tags ? String(post.tags).split(',') : []);

  return (
    <div className="page-container">
      {/* Back */}
      <Link to="/posts" className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 mb-8 transition-colors">
        ← 返回社区
      </Link>

      <DataStatus {...postState} label="帖子详情" />
      <ErrorNotice error={actionError} />
      <ErrorNotice error={followState.error} onRetry={followState.refetch} />
      {notice && <p role="status" className="p-3 mb-4 text-sm bg-green-50 text-green-800">{notice}</p>}
      {/* Article */}
      <article className="card p-5 sm:p-8 mb-6">
        {/* Tags */}
        <div className="flex items-center gap-2 mb-6">
          {post.category && <span className="tag bg-primary-50 text-primary-600">{post.category}</span>}
          {!!post.isEssence && <span className="tag bg-amber-50 text-amber-600">⭐ 精华</span>}
        </div>

        {/* Title */}
        <h1 className="text-2xl md:text-3xl font-bold text-gray-800 mb-9 leading-tight">{post.title}</h1>

        {/* Author */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-6 pb-6 border-b border-gray-100">
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
            disabled={!!busyAction || !post.userId || (isAuthed && (followState.loading || followState.data == null))}
            aria-pressed={followed}
            className={`px-5 py-2 text-sm font-medium transition-colors ${
              followed
                ? 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                : 'border border-primary-200 text-primary-500 hover:bg-primary-50'
            }`}
          >
            {busyAction === 'follow' ? '处理中…' : isAuthed && followState.data == null ? '关注状态未知' : followed ? '已关注' : '+ 关注'}
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
        <div className="flex flex-wrap items-center justify-start gap-3 pt-6 border-t border-gray-100">
          <button
            onClick={handleLike}
            aria-label={liked ? '取消点赞' : '点赞'}
            aria-pressed={liked}
            disabled={!!busyAction}
            className={`flex items-center gap-2 px-6 py-2.5 text-sm font-medium transition-all ${
              liked ? 'bg-pink-50 text-pink-500' : 'text-gray-400 hover:bg-gray-50'
            }`}
          >
            {liked ? '❤️' : '🤍'} {post.likeCount ?? '—'}
          </button>
          <button
            onClick={handleFavorite}
            aria-label={favorited ? '取消收藏' : '收藏帖子'}
            aria-pressed={favorited}
            disabled={!!busyAction}
            className={`flex items-center gap-2 px-6 py-2.5 text-sm font-medium transition-all ${
              favorited ? 'bg-amber-50 text-amber-500' : 'text-gray-400 hover:bg-gray-50'
            }`}
          >
            {favorited ? '⭐' : '☆'} {post.favoriteCount ?? '—'}
          </button>
          <span className="flex items-center gap-2 text-gray-400 text-sm px-4">💬 {post.commentCount ?? '—'}</span>
          <span className="flex items-center gap-2 text-gray-400 text-sm px-4">👀 {post.viewCount ?? '—'}</span>
        </div>
      </article>

      {/* Comments */}
      <section className="card p-5 sm:p-8">
        <h2 className="text-base font-bold text-gray-800 mb-8">💬 评论</h2>

        {/* Comment Input */}
        <div className="flex gap-4 mb-10">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-400 to-pink-400 flex items-center justify-center text-white text-xs font-bold shrink-0">
            C
          </div>
          <div className="flex-1 min-w-0">
            <label htmlFor="comment-content" className="sr-only">评论内容</label>
            <textarea
              id="comment-content"
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
        <DataStatus {...commentsState} count={comments.length} label="评论当前页" />
        {commentsLoading && !commentsState.lastUpdatedAt ? (
          <Loading rows={2} />
        ) : commentsState.error && !commentsState.lastUpdatedAt ? null : comments.length === 0 ? (
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
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2.5 mb-1.5">
                      <span className="text-sm font-semibold text-gray-700">{name}</span>
                      <span className="text-xs text-gray-400">{comment.createTime}</span>
                    </div>
                    <p className="text-sm text-gray-600 leading-relaxed">{comment.content}</p>
                    <div className="flex items-center gap-5 mt-2.5 text-xs text-gray-400">
                      <span>❤️ {comment.likeCount ?? '—'}</span>
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
