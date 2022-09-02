package com.nowcoder.community.util;

/**
 * 常量接口的意义在于：
 */
public interface CommunityConstant {

/**
 * 激活成功
 */
int ACTIVATION_SUCCESS=0;

/**
 * 重复激活
 */
int ACTIVATION_REPEAT=1;
/**
 * 激活失败
 */
int ACTIVATION_FAILURE=2;

//用户未点记住我，则时间为12个小时
int DEFAULT_EXPIRED_SECONDS = 3600 * 12;

//用户点击则时间为100天
int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

//我们不仅能对帖子评论，也能对评论发表评论。
/**
 * 实体类型：帖子
 */
int ENTITY_TYPE_POST=1;

/**
 * 实体类型：评论
 */
int ENTITY_TYPE_COMMENT=2;

/**
 * 实体类型：用户
 */
int ENTITY_TYPE_USER=3;

    /**
     * 主题: 评论
     */
    String TOPIC_COMMENT = "comment";

    /**
     * 主题: 点赞
     */
    String TOPIC_LIKE = "like";

    /**
     * 主题: 关注
     */
    String TOPIC_FOLLOW = "follow";

    /**
     * 系统用户ID
     */
    int SYSTEM_USER_ID = 1;

    /**
     *主题：发帖
     */
    String TOPIC_PUBLISH="punlish";


    /**
     *主题：发帖
     */
    String TOPIC_DELETE="delete";
    /**
     * 普通用户权限
     */
    String AUTHORITY_USER="user";

    /**
     * 管理员
     */
    String AUTHORITY_ADMIN="admin";

    /**
     * 权限：版主
     */
    String AUTHORITY_MODERATOR="moderator";
}
