package com.cyl.job.admin.listener;

import com.cyl.job.admin.register.FrameLessCylJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class StartListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartListener.class);
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // todo
        logger.info("开始。。。");
        logger.info("FrameLessCylJobConfig init。。。");
        FrameLessCylJobConfig.getInstance().initcylJobExecutor();
   
    }
}
