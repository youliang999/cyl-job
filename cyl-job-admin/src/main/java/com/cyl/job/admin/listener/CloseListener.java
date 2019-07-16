package com.cyl.job.admin.listener;

import com.cyl.job.admin.register.FrameLessCylJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class CloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(CloseListener.class);
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("程序停止。。。");
        FrameLessCylJobConfig.getInstance().destorycylJobExecutor();

        logger.info("========================================");
        logger.info("====== FrameLessCylJobConfig destroy ===");
        logger.info("========================================");
    }
}
