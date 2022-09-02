package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    //我们在点赞的时候需要传入点赞的是那个帖子或者那个回复，故需要传入postId
    @RequestMapping(path="/like",method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String like(int entityType,int entityId,int entityUserId,int postId)
    {
        User user=hostHolder.getUser();
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        //数量
        long likeCount=likeService.findEntityLikeCount(entityType,entityId);

        //状态
        int likeStatus=likeService.findEntityLikeStatus(user.getId(), entityType,entityId);

        Map<String,Object>map =new HashMap<>();

        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);
        //是否触发点赞事件,因为我们的点赞有两个作用，一个是点赞，一个是取消点赞
        if(likeStatus==1)
        {
            Event event=new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityId(entityId)
                    .setEntityType(entityType)
                    .setEntityUserId(entityUserId)
                    .setData("postId",postId);
        eventProducer.fireEvent(event);
        }
        //判断是不是帖子的赞，因为我们还可能对
        if(entityType==ENTITY_TYPE_POST)
        {
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }
        //因为我们这里不是整个的刷新页面，所以上面的map数据不是放到model中，也没有model这个对象
        return CommunityUtil.getJSONString(0,null,map);
    }
}
