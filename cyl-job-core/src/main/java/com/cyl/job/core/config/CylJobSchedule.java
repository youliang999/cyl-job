package com.cyl.job.core.config;

import com.cyl.job.core.helper.JobFailMonitorHelper;
import com.cyl.job.core.helper.JobRegistryMonitorHelper;
import com.cyl.job.core.helper.JobScheduleHelper;
import com.cyl.job.core.helper.JobTriggerPoolHelper;
import com.cyl.job.core.service.ExecutorBiz;
import com.cyl.job.core.service.impl.ExecutorBizImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class CylJobSchedule implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(CylJobSchedule.class);

    @Override
    public void destroy() throws Exception {
        //stop-schedule
        JobScheduleHelper.getInstance().toStop();

        //admin trigger pool stop
        JobTriggerPoolHelper.toStop();

        //admin registry monitor stop
        JobRegistryMonitorHelper.getInstance().toStop();

        //admin fail monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        log.info(">>>>>> stop cyl-job admin success.");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //admin registry monitor run
        JobRegistryMonitorHelper.getInstance().start();
        
        //admin fail monitor run
        JobFailMonitorHelper.getInstance().start();
        
        //start-schedule
        JobScheduleHelper.getInstance().start();

        log.info(">>>>>> init cyl-job admin success.");
    }


    //execute client
    private static ConcurrentHashMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    public static ExecutorBiz getExecutorBiz(String address) {
        //valid
        if (address == null || address.trim().length() == 0) {
            return null;
        }

        //load cache
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }
        executorBiz = new ExecutorBizImpl();

        //set cache
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }


}
