package com.cyl.job.core.config;


import javax.annotation.Resource;
import javax.sql.DataSource;

import com.cyl.api.dao.CylJobGroupDao;
import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.dao.CylJobLogDao;
import com.cyl.api.dao.CylJobRegistryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class CylJobAdminConfig implements InitializingBean {
    private static Logger log = LoggerFactory.getLogger(CylJobAdminConfig.class);
    private static CylJobAdminConfig cylJobAdminConfig = null;

    public static CylJobAdminConfig getInstance() {
        return cylJobAdminConfig;
    }

    @Value("${cyl.job.accessToken}")
    private String accessToken;

    @Value("${cyl.mail.username}")
    private String emailUserName;

    @Override
    public void afterPropertiesSet() throws Exception {
        cylJobAdminConfig = this;
    }

    @Resource
    private CylJobLogDao cylJobLogDao;

    @Resource
    private CylJobInfoDao cylJobInfoDao;

    @Resource
    private CylJobRegistryDao cylJobRegistryDao;

    @Resource
    private CylJobGroupDao cylJobGroupDao;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private DataSource dataSource;

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailUserName() {
        return emailUserName;
    }

    public CylJobLogDao getCylJobLogDao() {
        return cylJobLogDao;
    }

    public CylJobInfoDao getCylJobInfoDao() {
        return cylJobInfoDao;
    }

    public CylJobRegistryDao getCylJobRegistryDao() {
        return cylJobRegistryDao;
    }

    public CylJobGroupDao getCylJobGroupDao() {
        return cylJobGroupDao;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
