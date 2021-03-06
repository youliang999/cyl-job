package com.cyl.job.core.thread;

import com.cyl.api.model.HandleCallbackParam;
import com.cyl.api.model.ResponseModel;
import com.cyl.job.core.config.RegistryConfig;
import com.cyl.job.core.executor.CylJobExecutor;
import com.cyl.job.core.log.CylJobFileAppender;
import com.cyl.job.core.log.CylJobLogger;
import com.cyl.job.core.service.AdminBiz;
import com.cyl.api.util.FileUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TriggerCallbackThread {
    private static final Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return instance;
    }

    /**
     * job result callback queue
     */
    private LinkedBlockingQueue<HandleCallbackParam> callbackQueue = new LinkedBlockingQueue<>();

    public static void pushCallBack(HandleCallbackParam callback) {
        logger.info("callback {}", new Gson().toJson(callback));
        getInstance().callbackQueue.add(callback);
        logger.info(">>>>>> cyl-job, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * callback thread
     */
    private Thread triggerCallbackThread;
    private Thread triggerRetryCallbackThread;
    private volatile boolean toStop = false;

    public void start() {
        //valid
        if (CylJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>> cyl-job, executor callback config all, adminAdress is null.");
            return;
        }

        //callback
        triggerCallbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        HandleCallbackParam callback = getInstance().callbackQueue.poll();
                        if (callback != null) {

                            //callback list param
                            List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                            int drainToNum = getInstance().callbackQueue.drainTo(callbackParamList);
                            callbackParamList.add(callback);

                            //callback, will retry if error
                            if (callbackParamList != null && callbackParamList.size() > 0) {
                                doCallback(callbackParamList);
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                //last callback
                try {

                    List<HandleCallbackParam> callbackParams = new ArrayList<>();
                    int drainToNum = getInstance().callbackQueue.drainTo(callbackParams);
                    if (callbackParams != null && callbackParams.size() > 0) {
                        doCallback(callbackParams);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info(">>>>> cyl-job, executor callback thread destory.");
            }
        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("cyl-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();


        //retry
        triggerRetryCallbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        retryFailCallbackFile();
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>>>>> cyl-job, executor retry callback thread destory.");
            }
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();
    }

    /**
     * tostop
     *
     * @param
     */
    public void toStop() {
        toStop = true;
        //stop callback, interrupt and wait
        triggerCallbackThread.interrupt();
        try {
            triggerCallbackThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        triggerRetryCallbackThread.interrupt();
        try {
            triggerRetryCallbackThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    //do callback will retry if error
    private void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        //callback, will retry if error
        for (AdminBiz adminBiz : CylJobExecutor.getAdminBizList()) {
            try {
                ResponseModel<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && ResponseModel.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>---------- cyl-job job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- cyl-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                callbackLog(callbackParamList, "<br>--------- cyl-job job callback error, errorMsg:" + e.getMessage());
            }
            if (!callbackRet) {
                appendFailCallbackFile(callbackParamList);
            }
        }
    }

    //callback log
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            String logFileName = CylJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            CylJobFileAppender.contextHolder.set(logFileName);
            CylJobLogger.log(logContent);
        }
    }

    //---fail callback file -------------------
    private static String failCallbackFilePath = CylJobFileAppender.getLogBasePath().concat(File.separator).concat("callbacklog").concat(File.separator);
    private static String failCallbackFileName = failCallbackFilePath.concat("cyl-job-callback-{x}").concat(".log");
    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParams) {
        //valid
        if (callbackParams == null && callbackParams.size() > 0) {
            return;
        }

        //append file
        byte[] callbackParamList_bytes = CylJobExecutor.getSerializer().serialize(callbackParams);
        File callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            for(int i=0; i<100; i++) {
                callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        FileUtil.writeFileContent(callbackLogFile, callbackParamList_bytes);
    }

    //retry
    private void retryFailCallbackFile() {
        //valid
        File callbackLogPath = new File(failCallbackFilePath);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile()) {
            callbackLogPath.delete();
        }
        if (!(callbackLogPath.isDirectory() && callbackLogPath.list() != null && callbackLogPath.list().length > 0)) {
            return;
        }

        //load and clear file. retry
        for (File callbackLogFile : callbackLogPath.listFiles()) {
            byte[] callbackParamList_bytes = FileUtil.readFileContent(callbackLogFile);
            List<HandleCallbackParam> callbackParams = (List<HandleCallbackParam>) CylJobExecutor.getSerializer().deserialize(callbackParamList_bytes, HandleCallbackParam.class);

            callbackLogFile.delete();
            doCallback(callbackParams);
        }
    }
}
