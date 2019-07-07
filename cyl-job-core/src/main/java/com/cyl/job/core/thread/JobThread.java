package com.cyl.job.core.thread;

import com.cyl.job.core.biz.model.HandleCallbackParam;
import com.cyl.job.core.biz.model.ResponseModel;
import com.cyl.job.core.biz.model.TriggerParam;
import com.cyl.job.core.executor.CylJobExecutor;
import com.cyl.job.core.handler.IJobHandler;
import com.cyl.job.core.log.CylJobFileAppender;
import com.cyl.job.core.log.CylJobLogger;
import com.cyl.job.core.util.ShardingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class JobThread extends Thread {
    private static Logger logger = LoggerFactory.getLogger(JobThread.class);

    private int jobId;
    private IJobHandler jobHandler;
    private LinkedBlockingQueue<TriggerParam> triggerQueue;
    private Set<Integer> triggerLogIdSet;       //avoid repeat trigger for the same TRIGGER_LOG_ID

    private volatile boolean toStop = false;
    private String stopReason;

    private boolean running = false; // if running job
    private int idleTimes = 0;      // idle times

    public JobThread(int jobId, IJobHandler jobHandler) {
        jobId = jobId;
        this.jobHandler = jobHandler;
        this.triggerQueue = new LinkedBlockingQueue<>();
        this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<Integer>());
    }

    public IJobHandler getJobHandler() {
        return jobHandler;
    }

    /**
     * new trigger to queue
     */
    public ResponseModel<String> pushTriggerQueue(TriggerParam triggerParam) {
        //avoid reset
        if (triggerLogIdSet.contains(triggerParam.getLogId())) {
            logger.info(">>>>>> repeat trigger job, logId:{}", triggerParam.getLogId());
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "repeat trigger job, logId:" + triggerParam.getLogId());
        }

        triggerLogIdSet.add(triggerParam.getLogId());
        triggerQueue.add(triggerParam);
        return ResponseModel.SUCCESS;
    }

    /**
     * kill job thread
     */
    public void toStop(String stopReason) {
        /**
         * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
         * 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
         * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
         */
        this.toStop = true;
        this.stopReason = stopReason;
    }

    /**
     * is running job
     */
    public boolean isRunningOrHasQueue() {
        return running || triggerQueue.size() > 0;
    }

    @Override
    public void run() {
        //init
        try {
            jobHandler.init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        //execute
        while (!toStop) {
            running = false;
            idleTimes++;

            TriggerParam triggerParam = null;
            ResponseModel<String> executeResult = null;
            try {
                //to check toStop signal, we need cycle, so wo cannot use queue.take(), instand of poll(timeout)
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null) {
                    running = true;
                    idleTimes = 0;
                    triggerLogIdSet.remove(triggerParam.getLogId());

                    //log filename,like "logPath/yyyy-MM-dd/9999.log"
                    String logFileName = CylJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTim()), triggerParam.getLogId());
                    CylJobFileAppender.contextHolder.set(logFileName);
                    ShardingUtil.setShardingVo(new ShardingUtil.ShardingVO(triggerParam.getBroadcastIndex(), triggerParam.getBroadcastTotal()));

                    //execute
                    CylJobLogger.log("<br>---------- cyl-job job execute start --------------<br>------------ Param:" + triggerParam.getExecutorParams());

                    if (triggerParam.getExecutorTimeout() > 0) {
                        //limit time out
                        Thread futureThread = null;
                        try {
                            final TriggerParam triggerParamTmp = triggerParam;
                            FutureTask<ResponseModel<String>> futureTask = new FutureTask<>(new Callable<ResponseModel<String>>() {
                                @Override
                                public ResponseModel<String> call() throws Exception {
                                    return jobHandler.execute(triggerParamTmp.getExecutorParams());
                                }
                            });
                            futureThread = new Thread(futureTask);
                            futureThread.start();
                            executeResult = futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            CylJobLogger.log("<br>--------------- cyl-job job execute timeout");
                            CylJobLogger.log(e);
                            executeResult = new ResponseModel<>(IJobHandler.FAIL_TIMEOUT.getCode(), "job execute timeout");
                        } finally {
                            futureThread.interrupt();
                        }
                    } else {
                        // just execute
                        executeResult = jobHandler.execute(triggerParam.getExecutorParams());
                    }

                    if (executeResult == null) {
                        executeResult = IJobHandler.FAIL;
                    } else {
                        executeResult.setMsg((executeResult != null && executeResult.getMsg() != null && executeResult.getMsg().length() > 50000)
                                ? executeResult.getMsg().substring(0, 50000).concat("...")
                                : executeResult.getMsg());
                        executeResult.setContent(null);
                    }
                    CylJobLogger.log("<br>------------- cyl-job job execute end(finish) ------------<br>-------------Response:" + executeResult);
                } else {
                    if (idleTimes > 30) {
                        CylJobExecutor.removeJobThread(jobId, "execute idel times over limit.");

                    }
                }
            } catch (Throwable e) {
                if (toStop) {
                    CylJobLogger.log("<br>---------- JobThread toStop, stopReason:" + stopReason);
                }
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                String errorMsg = stringWriter.toString();
                executeResult = new ResponseModel<>(ResponseModel.FAIL_CODE, errorMsg);
                CylJobLogger.log("<br>------------------ JobThread Exception: " + errorMsg + "<br>---------------- cyl-job job execute end(error)----------");
            } finally {
                if (triggerParam != null) {
                    //callback handler info
                    if (!toStop) {
                        //common
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(), triggerParam.getLogDateTim(), executeResult));
                    } else {
                        // is killed
                        ResponseModel<String> stopResult = new ResponseModel<>(ResponseModel.FAIL_CODE, stopReason + "[job running, killed]");
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(), triggerParam.getLogDateTim(), stopResult));
                    }
                }
            }
            }
        //callback trigger request in queue
        while (triggerQueue != null && triggerQueue.size() > 0) {
            TriggerParam triggerParam = triggerQueue.poll();
            if (triggerParam != null) {
                //is killed
                ResponseModel<String> stopResult = new ResponseModel<>(ResponseModel.FAIL_CODE, stopReason + " [job not executed, in the job queue, killed.])");
                TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(), triggerParam.getLogDateTim(), stopResult));
            }
        }

        //destory
        try {
            jobHandler.destroy();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        logger.info(">>>>> cyl-job JobThread stoped, hashCode:{}", Thread.currentThread());

    }
}
