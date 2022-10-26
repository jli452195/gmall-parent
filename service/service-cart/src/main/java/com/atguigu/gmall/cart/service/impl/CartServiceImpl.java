package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private ProductFeignClient productFeignClient;

    // 添加购物车
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
         * 1、先判断这个购物车中是否有该商品
         *       true:
         *           数量增加
         *       false:
         *           直接添加购物车
         *
         * 2、查询的时候按照修改的时间排序
         *
         * 3、每次添加都是默认选中状态
         *
         * 4、商品的实时价格
         * */
        // 购物车中的key = user:userId:cart
        String cartKey = getCartKey(userId);
        // hget key field;
        CartInfo cartInfoExist = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        // 判断
        if (cartInfoExist != null) {
            // 数量增加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            // 设置一下修改时间
            // 设置选中状态
            if (cartInfoExist.getIsChecked().intValue() == 0) {
                cartInfoExist.setIsChecked(1);
            }
            //设置实时价格：sku_info.price
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            // 写入缓存
//            redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        } else {
            // 说明这个商品再购物车中不存在
            cartInfoExist = new CartInfo();
            // 获取skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            // 赋值
            cartInfoExist.setUserId(userId);
            cartInfoExist.setSkuId(skuId);
            cartInfoExist.setSkuNum(skuNum);
            cartInfoExist.setCartPrice(skuInfo.getPrice());
            cartInfoExist.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoExist.setSkuName(skuInfo.getSkuName());
            cartInfoExist.setCreateTime(new Date());
            cartInfoExist.setUpdateTime(new Date());
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));

        }
        // 缓存
        redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfoExist);

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        // 获取购物车的key
        String cartKey = getCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.boundHashOps(cartKey).values();
        // 遍历集合 获取选中商品
        List<CartInfo> cartInfos = cartInfoList.stream().filter(cartInfo -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        // 返回选择购物车
        return cartInfos;
    }

    // 删除购物车
    @Override
    public void deleteCart(Long skuId, String userId) {
        // hdel key = field;
        String cartKey = getCartKey(userId);
        // 直接删除
        redisTemplate.boundHashOps(cartKey).delete(skuId.toString());

    }

    // 修改状态
    @Override
    public void checkCart(Long skuId, String userId, Integer isChecked) {
        // hget key field;
        String cartKey = getCartKey(userId);
        CartInfo cartInfo = (CartInfo) redisTemplate.boundHashOps(cartKey).get(skuId.toString());
        if (cartInfo != null) {
            cartInfo.setIsChecked(isChecked);
        }
        // hset key field value
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfo);

    }

    // 查看购物车列表
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        // 声明 登录 购物车集合
        List<CartInfo> cartInfoLoginList = new ArrayList<>();
        // 声明未登录购物车集合
        List<CartInfo> cartInfoNoLoginList = new ArrayList<>();

        //  登录userId 为空！可能临时用户Id
        //  userId = null  userTempId != null
        //  userId = null  userTempId == null
        //  userId !=null  userTempId != null 合并！
        //  userId !=null  userTempId == null 不合并！
        // 一定能够获取到未登录的购物车集合
        if (!StringUtils.isEmpty(userTempId)) {
            // 临时用户id 购物车的key
            String cartKey = getCartKey(userTempId);
            // 可以获取到临时用户购物车
            cartInfoNoLoginList = redisTemplate.opsForHash().values(cartKey);
        }
        // 判断{userId != null} 一定的未登录购物车集合数据
        if (!CollectionUtils.isEmpty(cartInfoNoLoginList) && StringUtils.isEmpty(userId)) {
            cartInfoNoLoginList.sort((o1, o2) -> {
                // 降序排列
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
            });
            // 返回未购物车集合
            return cartInfoNoLoginList;
        }

        //  考虑userId !=null  userTempId != null 合并！
        //  考虑userId !=null  userTempId == null 不合并！
        // 获取到登录购物车集合
        String cartKey = getCartKey(userId);
        // BoundHashOperations<H, HK, HV>
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        // 未登录购物车有数据
        if (!CollectionUtils.isEmpty(cartInfoNoLoginList)) {
            // 发生合并
            for (CartInfo cartNoLoginInfo : cartInfoNoLoginList) {
                //有相同skuId
                if (boundHashOperations.hasKey(cartNoLoginInfo.getSkuId().toString())) {
                    // skuId 相同时 数量相加
                    CartInfo cartLoginInfo = boundHashOperations.get(cartNoLoginInfo.getSkuId().toString());
                    // 17 18
                    cartLoginInfo.setSkuNum(cartLoginInfo.getSkuNum() + cartNoLoginInfo.getSkuNum());
                    // 修改更新时间
                    cartLoginInfo.setUpdateTime(new Date());
                    // 是否选中！
                    if (cartNoLoginInfo.getIsChecked().intValue() == 1) {
                        // 登录时为 0、将未登录赋值为1
                        if (cartLoginInfo.getIsChecked().intValue() == 0) {
                            cartLoginInfo.setIsChecked(1);
                        }
                    }
                    // boundHashOperations.put(cartNoLoginInfo.getSkuId().toString(),cartLoginInfo);
                    redisTemplate.opsForHash().put(cartKey, cartNoLoginInfo.getSkuId().toString(), cartLoginInfo);
                } else {
                    // 未有相同的数据 19
                    // 细节：将未登录的userId 替换为已登录的userId
                    cartNoLoginInfo.setUserId(userId);
                    //  cartNoLoginInfo.setSkuPrice(this.productFeignClient.getSkuPrice(cartNoLoginInfo.getSkuId()));
                    cartNoLoginInfo.setCreateTime(new Date());
                    cartNoLoginInfo.setUpdateTime(new Date());
                    // 添加数据
                    redisTemplate.opsForHash().put(cartKey, cartNoLoginInfo.getSkuId().toString(), cartNoLoginInfo);
                }
            }
            // 删除未登录购物车数据
            redisTemplate.delete(getCartKey(userTempId));
        }
        // 获取到所有的数据 17,18,19
        cartInfoLoginList = boundHashOperations.values();
        // 遍历 设置排序
        if (CollectionUtils.isEmpty(cartInfoLoginList)) {
            // 为空返回
            return new ArrayList<>();
        }
        // 排序
        cartInfoLoginList.sort((o1, o2) -> {
            // 降序排列
            return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
        });
        // 返回为登录购物车集合
        return cartInfoLoginList;

    }

    // 获取购物车中的key
    private String getCartKey(String userId) {
        String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }
}
