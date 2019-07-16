package com.cyl.job.core.service.impl;

import com.cyl.api.enums.ExecutorBlockStrategyEnum;
import com.cyl.api.model.LogResult;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.model.TriggerParam;
import com.cyl.job.core.executor.CylJobExecutor;
import com.cyl.api.glue.GlueTypeEnum;
import com.cyl.job.core.handler.IJobHandler;
import com.cyl.job.core.log.CylJobFileAppender;
import com.cyl.job.core.service.ExecutorBiz;
import com.cyl.job.core.thread.JobThread;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExecutorBizImpl implements ExecutorBiz {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    @Override
    public ResponseModel<String> beat() {
        return ResponseModel.SUCCESS;
    }

    //不清楚呀
    @Override
    public ResponseModel<String> idleBeat(int jobId) {
        //isRunningOrHasQueue
        boolean isRunningOrHasQueue = false;
        JobThread jobThread = CylJobExecutor.loadJobThread(jobId);
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            isRunningOrHasQueue = true;
        }
        if (isRunningOrHasQueue) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "job thread is running or has trigger queue");
        }
        return ResponseModel.SUCCESS;
    }

    @Override
    public ResponseModel<String> kill(int jobId) {
        //kill handlerThread, and  create new one
        JobThread jobThread = CylJobExecutor.loadJobThread(jobId);  
        if (jobThread != null) {
            CylJobExecutor.removeJobThread(jobId, "schedule center kill job");
            return ResponseModel.SUCCESS;
        }
        return new ResponseModel<>(ResponseModel.SUCCESS_CODE, "job thread already killed.");
    }

    @Override
    public ResponseModel<LogResult> log(long logDateTim, int logId, int fromLineNum) {
        //log  filename: logPath/yyyy-MM-dd/9999.log
        String logFileName = CylJobFileAppender.makeLogFileName(new Date(logDateTim), logId);
        LogResult logResult = CylJobFileAppender.readLog(logFileName, fromLineNum);
        return new ResponseModel<>(logResult);
    }

    @Override
    public ResponseModel<String> run(TriggerParam triggerParam) {
        logger.info("biz run...");
        // load old: jobHandler + jobThread
        JobThread jobThread = CylJobExecutor.loadJobThread(triggerParam.getJobId());
        IJobHandler jobHandler = jobThread != null ? jobThread.getJobHandler() : null;
        String removeReson = null;
        
        //valid jobhander + jobthread
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        if (GlueTypeEnum.BEAN == glueTypeEnum) {
            //new JobHandler 
            IJobHandler iJobHandler = CylJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

            //valid old jobThread
            if (jobThread != null && jobHandler != iJobHandler) {
                //changer handler, need kill old thread
                removeReson = "change jobhandler or glueType and terminate the old job thread";

                jobThread = null;
                jobHandler = null;
            }

            //valid handler
            if (jobHandler == null) {
                jobHandler = iJobHandler;
                if (jobHandler == null) {
                    return new ResponseModel<>(ResponseModel.FAIL_CODE,
                            "job handler [" + triggerParam.getExecutorHandler() + "] not found");
                }
            }
        } //todo add other type...... 
        else {
            return new ResponseModel<>(ResponseModel.FAIL_CODE,
                    "glueType[" + triggerParam.getGlueType() + "] not valid");
        }

        if (jobThread != null) {
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum
                    .match(triggerParam.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                //discard when running 
                if (jobThread.isRunningOrHasQueue()) {
                    return new ResponseModel<>(ResponseModel.FAIL_CODE,
                            "block strategy effect: " + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                //kill running jobThread
                if (jobThread.isRunningOrHasQueue()) {
                    removeReson = "block strategy effect: " + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
                    jobThread = null;
                }
            } else {
                // just queue trigger 
            }
        }
            //replace thread (new or exists invalid)
            if (jobThread == null) {
                jobThread = CylJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeReson);
            }
            
            //push data to queue 
            ResponseModel<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
            return pushResult;
            
    }
}
