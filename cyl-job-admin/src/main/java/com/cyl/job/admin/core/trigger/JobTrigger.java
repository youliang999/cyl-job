package com.cyl.job.admin.core.trigger;


import com.cyl.job.admin.core.config.CylJobAdminConfig;
import com.cyl.job.admin.core.model.CylJobGroup;
import com.cyl.job.admin.core.model.CylJobInfo;
import com.cyl.job.admin.core.model.CylJobLog;
import com.cyl.job.admin.enums.ExecutorBlockStrategyEnum;
import com.cyl.job.admin.enums.ExecutorRouteStrategyEnum;
import com.cyl.job.admin.enums.TriggerTypeEnum;
import com.cyl.job.core.biz.model.ResponseModel;
import com.cyl.job.core.biz.model.TriggerParam;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobTrigger {
    private static Logger log = LoggerFactory.getLogger(JobTrigger.class);

    public static void trigger(int jobId, TriggerTypeEnum triggerTypeEnum, int failRetryCount, String executorParam) {
        CylJobInfo cylJobInfo = CylJobAdminConfig.getInstance().getCylJobInfoDao().loadById(jobId);
        if(cylJobInfo == null) {
            log.warn("[WARN]>>>>>> trigger fail, jobInfo invalid, jobId:{}", jobId);
        }
        if (executorParam != null) {
            cylJobInfo.setExecutorParam(executorParam);
        }
        final int finalFailRetryCount = failRetryCount > 0 ? failRetryCount : cylJobInfo.getExecutorFailRetryCount();
        CylJobGroup cylJobGroup = CylJobAdminConfig.getInstance().getCylJobGroupDao().load(cylJobInfo.getJobGroup());
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum
                .match(cylJobInfo.getExecutorBlockStrategy(), null) && cylJobGroup.getRegistryList() != null
                && !cylJobGroup.getRegistryList().isEmpty()) {
            for (int i = 0; i < cylJobGroup.getRegistryList().size(); i++) {

            }
        } else {

        }
    }


    private static void processTrigger(CylJobGroup cylJobGroup, CylJobInfo cylJobInfo, int finalFailRetryCount, TriggerTypeEnum triggerTypeEnum) {
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum
                .match(cylJobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum
                .match(cylJobInfo.getExecutorRouteStrategy(), null);
        // 保存日志
        CylJobLog cylJobLog = new CylJobLog();
        cylJobLog.setJobGroup(cylJobInfo.getJobGroup());
        cylJobLog.setJobId(cylJobInfo.getId());
        cylJobLog.setTriggerTime(new Date());
        CylJobAdminConfig.getInstance().getCylJobLogDao().save(cylJobLog);
        log.info("[INFO]>>>>>> cyl-job trigger start, jobLogId:{}", cylJobLog.getId());

        //初始化 触发任务参数
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(cylJobInfo.getId());
        triggerParam.setExecutorHandler(cylJobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(cylJobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(cylJobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(cylJobInfo.getExecutorTimeout());
        triggerParam.setLogId(cylJobLog.getId());
        triggerParam.setLogDateTim(cylJobLog.getTriggerTime().getTime());
        triggerParam.setGlueType(cylJobInfo.getGlueType());
        triggerParam.setGlueSource(cylJobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(cylJobInfo.getGlueUpdatetime().getTime());
        triggerParam.setBroadcastIndex(1);
        triggerParam.setBroadcastTotal(1);

        //初始化地址
        String address = null;
        ResponseModel<String> routeAddressResult = null;
        if (cylJobGroup.getRegistryList() != null && !cylJobGroup.getRegistryList().isEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                address = cylJobGroup.getRegistryList().get(0);
            } else {
                routeAddressResult = executorRouteStrategyEnum.getRouter()
                        .route(triggerParam, cylJobGroup.getRegistryList());
                if (routeAddressResult.getCode() == ResponseModel.SUCCESS_CODE) {
                    address = routeAddressResult.getContent();
                }
            }
        } else {
            routeAddressResult = new ResponseModel<>(ResponseModel.FAIL_CODE, "调度失败：执行器地址为空");
        }
        //触发执行器
        ResponseModel<String> triggerResult = null;
        if(address != null) {
//todo            triggerResult = 
        } else {
            triggerResult = new ResponseModel<>(ResponseModel.FAIL_CODE, "执行器地址为空!");
        }
    }

    public static ResponseModel<String> runExecutor(TriggerParam triggerParam, String address) {
        ResponseModel<String> runResult = null;
        //执行任务

        //保存执行记录结果
        StringBuffer resultBf = new StringBuffer("触发调度");
        resultBf.append("<br>address:").append(address);
        resultBf.append("<br>code:").append(runResult.getCode());
        resultBf.append("<br>msg:").append(runResult.getMsg());

        runResult.setMsg(resultBf.toString());
        return runResult;
    }
}
