package com.cyl.job.core.thread;

import com.cyl.api.model.RegistryParam;
import com.cyl.api.model.ResponseModel;
import com.cyl.job.core.config.RegistryConfig;
import com.cyl.job.core.executor.CylJobExecutor;
import com.cyl.job.core.service.AdminBiz;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xuxueli on 17/3/2.
 */
public class ExecutorRegistryThread {
    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();
    public static ExecutorRegistryThread getInstance(){
        return instance;
    }

    private Thread registryThread;
//    private volatile boolean toStop = false;
    public void start(final String appName, final String address){

        // valid
        if (appName==null || appName.trim().length()==0) {
            logger.warn(">>>>>>>>>>> cyl-job, executor registry config fail, appName is null.");
            return;
        }
        if (CylJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> cyl-job, executor registry config fail, adminAddresses is null.");
            return;
        }
        
        // registry
//                while (!toStop) {
            try {
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                for (AdminBiz adminBiz: CylJobExecutor.getAdminBizList()) {
                    try {
                        ResponseModel<String> registryResult = adminBiz.registry(registryParam);
                        if (registryResult!=null && ResponseModel.SUCCESS_CODE == registryResult.getCode()) {
                            registryResult = ResponseModel.SUCCESS;
                            logger.debug(">>>>>>>>>>> cyl-job registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            break;
                        } else {
                            logger.info(">>>>>>>>>>> cyl-job registry fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                        }
                    } catch (Exception e) {
                        logger.info(">>>>>>>>>>> cyl-job registry error, registryParam:{}", registryParam, e);
                    }

                }
            } catch (Exception e) {
//                        if (!toStop) {
                    logger.error(e.getMessage(), e);
//                        }

            }

            try {
//                        if (!toStop) {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
//                        }
            } catch (InterruptedException e) {
//                        if (!toStop) {
                    logger.warn(">>>>>>>>>>> cyl-job, executor registry thread interrupted, error msg:{}", e.getMessage());
//                        }
            }
        

        // registry remove
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
            for (AdminBiz adminBiz: CylJobExecutor.getAdminBizList()) {
                try {
                    ResponseModel<String> registryResult = adminBiz.registryRemove(registryParam);
                    if (registryResult!=null && ResponseModel.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = ResponseModel.SUCCESS;
                        logger.info(">>>>>>>>>>> cyl-job registry-remove success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                        break;
                    } else {
                        logger.info(">>>>>>>>>>> cyl-job registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                    }
                } catch (Exception e) {
//                            if (!toStop) {
                        logger.info(">>>>>>>>>>> cyl-job registry-remove error, registryParam:{}", registryParam, e);
//                            }

                }

            }
        } catch (Exception e) {
//                    if (!toStop) {
                logger.error(e.getMessage(), e);
//                    }
        }
        logger.info(">>>>>>>>>>> cyl-job, executor registry thread destory.");


    }

//    public void toStop() {
//        toStop = true;
//        // interrupt and wait
//        registryThread.interrupt();
//        try {
//            registryThread.join();
//        } catch (InterruptedException e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

}
