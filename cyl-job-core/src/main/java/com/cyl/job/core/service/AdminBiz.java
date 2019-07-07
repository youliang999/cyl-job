package com.cyl.job.core.service;

import com.cyl.job.core.biz.model.HandleCallbackParam;
import com.cyl.job.core.biz.model.RegistryParam;
import com.cyl.job.core.biz.model.ResponseModel;

import java.util.List;

public interface AdminBiz {

    public static final String MAPPING = "/api";


    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    public ResponseModel<String> callback(List<HandleCallbackParam> callbackParamList);


    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    public ResponseModel<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    public ResponseModel<String> registryRemove(RegistryParam registryParam);
}
