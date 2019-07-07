package com.cyl.job.core.handler;

import com.cyl.job.core.biz.model.ResponseModel;

public abstract class IJobHandler {


    /** success */
    public static final ResponseModel<String> SUCCESS = new ResponseModel<String>(200, null);
    /** fail */
    public static final ResponseModel<String> FAIL = new ResponseModel<String>(500, null);
    /** fail timeout */
    public static final ResponseModel<String> FAIL_TIMEOUT = new ResponseModel<String>(502, null);


    /**
     * execute handler, invoked when executor receives a scheduling request
     *
     * @param param
     * @return
     * @throws Exception
     */
    public abstract ResponseModel<String> execute(String param) throws Exception;


    /**
     * init handler, invoked when JobThread init
     */
    public void init() {
        // do something
    }


    /**
     * destroy handler, invoked when JobThread destroy
     */
    public void destroy() {
        // do something
    }
}
