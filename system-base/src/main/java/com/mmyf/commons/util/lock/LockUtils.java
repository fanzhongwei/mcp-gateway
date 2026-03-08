package com.mmyf.commons.util.lock;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.google.common.collect.Maps;
import com.mmyf.commons.exception.ResourceUpdateFailedException;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.service.DbOperateService;
import com.mmyf.commons.util.config.ConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


/**
 * package com.mmyf.commons.util.lock
 * description: 锁工具类，支持redisson分布式锁，没有没有开启redisson则使用本地锁
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-09 17:50:08
 */
@Component
@Slf4j
public class LockUtils {

    @Autowired(required = false)
    private RedissonClient redissonClient;

    private static RedissonClient REDISSONC_LIENT;

    // 创建一个新事务，如果当前存在事务，则把当前事务挂起
    // 如果选择默认的传播行为：PROPAGATION_REQUIRES（创建一个事务，如果当前已存在事务则加入到这个事务中），如果tryLockAndRun被嵌套到其它事务中使用，可能会导致锁长时间被占用，知道外层事务结束后才释放锁
    private static TransactionDefinition TRANSACTION_DEFINITION = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    private static PlatformTransactionManager TRANSACTION_MANAGER;

    @Autowired
    private DbOperateService dbOperateService;
    private static DbOperateService DB_OPERATE_SERVICE;

    @PostConstruct
    public void init() {
        // 开启redisson时才设置
        if (ConfigUtils.isRedisEnabled()) {
            REDISSONC_LIENT = redissonClient;
        }
        TRANSACTION_MANAGER = platformTransactionManager;
        DB_OPERATE_SERVICE = dbOperateService;
    }

    private static final Map<String, ReentrantLock> LOCAL_LOCK_MAP = Maps.newConcurrentMap();
    private static final Map<String, RLock> REDIS_LOCK_MAP = Maps.newConcurrentMap();

    /**
     * 根据key加锁
     * @param key 要加锁的关键字
     */
    public static void lock(String key) {
        // 分布式锁
        if (null != REDISSONC_LIENT) {
            REDIS_LOCK_MAP.computeIfAbsent(key, k -> REDISSONC_LIENT.getLock(key)).lock();
        } else {
            // 本地锁
            LOCAL_LOCK_MAP.computeIfAbsent(key, k -> new ReentrantLock()).lock();
        }
    }

    /**
     * 根据key解锁
     * @param key 要解锁的关键字
     */
    public static void unlock(String key) {
        // 分布式锁
        if (null != REDISSONC_LIENT) {
            RLock rLock = REDIS_LOCK_MAP.get(key);
            if (null == rLock) {
                // REDIS_LOCK_MAP.remove时无法判断有哪些线程正在请求锁，先将其移除，后面解锁时发现REDIS_LOCK_MAP中没有锁则重新从REDISSONC_LIENT获取一个
                rLock = REDISSONC_LIENT.getLock(key);
            }
            rLock.unlock();
            // 这里无法判断有哪些线程正在请求锁，先将其移除，后面解锁时发现REDIS_LOCK_MAP中没有锁则重新从REDISSONC_LIENT获取一个
            if (rLock.getHoldCount() == 0) {
                REDIS_LOCK_MAP.remove(key);
            }
        } else {
            ReentrantLock reentrantLock = LOCAL_LOCK_MAP.get(key);
            reentrantLock.unlock();
            // 没有线程持有，并且没有线程在请求，那么unlock后将锁移除
            if (reentrantLock.getHoldCount() == 0 && reentrantLock.getQueueLength() == 0) {
                LOCAL_LOCK_MAP.remove(key);
            }
        }
    }

    /**
     * select for update方式更新数据，保证并发场景下数据一致性
     *
     * @param lockEntityClass 加锁的数据类型
     * @param selectForUpdate 查询条件
     * @param lockExecutor 对加锁数据进行操作执行的方法，返回需要更新的的数据，如果为null，则不更新
     * @return void
     * @date 2024/9/16 下午10:46
     **/
    public static void forUpdateLockAndRun(Class<?> lockEntityClass, AbstractWrapper selectForUpdate, LockExecutor lockExecutor) {
        forUpdateLockAndRun(lockEntityClass, selectForUpdate, lockExecutor, 0);
    }

    private static void forUpdateLockAndRun(Class<?> lockEntityClass, AbstractWrapper selectForUpdate, LockExecutor lockExecutor, int retryTimes) {
        if (retryTimes >= 3) {
            throw new ResourceUpdateFailedException("tryForUpdateLockAndRun失败，重试次数已达到上限，请稍后再试");
        }
        BaseMapper mapper = DB_OPERATE_SERVICE.getMapperByEntityClass(lockEntityClass);
        // TransactionDefinition.PROPAGATION_REQUIRES_NEW：创建一个新事务，如果当前存在事务，则把当前事务挂起
        TransactionStatus transactionStatus = TRANSACTION_MANAGER.getTransaction(TRANSACTION_DEFINITION);
        IEntity lockData = null;
        try {
            selectForUpdate.last(" for update");
            lockData = (IEntity) mapper.selectOne(selectForUpdate);
        } catch (Exception e) {
            log.error("tryForUpdateLockAndRun异常，获取锁失败，进行重试", e);
            TRANSACTION_MANAGER.rollback(transactionStatus);
            forUpdateLockAndRun(lockEntityClass, selectForUpdate, lockExecutor, retryTimes + 1);
            return;
        }
        Assert.notNull(lockData, "tryForUpdateLockAndRun异常，未获取到需要锁定的数据，无法进行重试");
        try {
            IEntity newLockData = lockExecutor.executeLock(lockData);
            if (null != newLockData) {
                selectForUpdate.last("");
                mapper.update(newLockData, selectForUpdate);
            }
            TRANSACTION_MANAGER.commit(transactionStatus);
        } catch (Exception e) {
            log.error("tryForUpdateLockAndRun获取到锁，但更新数据失败，进行重试", e);
            TRANSACTION_MANAGER.rollback(transactionStatus);
            forUpdateLockAndRun(lockEntityClass, selectForUpdate, lockExecutor, retryTimes + 1);
        }
    }

    public static interface LockExecutor {
        IEntity executeLock(IEntity lockData);
    }
}
