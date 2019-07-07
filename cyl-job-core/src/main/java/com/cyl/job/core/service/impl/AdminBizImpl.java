package com.cyl.job.core.service.impl;

import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.dao.CylJobLogDao;
import com.cyl.api.dao.CylJobRegistryDao;
import com.cyl.job.core.biz.model.HandleCallbackParam;
import com.cyl.job.core.biz.model.RegistryParam;
import com.cyl.job.core.biz.model.ResponseModel;
import com.cyl.job.core.service.AdminBiz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AdminBizImpl implements AdminBiz {
    private static final Logger logger = LoggerFactory.getLogger(AdminBizImpl.class);

    @Resource
    public CylJobLogDao cylJobLogDao;
    @Resource
    private CylJobInfoDao cylJobInfoDao;
    @Resource
    private CylJobRegistryDao cylJobRegistryDao;

    @Override
    public ResponseModel<String> callback(List<HandleCallbackParam> callbackParamList) {
        return null;
    }

    @Override
    public ResponseModel<String> registry(RegistryParam registryParam) {
        return null;
    }

    @Override
    public ResponseModel<String> registryRemove(RegistryParam registryParam) {
        return null;
    }
}
