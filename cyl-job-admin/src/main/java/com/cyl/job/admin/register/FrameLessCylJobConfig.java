package com.cyl.job.admin.register;

import com.cyl.job.admin.register.handler.MyJobHandler;
import com.cyl.job.core.executor.CylJobExecutor;
import com.cyl.job.admin.register.handler.DemoJobHandler;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xuxueli 2018-10-31 19:05:43
 */
@Configuration
public class FrameLessCylJobConfig implements InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(FrameLessCylJobConfig.class);


    private static FrameLessCylJobConfig instance = new FrameLessCylJobConfig();
    public static FrameLessCylJobConfig getInstance() {
        return instance;
    }


    private CylJobExecutor cylJobExecutor = null;

    /**
     * init
     */
    public void initcylJobExecutor() {
        logger.info("========= FrameLessCylJobConfig  init===============");
        // registry jobhandler
        CylJobExecutor.registJobHandler("demoJobHandler", new DemoJobHandler());
        CylJobExecutor.registJobHandler("myJobHandler", new MyJobHandler());
//        CylJobExecutor.registJobHandler("shardingJobHandler", new ShardingJobHandler());
//        CylJobExecutor.registJobHandler("httpJobHandler", new HttpJobHandler());
//        CylJobExecutor.registJobHandler("commandJobHandler", new CommandJobHandler());

        // load executor prop
        Properties cylJobProp = loadProperties("cyl-job-executor.properties");


        // init executor
        cylJobExecutor = new CylJobExecutor();
        cylJobExecutor.setAdminAddress(cylJobProp.getProperty("cyl.job.admin.addresses"));
        cylJobExecutor.setAppName(cylJobProp.getProperty("cyl.job.executor.appname"));
        cylJobExecutor.setIp(cylJobProp.getProperty("cyl.job.executor.ip"));
        cylJobExecutor.setPort(Integer.valueOf(cylJobProp.getProperty("cyl.job.executor.port")));
        cylJobExecutor.setAccessToken(cylJobProp.getProperty("cyl.job.accessToken"));
        cylJobExecutor.setLogPath(cylJobProp.getProperty("cyl.job.executor.logpath"));
        cylJobExecutor.setLogRetentionDays(Integer.valueOf(cylJobProp.getProperty("cyl.job.executor.logretentiondays")));

        // start executor
        try {
            cylJobExecutor.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * destory
     */
    public void destorycylJobExecutor() {
        if (cylJobExecutor != null) {
            cylJobExecutor.destroy();
        }
    }


    public static Properties loadProperties(String propertyFileName) {
        InputStreamReader in = null;
        try {
            ClassLoader loder = Thread.currentThread().getContextClassLoader();

            in = new InputStreamReader(loder.getResourceAsStream(propertyFileName), "UTF-8");;
            if (in != null) {
                Properties prop = new Properties();
                prop.load(in);
                return prop;
            }
        } catch (IOException e) {
            logger.error("load {} error!", propertyFileName);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("close {} error!", propertyFileName);
                }
            }
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        getInstance().initcylJobExecutor();
    }
}
