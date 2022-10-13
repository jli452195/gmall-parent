package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheImpl {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //定义环绕通知
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object gmallCacheJoinPoint(ProceedingJoinPoint point) throws Throwable {
        Object object = null;
        /*
         * 1. 先获取Key
         *   key = 由 注解的前缀+方法参数获取
         *       先获取到注解，再获取注解的前缀
         * 2. 根据 key 来获取缓存数据
         *   true:
         *       return
         *   false:
         *       getDB
         *       分布式锁业务逻辑
         * */
        Object[] args = point.getArgs();
        System.out.println("args = " + args);

        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();

        // 组成缓存key = sku
        String key = prefix + Arrays.asList(args).toString();

        // 获取缓存数据
        try {
            //  this.redisTemplate.opsForValue().get(key);
            object = this.getRedisData(key, methodSignature);
            //判断
            if (object == null) {
                // 拦截一道 locKey
                String locKey = key + ":lock";
                RLock lock = redissonClient.getLock(locKey);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res) {
                    try {
                        //说明缓存中没有
                        object = point.proceed(); // 通过这个方法，能够找到当前注解所在的方法，并执行这个方法体！
                        if (object == null) {
                            // 声明一个对象
                            Object object1 = new Object();
                            this.redisTemplate.opsForValue().set(key, JSON.toJSONString(object1), RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return object1;
                        }
                        // 将数据放入缓存
                        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(object), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        // 返回数据
                        return object;
                    } finally {
                        // 解锁
                        lock.unlock();
                    }
                }else {
                    Thread.sleep(200);
                    return gmallCacheJoinPoint(point);
                }
            }else {
                // 直接返回数据
                return object;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        // 查询数据库
        return point.proceed();
    }

    /**
     * 从缓存中获取数据
     * @param key
     * @param methodSignature
     * @return
     */
    private Object getRedisData(String key, MethodSignature methodSignature) {
        //调用命令
        String strJson = (String) this.redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(strJson)) {
            //返回具体的数据类型，并不是字符串了
            return JSON.parseObject(strJson, methodSignature.getReturnType());
        }
        // 默认返回空
        return null;
    }

}
