package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    //点赞
    public void like(int userId,int entityType,int entityId,int entityUserId)
    {
        /*
        重构代码使得在点赞后，增加相应的用户点赞数量，这两个逻辑构成了一个事务。
         */
//        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
//        //判断key中是否存在value值
//        boolean isMember=redisTemplate.opsForSet().isMember(entityLikeKey,userId);
//        if(isMember)
//        {
//            //去除key为entityLikeKey的set中的一个值userId
//            redisTemplate.opsForSet().remove(entityLikeKey,userId);
//        }else{
//            //向key为entityLikeKey的set中添加一个值userId
//            redisTemplate.opsForSet().add(entityLikeKey,userId);
//        }
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
                String userLikeKey= RedisKeyUtil.getUserLikeKey( entityUserId);
                boolean isMember=redisTemplate.opsForSet().isMember(entityLikeKey,userId);
                operations.multi();

                if(isMember)
                {
                    operations.opsForSet().remove(entityLikeKey,userId);
                    operations.opsForValue().decrement(userLikeKey);
                }
                else{
                    operations.opsForSet().add(entityLikeKey,userId);
                    operations.opsForValue().increment(userLikeKey);
                }
                return operations.exec();
        }
    });
    }

    //查询某点赞实体的数量
    public long findEntityLikeCount(int entityType,int entityId)
    {
        String entityLikeKey=RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId,int entityType,int entityId)
    {
        String entityLikeKey=RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        Boolean member = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if(member)
        {
            return 1;
        }
        else {
            return 0;
        }
    }
    //点赞/发帖/评论/都是异步请求，页面不整体刷新



    //查询某个用户获得的赞

    public int findUserLikeCount(int userId)
    {
        String userLikeKey= RedisKeyUtil.getUserLikeKey(userId);
        Integer count= (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count==null?0:count.intValue();
    }

}
