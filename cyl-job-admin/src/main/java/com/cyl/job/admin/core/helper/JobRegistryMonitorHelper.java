package com.cyl.job.admin.core.helper;

import com.cyl.job.admin.core.config.CylJobAdminConfig;
import com.cyl.job.admin.core.config.RegistryConfig;
import com.cyl.job.admin.core.config.RegistryConfig.RegistType;
import com.cyl.job.admin.core.model.CylJobGroup;
import com.cyl.job.admin.core.model.CylJobRegistry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobRegistryMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryMonitorHelper.class);

    private static JobRegistryMonitorHelper instance = new JobRegistryMonitorHelper();
    public static JobRegistryMonitorHelper getInstance(){
        return instance;
    }

    private Thread registryThread;
    private volatile boolean toStop = false;

    public void start() {
        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        List<CylJobGroup> cylJobGroups = CylJobAdminConfig.getInstance().getCylJobGroupDao().findByAddressType(0);
                        if (cylJobGroups != null && cylJobGroups.isEmpty()) {
                          
                            // remove dead address (admin/executor)
                            CylJobAdminConfig.getInstance().getCylJobRegistryDao().removeDead(RegistryConfig.DEAD_TIMEOUT);
                            
                            // fresh online address (admin/executor)
                            HashMap<String, List<String>> addressMap = new HashMap<>();
                            List<CylJobRegistry> list = CylJobAdminConfig.getInstance().getCylJobRegistryDao()
                                    .findAll(RegistryConfig.DEAD_TIMEOUT);
                            if (list != null) {
                                for (CylJobRegistry item : list) {
                                    if (RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                        String appName = item.getRegistryKey();
                                        List<String> registryList = addressMap.get(appName);
                                        if (registryList == null) {
                                            registryList = new ArrayList<>();
                                        }
                                        if (!registryList.contains(item.getRegistryValue())) {
                                            registryList.add(item.getRegistryValue());
                                        }
                                        addressMap.put(appName, registryList);
                                    }
                                }
                            }
                            // fresh group address
                            for (CylJobGroup group : cylJobGroups) {
                                List<String> registryList = addressMap.get(group.getAppName());
                                String addressListStr = null;
                                if (registryList != null && !registryList.isEmpty()) {
                                    Collections.sort(registryList);
                                    addressListStr = "";
                                    for (String item : registryList) {
                                        addressListStr += item + ",";
                                    }
                                    addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
                                }
                                group.setAddressList(addressListStr);
                                CylJobAdminConfig.getInstance().getCylJobGroupDao().update(group);
                            }
                     
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error("[ERROR]>>>>>> cyl-job, job registry monitor thread error:{}", e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.DEAD_TIMEOUT);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error("[ERROR]>>>>>> cyl-job, job registry monitor thread error:{}", e);
                        }
                    }
                }
                logger.info("[ERROR] >>>>>> cyl-job, job registry monitor thread stop");
            }
        });
        registryThread.setDaemon(true);
        registryThread.setName("cyl-job, admin JobRegistryMonitorHelper");
        registryThread.start();
    }


    private Date getTimeout() {
        LocalDateTime localDateTime = LocalDateTime.now().plusSeconds(-RegistryConfig.DEAD_TIMEOUT);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date timeout = Date.from(instant);
        return timeout;
    }
}
