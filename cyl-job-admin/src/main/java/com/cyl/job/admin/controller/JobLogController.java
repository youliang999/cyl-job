package com.cyl.job.admin.controller;

import com.cyl.api.dao.CylJobGroupDao;
import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.dao.CylJobLogDao;
import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobLog;
import com.cyl.api.model.LogResult;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.util.DateUtil;
import com.cyl.api.util.I18nUtil;
import com.cyl.job.admin.exception.CylJobException;
import com.cyl.job.core.config.CylJobSchedule;
import com.cyl.job.core.service.ExecutorBiz;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/joblog")
public class JobLogController {
    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    private CylJobGroupDao cylJobGroupDao;
    @Resource
    public CylJobInfoDao cylJobInfoDao;
    @Resource
    public CylJobLogDao cylJobLogDao;


    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "0") Integer jobId) {

        // 执行器列表
        List<CylJobGroup> jobGroupList_all =  cylJobGroupDao.findAll();

        // filter group
        List<CylJobGroup> jobGroupList = JobInfoController.filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList==null || jobGroupList.size()==0) {
            throw new CylJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);

        // 任务
        if (jobId > 0) {
            CylJobInfo jobInfo = cylJobInfoDao.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }

            model.addAttribute("jobInfo", jobInfo);

            // valid permission
            JobInfoController.validPermission(request, jobInfo.getJobGroup());
        }

        return "joblog/joblog.index";
    }

    @RequestMapping("/getJobsByGroup")
    @ResponseBody
    public ResponseModel<List<CylJobInfo>> getJobsByGroup(int jobGroup){
        List<CylJobInfo> list = cylJobInfoDao.getJobsByGroup(jobGroup);
        return new ResponseModel<List<CylJobInfo>>(list);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            int jobGroup, int jobId, int logStatus, String filterTime) {

        // valid permission
        JobInfoController.validPermission(request, jobGroup);	// 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime!=null && filterTime.trim().length()>0) {
            String[] temp = filterTime.split(" - ");
            if (temp!=null && temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        // page query
        List<CylJobLog> list = cylJobLogDao.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int list_count = cylJobLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @RequestMapping("/logDetailPage")
    public String logDetailPage(int id, Model model){

        // base check
        ResponseModel<String> logStatue = ResponseModel.SUCCESS;
        CylJobLog jobLog = cylJobLogDao.load(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("executorAddress", jobLog.getExecutorAddress());
        model.addAttribute("triggerTime", jobLog.getTriggerTime().getTime());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }

    @RequestMapping("/logDetailCat")
    @ResponseBody
    public ResponseModel<LogResult> logDetailCat(String executorAddress, long triggerTime, int logId, int fromLineNum){
        try {
            ExecutorBiz executorBiz = CylJobSchedule.getExecutorBiz(executorAddress);
            ResponseModel<LogResult> logResult = executorBiz.log(triggerTime, logId, fromLineNum);

            // is end
            if (logResult.getContent()!=null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                CylJobLog jobLog = cylJobLogDao.load(logId);
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

            return logResult;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ResponseModel<LogResult>(ResponseModel.FAIL_CODE, e.getMessage());
        }
    }

    @RequestMapping("/logKill")
    @ResponseBody
    public ResponseModel<String> logKill(int id){
        // base check
        CylJobLog log = cylJobLogDao.load(id);
        CylJobInfo jobInfo = cylJobInfoDao.loadById(log.getJobId());
        if (jobInfo==null) {
            return new ResponseModel<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ResponseModel.SUCCESS_CODE != log.getTriggerCode()) {
            return new ResponseModel<String>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }

        // request of kill
        ResponseModel<String> runResult = null;
        try {
            ExecutorBiz executorBiz = CylJobSchedule.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(jobInfo.getId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ResponseModel<String>(500, e.getMessage());
        }

        if (ResponseModel.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ResponseModel.FAIL_CODE);
            log.setHandleMsg( I18nUtil.getString("joblog_kill_log_byman")+":" + (runResult.getMsg()!=null?runResult.getMsg():""));
            log.setHandleTime(new Date());
            cylJobLogDao.updateHandleInfo(log);
            return new ResponseModel<String>(runResult.getMsg());
        } else {
            return new ResponseModel<String>(500, runResult.getMsg());
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    public ResponseModel<String> clearLog(int jobGroup, int jobId, int type){

        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);	// 清理一个月之前日志数据
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);	// 清理三个月之前日志数据
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);	// 清理六个月之前日志数据
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);	// 清理一年之前日志数据
        } else if (type == 5) {
            clearBeforeNum = 1000;		// 清理一千条以前日志数据
        } else if (type == 6) {
            clearBeforeNum = 10000;		// 清理一万条以前日志数据
        } else if (type == 7) {
            clearBeforeNum = 30000;		// 清理三万条以前日志数据
        } else if (type == 8) {
            clearBeforeNum = 100000;	// 清理十万条以前日志数据
        } else if (type == 9) {
            clearBeforeNum = 0;			// 清理所有日志数据
        } else {
            return new ResponseModel<String>(ResponseModel.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        cylJobLogDao.clearLog(jobGroup, jobId, clearBeforeTime, clearBeforeNum);
        return ResponseModel.SUCCESS;
    }
}
