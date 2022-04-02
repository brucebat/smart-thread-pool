package com.brucebat.smart.thread.pool.common.utils;

import com.brucebat.smart.thread.pool.common.ThreadPoolConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工具类
 *
 * @author brucebat
 * @version 1.0
 * @since Created at 2022/4/1 10:39 AM
 */
public class ThreadPoolUtils {


    public static ThreadPoolConfig assembleConfig(String appName, String threadPoolName, ThreadPoolExecutor threadPoolExecutor) {
        if (StringUtils.isBlank(appName) || StringUtils.isBlank(threadPoolName) || Objects.isNull(threadPoolExecutor)) {
            throw new IllegalArgumentException("missing required parameters: appName, threadPoolName or threadPoolExecutor");
        }
        // 进行线程池基本信息组装
        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
        threadPoolConfig.setAppName(appName);
        threadPoolConfig.setThreadPoolName(threadPoolName);
        threadPoolConfig.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        threadPoolConfig.setMaxPoolSize(threadPoolExecutor.getMaximumPoolSize());
        threadPoolConfig.setKeepAliveTime(threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS));
        threadPoolConfig.setTimeUnit(TimeUnit.MILLISECONDS);
        threadPoolConfig.setWorkQueue(threadPoolExecutor.getQueue());
        threadPoolConfig.setThreadFactory(threadPoolExecutor.getThreadFactory());
        threadPoolConfig.setRejectedExecutionHandler(threadPoolExecutor.getRejectedExecutionHandler());
        return threadPoolConfig;
    }

    /**
     * 根据配置信息进行线程池调整
     *
     * @param threadPoolExecutor 待修改线程池配置信息
     * @param corePoolSize       待修改核心线程数
     * @param maxPoolSize        待修改最大线程数
     * @param workQueueSize      待修改工作队列长度
     */
    @SuppressWarnings("unchecked")
    public static void modifyThreadPool(ThreadPoolExecutor threadPoolExecutor, Integer corePoolSize, Integer maxPoolSize, Integer workQueueSize) {
        if (Objects.isNull(threadPoolExecutor) || Objects.isNull(corePoolSize) || Objects.isNull(maxPoolSize) || Objects.isNull(workQueueSize)) {
            throw new IllegalArgumentException("missing required parameters: threadPoolExecutor, corePoolSize, maxPoolSize or workQueueSize");
        }
        if (corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException("corePoolSize cannot be greater than maxPoolSize!");
        }
        // TODO 需要注意以下两个问题：
        //  1. ThreadPoolExecutor中的阻塞队列是无法进行替换的，因为这里使用了final —— 一个小猜想，是否可以继承ThreadPoolExecutor扩展出设置workQueue方法（反射？）;
        //  2. JDK给出的阻塞队列是无法进行动态扩容的，如果想要进行扩容就需要使用自定义阻塞队列 —— 此处需要开发人员使用自定义实现可以进行动态扩容的阻塞队列;
        BlockingQueue<Runnable> oldWorkQueue = threadPoolExecutor.getQueue();
        try {
            Class<?> workQueueClass = Class.forName(oldWorkQueue.getClass().getName());
            BlockingQueue<Runnable> newWorkQueue = (BlockingQueue<Runnable>) workQueueClass.newInstance();
            modifyThreadPool(threadPoolExecutor, corePoolSize, maxPoolSize, newWorkQueue);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据给出的线程池主要配置对待修改的线程池进行内部属性修改
     *
     * @param threadPoolExecutor 待修改的线程池
     * @param corePoolSize       待修改核心线程数
     * @param maxPoolSize        待修改的最大线程数
     * @param workQueue          待修改的工作队列
     */
    public static void modifyThreadPool(ThreadPoolExecutor threadPoolExecutor, Integer corePoolSize, Integer maxPoolSize, BlockingQueue<Runnable> workQueue) {
        // 优先设置最大线程数，防止设置先设置核心线程数不生效
        threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
        threadPoolExecutor.setCorePoolSize(corePoolSize);
        while (!threadPoolExecutor.getQueue().isEmpty()) {
            Runnable work = threadPoolExecutor.getQueue().poll();
            if (Objects.nonNull(work)) {
                workQueue.add(work);
            }
        }
        // TODO 基于原始的阻塞队列是无法进行设置的
    }
}
