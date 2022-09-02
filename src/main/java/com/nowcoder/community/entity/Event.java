package com.nowcoder.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {

    private String topic;
//    userId是事件的触发者
    private int userId;
    private int entityType;
    private int entityId;
    //entityUserId是事件的被动触发者，比如说张三关注了李四，则张三是事件触发者，李四是事件的被触发者
    //entityUserId还可能是一个帖子的发布者，这时候当前对于帖子作出评论后，我们把这个帖子的发布者也记录下来，以备后续使用，因为我们后面系统的通知就是对他发送的
    private int entityUserId;
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
