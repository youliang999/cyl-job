package com.cyl.job.core.service.impl;

import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.dao.CylJobLogDao;
import com.cyl.api.dao.CylJobRegistryDao;
import com.cyl.api.enums.TriggerTypeEnum;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobLog;
import com.cyl.api.model.HandleCallbackParam;
import com.cyl.api.model.RegistryParam;
import com.cyl.api.model.ResponseModel;
import com.cyl.job.core.handler.IJobHandler;
import com.cyl.job.core.helper.JobTriggerPoolHelper;
import com.cyl.job.core.service.AdminBiz;
import com.google.gson.Gson;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
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
        for (HandleCallbackParam handleCallbackParam : callbackParamList) {
            ResponseModel<String> callbackResult = callback(handleCallbackParam);
            logger.info(">>>>>> JobApiController.callback {}, handlerCallbackParam={}, callbackResult:{}",
                    (callbackResult.getCode() == IJobHandler.SUCCESS.getCode() ? "success" : "fail"), handleCallbackParam, callbackResult);
        }
        return ResponseModel.SUCCESS;
    }

    private ResponseModel<String> callback(HandleCallbackParam callbackParam) {
        //vaild log item
        CylJobLog log = cylJobLogDao.load(callbackParam.getLogId());
        if (log == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "log item not found.");
        }
        if (log.getHandleCode() > 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "log repeate callback.");
        }
        
        //trigger success, to trigger child job
        String callbackMsg = null;
        if (IJobHandler.SUCCESS.getCode() == callbackParam.getExecuteResult().getCode()) {
            CylJobInfo cylJobInfo = cylJobInfoDao.loadById(log.getJobId());
            if (cylJobInfo != null && cylJobInfo.getChildJobId() != null
                    && cylJobInfo.getChildJobId().trim().length() > 0) {
                callbackMsg = "<br><br><span style=\"color:#00c0ef;\" >>>>>>>>> " + "触发子任务" + "<<<<<<<<< </span><br>";

                String[] childJobIds = cylJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId =
                            (childJobIds[i] != null && childJobIds[i].trim().length() > 0 && isNumeric(childJobIds[i]))
                                    ? Integer.valueOf(childJobIds[i]) : -1;
                    if (childJobId > 0) {
                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null);
                        ResponseModel<String> triggerChildResult = ResponseModel.SUCCESS;

                        //add msg
                        callbackMsg += MessageFormat.format("{0}/{1} [任务ID={2}], 触发{3}, 触发备注: {4} <br>",
                                (i + 1),
                                childJobIds.length,
                                childJobId,
                                (triggerChildResult.getCode() == ResponseModel.SUCCESS_CODE) ? "成功" : "失败",
                                triggerChildResult.getMsg());
                    } else {
                        callbackMsg += MessageFormat.format("{0}/{1} [任务ID={2}], 触发失败, 触发备注: 任务ID格式错误 <br>",
                                (i + 1),
                                childJobIds.length,
                                childJobId);
                    } 
                }
            }
        }
        
        //handle msg
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (callbackParam.getExecuteResult().getMsg() != null) {
            handleMsg.append(callbackParam.getExecuteResult().getMsg());
        }
        if (callbackMsg != null) {
            handleMsg.append(callbackMsg);
        }
        
        //success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(callbackParam.getExecuteResult().getCode());
        log.setHandleMsg(handleMsg.toString());
        return ResponseModel.SUCCESS;
    }

    private boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public ResponseModel<String> registry(RegistryParam registryParam) {
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        logger.info("registryParam: {}", new Gson().toJson(registryParam));
        logger.info("cylJobRegistryDao:", new Gson().toJson(cylJobRegistryDao));
        int ret = cylJobRegistryDao.registryUpdate(registryParam.getRegistGroup(), registryParam.getRegistryKey(),
                registryParam.getRegistryValue());
        if (ret < 1) {
            cylJobRegistryDao.registrySave(registryParam.getRegistGroup(), registryParam.getRegistryKey(),
                    registryParam.getRegistryValue());
        }
        return ResponseModel.SUCCESS;
    }

    @Override
    public ResponseModel<String> registryRemove(RegistryParam registryParam) {
        cylJobRegistryDao.registryDelete(registryParam.getRegistGroup(), registryParam.getRegistryKey(),
                registryParam.getRegistryValue());
        return ResponseModel.SUCCESS;
    }
}
