package com.cyl.job.core.trigger;


import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobLog;
import com.cyl.job.core.config.CylJobAdminConfig;
import com.cyl.job.core.config.CylJobSchedule;
import com.cyl.api.enums.ExecutorBlockStrategyEnum;
import com.cyl.api.enums.ExecutorRouteStrategyEnum;
import com.cyl.api.enums.TriggerTypeEnum;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.model.TriggerParam;
import com.cyl.api.util.IpUtil;
import java.util.Date;

import com.cyl.job.core.service.ExecutorBiz;
import com.cyl.api.util.ThrowableUtil;
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
                processTrigger(cylJobGroup, cylJobInfo, finalFailRetryCount, triggerTypeEnum);
            }
        } else {
            processTrigger(cylJobGroup, cylJobInfo, finalFailRetryCount, triggerTypeEnum);
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
            triggerResult = runExecutor(triggerParam, address);
        } else {
            triggerResult = new ResponseModel<>(ResponseModel.FAIL_CODE, "执行器地址为空!");
        }
        
        //收集触发信息
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append("任务触发类型").append("：").append(triggerTypeEnum.getTitle());
        triggerMsgSb.append("<br>").append("调度机器").append("：").append(
                IpUtil.getIp());
        triggerMsgSb.append("<br>").append("执行器注册方式").append("：")
                .append( (cylJobGroup.getAddressType() == 0) ? "自动注册" : "手动录入" );
        triggerMsgSb.append("<br>").append("执行器-地址列表").append("：").append(cylJobGroup.getRegistryList());
        triggerMsgSb.append("<br>").append("路由策略").append("：").append(executorRouteStrategyEnum.getTitle());
//        if (shardingParam != null) {
//            triggerMsgSb.append("("+shardingParam+")");
//        }
        triggerMsgSb.append("<br>").append("阻塞处理策略").append("：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>").append("任务超时时间").append("：").append(cylJobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append("失败重试次数").append("：").append(finalFailRetryCount);

        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ "触发调度" +"<<<<<<<<<<< </span><br>")
                .append((routeAddressResult!=null&&routeAddressResult.getMsg()!=null)?routeAddressResult.getMsg()+"<br><br>":"").append(triggerResult.getMsg()!=null?triggerResult.getMsg():"");
        
        //保存触发日志
        cylJobLog.setExecutorAddress(address);
        cylJobLog.setExecutorHandler(cylJobInfo.getExecutorHandler());
        cylJobLog.setExecutorParam(cylJobInfo.getExecutorParam());
        cylJobLog.setExecutorShardingParam("");
        cylJobLog.setExecutorFailRetryCount(finalFailRetryCount);
        
        //日志设置触发时间
        cylJobLog.setTriggerCode(triggerResult.getCode());
        cylJobLog.setTriggerMsg(triggerMsgSb.toString());
        CylJobAdminConfig.getInstance().getCylJobLogDao().updateTriggerInfo(cylJobLog);

        log.info(">>>>>> cyl-job triiger end, jobId:{}", cylJobLog.getId());
    }

    public static ResponseModel<String> runExecutor(TriggerParam triggerParam, String address) {
        ResponseModel<String> runResult = null;
        //执行任务
        try {
            ExecutorBiz executorBiz = CylJobSchedule.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            log.error(">>>>>> cyl-job trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new ResponseModel<>(ResponseModel.FAIL_CODE, ThrowableUtil.toString(e));
        }
        //保存执行记录结果
        StringBuffer resultBf = new StringBuffer("触发调度");
        resultBf.append("<br>address:").append(address);
        resultBf.append("<br>code:").append(runResult.getCode());
        resultBf.append("<br>msg:").append(runResult.getMsg());

        runResult.setMsg(resultBf.toString());
        return runResult;
    }
}
