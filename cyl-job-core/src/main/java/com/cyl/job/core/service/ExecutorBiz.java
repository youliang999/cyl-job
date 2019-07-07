package com.cyl.job.core.service;

import com.cyl.job.core.biz.model.LogResult;
import com.cyl.job.core.biz.model.ResponseModel;
import com.cyl.job.core.biz.model.TriggerParam;

public interface ExecutorBiz {

    /**
     * beat
     * @return
     */
    public ResponseModel<String> beat();

    /**
     * idle beat
     *
     * @param jobId
     * @return
     */
    public ResponseModel<String> idleBeat(int jobId);

    /**
     * kill
     * @param jobId
     * @return
     */
    public ResponseModel<String> kill(int jobId);

    /**
     * log
     * @param logDateTim
     * @param logId
     * @param fromLineNum
     * @return
     */
    public ResponseModel<LogResult> log(long logDateTim, int logId, int fromLineNum);

    /**
     * run
     * @param triggerParam
     * @return
     */
    public ResponseModel<String> run(TriggerParam triggerParam);

}
