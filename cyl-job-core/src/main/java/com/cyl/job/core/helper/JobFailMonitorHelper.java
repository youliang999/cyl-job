package com.cyl.job.core.helper;

import com.cyl.api.enums.TriggerTypeEnum;
import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobLog;
import com.cyl.job.core.config.CylJobAdminConfig;
import com.cyl.api.model.ResponseModel;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;


public class JobFailMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);
    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();
    public static JobFailMonitorHelper getInstance(){
        return instance;
    }

    // ---------------------- monitor ----------------------

    private Thread monitorThread;
    private volatile boolean toStop = false;

    public void start() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        List<Integer> failLogIds = CylJobAdminConfig.getInstance().getCylJobLogDao()
                                .findFailJobLogIds(1000);
                        if (failLogIds != null && !failLogIds.isEmpty()) {
                            for (int failLogId : failLogIds) {
                                // lock log
                                int lockRet = CylJobAdminConfig.getInstance().getCylJobLogDao()
                                        .updateAlarmStatus(failLogId, 0, -1);
                                if (lockRet < 1) {
                                    continue;
                                }
                                CylJobLog log = CylJobAdminConfig.getInstance().getCylJobLogDao().load(failLogId);
                                CylJobInfo info = CylJobAdminConfig.getInstance().getCylJobInfoDao()
                                        .loadById(log.getJobId());
                                // 1. fail retry monitor
                                if (log.getExecutorFailRetryCount() > 0) {
                                    JobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY,
                                            (log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam());
                                    String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>"+ "失败重试触发" +"<<<<<<<<<<< </span><br>";
                                    log.setTriggerMsg(retryMsg);
                                    CylJobAdminConfig.getInstance().getCylJobLogDao().updateTriggerInfo(log);
                                }
                                
                                //2. fail alarm monitor
                                int newAlarmStatus = 0; // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                                if (info != null && info.getAlarmEmail() != null
                                        && info.getAlarmEmail().trim().length() > 0) {
                                    boolean alarmResult = true;
                                    try {
                                        alarmResult = failAlarmEmail(info, log);
                                    } catch (Exception e) {
                                        alarmResult = false;
                                        logger.error(e.getMessage(), e);
                                    }
                                    newAlarmStatus = alarmResult ? 2 : 3;
                                } else {
                                    newAlarmStatus = 1; 
                                }
                                CylJobAdminConfig.getInstance().getCylJobLogDao()
                                        .updateAlarmStatus(failLogId, -1, newAlarmStatus);
                            }
                        }
                        TimeUnit.SECONDS.sleep(10);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error("[ERROR]>>>>>> cyl-job, job fail monitor thread run error:{}", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> cyl-job, job fail monitor thread stop");
            }
            
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("cyl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }

    private boolean failAlarmEmail(CylJobInfo cylJobInfo, CylJobLog cylJobLog) {
        boolean alarmResult = true;
        
        //send monitor email
        if (cylJobInfo != null && cylJobInfo.getAlarmEmail() != null && cylJobInfo.getAlarmEmail().trim().length() > 0) {
            //alarm Content
            String alarmContent = "Alarm Job LogId=" + cylJobLog.getId();
            if (cylJobLog.getTriggerCode() != ResponseModel.SUCCESS_CODE) {
                alarmContent += "<br>TriggerMsg=<br>" + cylJobLog.getTriggerMsg();
            }
            if (cylJobLog.getHandleCode() > 0 && cylJobLog.getHandleCode() != ResponseModel.SUCCESS_CODE) {
                alarmContent += "<br>HandleCode=" + cylJobLog.getHandleMsg();
            }
            
            //email info
            CylJobGroup group = CylJobAdminConfig.getInstance().getCylJobGroupDao()
                    .load(Integer.valueOf(cylJobInfo.getJobGroup()));
            String personal = "分布式任务调度平台CYL-JOB";
            String title = "任务调度中心监控报警";
            String content = MessageFormat.format(mailBodyTemplate,
                    group!=null?group.getTitle():"null",
                    cylJobInfo.getId(),
                    cylJobInfo.getJobDesc(),
                    alarmContent);
            Set<String> emailSet = new HashSet<>(Arrays.asList(cylJobInfo.getAlarmEmail().split(",")));
            for (String email : emailSet) {
                
                //make email
                try {
                    MimeMessage mimeMessage = CylJobAdminConfig.getInstance().getMailSender().createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(CylJobAdminConfig.getInstance().getEmailUserName(), personal);
                    helper.setTo(email);
                    helper.setSubject(title);
                    helper.setText(content, true);
                    CylJobAdminConfig.getInstance().getMailSender().send(mimeMessage);
                } catch (Exception e) {
                    logger.error("[ERROR] >>>>>> cyl-job, job fail alarm email send error, JobLogId:{}",
                            cylJobLog.getId(), e);
                    alarmResult = false;
                }
            }
        }
        return alarmResult;
    }

    public void toStop() {
        toStop = true;
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }


    // ---------------------- alarm ----------------------

    // email alarm template
    private static final String mailBodyTemplate = "<h5>" + "监控告警明细" + "：</span>" +
            "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
            "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
            "      <tr>\n" +
            "         <td width=\"20%\" >"+ "执行器" +"</td>\n" +
            "         <td width=\"10%\" >"+ "任务ID" +"</td>\n" +
            "         <td width=\"20%\" >"+ "任务描述" +"</td>\n" +
            "         <td width=\"10%\" >"+ "告警类型 "+"</td>\n" +
            "         <td width=\"40%\" >"+ "告警内容" +"</td>\n" +
            "      </tr>\n" +
            "   </thead>\n" +
            "   <tbody>\n" +
            "      <tr>\n" +
            "         <td>{0}</td>\n" +
            "         <td>{1}</td>\n" +
            "         <td>{2}</td>\n" +
            "         <td>"+ "调度失败" +"</td>\n" +
            "         <td>{3}</td>\n" +
            "      </tr>\n" +
            "   </tbody>\n" +
            "</table>";

}
