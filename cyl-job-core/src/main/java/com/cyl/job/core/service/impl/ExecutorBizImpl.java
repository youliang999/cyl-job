package com.cyl.job.core.service.impl;

import com.cyl.job.core.biz.model.LogResult;
import com.cyl.job.core.biz.model.ResponseModel;
import com.cyl.job.core.biz.model.TriggerParam;
import com.cyl.job.core.service.ExecutorBiz;
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
        return null;
    }

    @Override
    public ResponseModel<String> kill(int jobId) {
        return null;
    }

    @Override
    public ResponseModel<LogResult> log(long logDateTim, int logId, int fromLineNum) {
        return null;
    }

    @Override
    public ResponseModel<String> run(TriggerParam triggerParam) {
        return null;
    }
}
