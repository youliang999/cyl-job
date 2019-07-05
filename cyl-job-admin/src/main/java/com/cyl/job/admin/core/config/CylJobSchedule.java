package com.cyl.job.admin.core.config;

import com.cyl.job.admin.core.helper.JobFailMonitorHelper;
import com.cyl.job.admin.core.helper.JobRegistryMonitorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CylJobSchedule implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(CylJobSchedule.class);

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //admin registry monitor run
        JobRegistryMonitorHelper.getInstance().start();
        
        //admin fail monitor run
        JobFailMonitorHelper.getInstance().start();
        
        //admin-server
        
    }
}
