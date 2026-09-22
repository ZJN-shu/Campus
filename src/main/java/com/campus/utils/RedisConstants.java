package com.campus.utils;

public class RedisConstants {
    // 登录相关
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 5L;

    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    // 缓存相关
    public static final String CACHE_PRODUCT_KEY = "cache:product:";
    public static final Long CACHE_PRODUCT_TTL = 30L;

    public static final String CACHE_POST_KEY = "cache:post:";
    public static final Long CACHE_POST_TTL = 30L;

    // 点赞相关
    public static final String LIKE_POST_KEY = "like:post:";
    public static final String LIKE_COMMENT_KEY = "like:comment:";
    public static final String LIKE_PRODUCT_KEY = "like:product:";

    // 收藏相关
    public static final String FAVORITE_POST_KEY = "favorite:post:";
    public static final String FAVORITE_PRODUCT_KEY = "favorite:product:";

    // 关注相关
    public static final String FOLLOW_KEY = "follow:";

    // 热榜相关
    public static final String HOT_RANK_ALL = "hot:rank:all";
    public static final String HOT_RANK_POST = "hot:rank:post";
    public static final String HOT_RANK_PRODUCT = "hot:rank:product";
    public static final String HOT_RANK_CATEGORY = "hot:rank:category:";
    public static final String HOT_SCORE_KEY = "hot:score:";
    /** 24小时今日热榜 */
    public static final String HOT_RANK_TODAY = "hot:rank:today";
    /** 最近活跃时间前缀（用于新鲜度加成） */
    public static final String HOT_LAST_ACTIVE_PREFIX = "hot:lastActive:";

    // 统计相关
    public static final String POST_STATS_KEY = "stats:post:";
    public static final String PRODUCT_STATS_KEY = "stats:product:";

}
