package com.cyl.job.core.service;

import com.cyl.api.model.HandleCallbackParam;
import com.cyl.api.model.RegistryParam;
import com.cyl.api.model.ResponseModel;

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
