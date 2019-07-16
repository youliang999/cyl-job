package com.cyl.job.core.executor;

import com.alibaba.dubbo.common.utils.NetUtils;
import com.cyl.job.core.handler.IJobHandler;
import com.cyl.job.core.serializer.HessianSerializer;
import com.cyl.job.core.service.AdminBiz;
import com.cyl.job.core.log.CylJobFileAppender;
import com.cyl.job.core.service.impl.AdminBizImpl;
import com.cyl.job.core.thread.ExecutorRegistryThread;
import com.cyl.job.core.thread.JobLogFileCleanThread;
import com.cyl.job.core.thread.JobThread;
import com.cyl.job.core.thread.TriggerCallbackThread;
import com.cyl.api.util.IpUtil;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CylJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CylJobExecutor.class);

    //-----------param-----------------
    private String adminAddress;
    private String appName;
    private String ip;
    private int port;
    private String accessToken;
    private String logPath;
    private int logRetentionDays;

    //-----------------start + stop ------------------------
    public void start() {
        logger.info("========================");
        logger.info("========================");
        logger.info("=====executor start=====");
        logger.info("========================");
        logger.info("========================");
        //init logpath
        CylJobFileAppender.initLogPath(logPath);

        //init adminBizList
        initAdminBizList(adminAddress, accessToken);

        //init JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        //init TriggerCallbackThread
        TriggerCallbackThread.getInstance().start();

        //init executor-server

        port = port > 0 ? port : NetUtils.getAvailablePort(9999);
        ip = (ip != null && ip.trim().length() > 0) ? ip : IpUtil.getIp();
//        String address = IpUtil.getIpPort(ip, port);
//        logger.info("register appName:{}, address:{}", appName, address);
//        ExecutorRegistryThread.getInstance().start(appName, address);
        logger.info("CylJobExecutor启动成功......");
    }
    

    public void destroy() {
        logger.info("=========================");
        logger.info("=========================");
        logger.info("=====executor destroy=====");
        logger.info("=========================");
        logger.info("=========================");
        // destory jobThreadRepository
        if (jobThreadRepository.size() > 0) {
            for (Map.Entry<Integer, JobThread> item : jobThreadRepository.entrySet()) {
                removeJobThread(item.getKey(), "web container destroy and kill the job.");
            }
            jobThreadRepository.clear();
        }
        jobThreadRepository.clear();

        //destory jobLogFileCleanThread
        JobLogFileCleanThread.getInstance().toStop();

        //destory TriggerCallbackThread
        TriggerCallbackThread.getInstance().toStop();

    }

    private static HessianSerializer serializer;
    public static HessianSerializer getSerializer() {
        return serializer;
    }

    // ---------------------- admin-client (rpc invoker) ----------------------
    private static List<AdminBiz> adminBizList;
    private static void initAdminBizList(String adminAddress, String accessToken) {
        try {
            serializer = HessianSerializer.class.newInstance();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (adminAddress != null && adminAddress.trim().length() > 0) {
            for (String address : adminAddress.trim().split(",")) {
                if (address != null && address.trim().length() > 0) {
                    AdminBiz adminBiz = new AdminBizImpl();
                    if (adminBizList == null) {
                        adminBizList = new ArrayList<>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }

    }

    public static List<AdminBiz> getAdminBizList(){
        return adminBizList;
    }


    //-----------------------------job handler repository----------------------
    private static ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<>();

    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
        logger.info(">>>>>> cyl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }

    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }

    //-----------------------------job thread repository-------------------------
    private static ConcurrentHashMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    public static JobThread registJobThread(int jobId, IJobHandler jobHandler, String removeOldReson) {
        JobThread newJobThread = new JobThread(jobId, jobHandler);
        newJobThread.start();
        logger.info(">>>>>>>>>>>  cyl-job regist JobThread success, jobId:{}, handler:{}", new Object[]{jobId, jobHandler});

        JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReson);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }

    public static void removeJobThread(int jobId, String removeOldReason){
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }
    }

    public static JobThread loadJobThread(int jobId) {
        JobThread jobThread = jobThreadRepository.get(jobId);
        return jobThread;
    }

    public void setAdminAddress(String adminAddress) {
        this.adminAddress = adminAddress;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
}
