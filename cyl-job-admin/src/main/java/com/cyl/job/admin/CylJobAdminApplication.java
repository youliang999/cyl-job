package com.cyl.job.admin;

import static com.cyl.job.admin.register.FrameLessCylJobConfig.loadProperties;

import com.cyl.api.util.IpUtil;
import com.cyl.job.admin.listener.CloseListener;
import com.cyl.job.admin.listener.StartListener;
import com.cyl.job.core.App;
import com.cyl.job.core.thread.ExecutorRegistryThread;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {"com.cyl"})
@MapperScan("com.cyl.api.dao")
@EnableTransactionManagement
public class CylJobAdminApplication {

	public static void main(String[] args) {
		try {

			SpringApplication app = new SpringApplication(CylJobAdminApplication.class);
//			app.addListeners(new StartListener());
			app.addListeners(new CloseListener());
			app.run();
			//init register
			// load executor prop
//			TimeUnit.SECONDS.sleep(5);
//			System.out.println("开始注册地址信息");
//			Properties cylJobProp = loadProperties("cyl-job-executor.properties");
//			String appName = cylJobProp.getProperty("cyl.job.executor.appname");
//			String ip = cylJobProp.getProperty("cyl.job.executor.ip");
//			int port = Integer.valueOf(cylJobProp.getProperty("cyl.job.executor.port"));
//			String address = IpUtil.getIpPort(ip, port);
//			System.out.println("register appName  address " +  appName + address);
//			ExecutorRegistryThread.getInstance().start(appName, address);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}

}
