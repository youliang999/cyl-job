package com.cyl.job.core.helper;

import com.cyl.api.enums.TriggerTypeEnum;
import com.cyl.job.core.trigger.JobTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class JobTriggerPoolHelper {
    private static final Logger log = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

    //生成实体类
    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    @Value("${trigger.timeout}")
    private long triggerTimeout = 500;

    //触发任务执行线程池
    private ThreadPoolExecutor fastTriggerPool = new ThreadPoolExecutor(
            20,
            50,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> new Thread(r, "cyl-job-admin, JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode()));

    private ThreadPoolExecutor slowTriggerPool = new ThreadPoolExecutor(
            10,
            30,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> new Thread(r, "cyl-job-admin, JobTriggerPoolHelper-slowTriggerPool" + r.hashCode()));

    private volatile long minTime = System.currentTimeMillis() / 1000 / 60; //毫秒转换为分钟

    /**
     * 任务执行超时缓存
     */
    private volatile ConcurrentHashMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();


    /**
     * 添加执行任务
     * @param jobId             任务id
     * @param triggerTypeEnum   触发类型
     * @param failRetryCount    失败重试次数
     * @param executeParam      执行参数
     */
    public void addTrigger(final int jobId, final TriggerTypeEnum triggerTypeEnum, final int failRetryCount, final String executeParam) {
        ThreadPoolExecutor triggerPool = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if(jobTimeoutCount != null && jobTimeoutCount.get() > 0) {
            triggerPool = slowTriggerPool;
        }
        triggerPool.execute(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();

                try {
                    //triger
                    JobTrigger.trigger(jobId, triggerTypeEnum, failRetryCount, executeParam);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    //check time out
                    long minTime_now = System.currentTimeMillis() / 1000 / 60;
                    if (minTime != minTime_now) {
                        minTime = minTime_now;
                        jobTimeoutCountMap.clear();
                    }

                    //inrc timeoutCountMap
                    long cost = System.currentTimeMillis() - start;
                    if(cost > triggerTimeout) {
                        AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        //need todo check
                        if (timeoutCount != null) {
                            timeoutCount.incrementAndGet();
                        }
                    }
                }

            }
        });
    }

    public void stop() {
        log.info("[STOP]>>>>>>开始停止cyl-job线程池...");
        this.fastTriggerPool.shutdown();
        this.slowTriggerPool.shutdown();
        log.info("[STOP]>>>>>>停止cyl-job线程池成功!");
    }

    public static void toStop() {
        helper.stop();
    }

    public static void trigger(int jobId, TriggerTypeEnum triggerTypeEnum, int failRetryCount, String executorParam) {
        helper.addTrigger(jobId, triggerTypeEnum, failRetryCount, executorParam);
    }
}
