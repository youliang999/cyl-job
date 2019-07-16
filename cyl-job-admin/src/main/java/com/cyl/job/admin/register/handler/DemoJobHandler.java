package com.cyl.job.admin.register.handler;

import com.cyl.api.model.ResponseModel;
import com.cyl.job.core.handler.IJobHandler;
import com.cyl.job.core.log.CylJobLogger;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务Handler示例（Bean模式）
 *
 * 开发步骤：
 * 1、继承"IJobHandler"：“com.xxl.job.core.handler.IJobHandler”；
 * 2、注册到执行器工厂：在 "JFinalCoreConfig.initXxlJobExecutor" 中手动注册，注解key值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobLogger.log" 打印执行日志；
 *
 * @author xuxueli 2015-12-19 19:43:36
 */
public class DemoJobHandler extends IJobHandler {

	private static final Logger logger = LoggerFactory.getLogger(DemoJobHandler.class);

	@Override
	public ResponseModel<String> execute(String param) throws Exception {
		CylJobLogger.log("CYL-JOB, Hello World.");
		logger.info("CYL-JOB, Hello World.");

		for (int i = 0; i < 5; i++) {
			CylJobLogger.log("beat at:" + i);
			logger.info("beat at:" + i);
			TimeUnit.SECONDS.sleep(2);
		}
		return SUCCESS;
	}

}
