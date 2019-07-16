package com.cyl.api.service.impl;

import com.cyl.api.dao.CylJobGroupDao;
import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.dao.CylJobLogDao;
import com.cyl.api.dao.CylJobLogGlueDao;
import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.service.CylJobService;
import com.cyl.api.cron.CronExpression;
import com.cyl.api.enums.ExecutorBlockStrategyEnum;
import com.cyl.api.enums.ExecutorRouteStrategyEnum;
import com.cyl.api.glue.GlueTypeEnum;
import com.cyl.api.util.DateUtil;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CylJobServiceImpl implements CylJobService {

    private static final Logger logger = LoggerFactory.getLogger(CylJobServiceImpl.class);

    @Resource
    private CylJobGroupDao cylJobGroupDao;
    @Resource
    private CylJobInfoDao cylJobInfoDao;
    @Resource
    private CylJobLogDao cylJobLogDao;
    @Resource
    private CylJobLogGlueDao cylJobLogGlueDao;
    
    @Override
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc,
            String executorHandler, String author) {
        //page list
        List<CylJobInfo> list = cylJobInfoDao
                .pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
        int listCount = cylJobInfoDao
                .pageListCount(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
        //page result
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", listCount);    //总记录数
        maps.put("recordsFiltered", listCount); //过滤后的总记录数
        maps.put("data", list);
        return maps;
    }

    @Override
    public ResponseModel<String> add(CylJobInfo jobInfo) {
        //valid
        CylJobGroup group = cylJobGroupDao.load(jobInfo.getJobGroup());
        if (group == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请选择执行器");
        }
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "Cron格式非法");
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入任务描述");
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入负责人");
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "路由策略非法");
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "阻塞处理策略非法");
        }
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "运行模式非法");
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && (jobInfo.getExecutorHandler() == null
                || jobInfo.getExecutorHandler().trim().length() == 0)) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入jobHandler");
        }
        
        //fix "\r" in sheel
//        if(GlueTypeEnum.GlueShell)
        
        //childJob valid
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobId : childJobIds) {
                if (childJobId != null && childJobId.trim().length() > 0 && isNumeric(childJobId)) {
                    CylJobInfo cylJobInfo = cylJobInfoDao.loadById(Integer.valueOf(childJobId));
                    if (cylJobInfo == null) {
                        return new ResponseModel<>(ResponseModel.FAIL_CODE, "子任务id: " + childJobId + "未找到");
                    }
                } else {
                    return new ResponseModel<>(ResponseModel.FAIL_CODE, "子任务id: " + childJobId + "非法");
                } 
            }
            String temp = ""; //join
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length() - 1);
            
            jobInfo.setChildJobId(temp);
        }
        
        //add in db
        cylJobInfoDao.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "新增失败");
        }

        return new ResponseModel<>(String.valueOf(jobInfo.getId()));
    }

    private boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public ResponseModel<String> update(CylJobInfo jobInfo) {
        //valid
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "Cron格式非法");
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入负责人");
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "请输入任务描述");
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "阻塞处理策略非法");
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "路由策略非法");
        }
        // ChildJobId valid
        if (jobInfo.getChildJobId()!=null && jobInfo.getChildJobId().trim().length()>0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem: childJobIds) {
                if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
                    CylJobInfo childJobInfo = cylJobInfoDao.loadById(Integer.valueOf(childJobIdItem));
                    if (childJobInfo==null) {
                        return new ResponseModel<>(ResponseModel.FAIL_CODE,
                                MessageFormat.format(("子任务id"+"({0})"+"未找到"), childJobIdItem));
                    }
                } else {
                    return new ResponseModel<String>(ResponseModel.FAIL_CODE,
                            MessageFormat.format(("子任务id"+"({0})"+"非法"), childJobIdItem));
                }
            }

            String temp = "";	// join ,
            for (String item:childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length()-1);

            jobInfo.setChildJobId(temp);
        }
        
        //group valid
        CylJobGroup jobGroup = cylJobGroupDao.load(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "执行器非法");
        }
        
        //stage job info
        CylJobInfo existJobInfo = cylJobInfoDao.loadById(jobInfo.getId());
        if (existJobInfo == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "任务id未找到");
        }
        
        //next trigger time(5s后生效，避开预读周期)
        long nextTrigerTime = existJobInfo.getTriggerNextTime();
        if (existJobInfo.getTriggerStatus() == 1 && !jobInfo.getJobCron().equals(existJobInfo.getJobCron())) {
            try {
                nextTrigerTime = new CronExpression(jobInfo.getJobCron())
                        .getNextInvalidTimeAfter(new Date(System.currentTimeMillis() + 5000))
                        .getTime();
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
                return new ResponseModel<>(ResponseModel.FAIL_CODE, "Cron格式非法，" + e.getMessage());
            }
        }
        existJobInfo.setJobGroup(jobInfo.getJobGroup());
        existJobInfo.setJobCron(jobInfo.getJobCron());
        existJobInfo.setJobDesc(jobInfo.getJobDesc());
        existJobInfo.setAuthor(jobInfo.getAuthor());
        existJobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        existJobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        existJobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        existJobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        existJobInfo.setExecutorParam(jobInfo.getExecutorParam());
        existJobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        existJobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        existJobInfo.setChildJobId(jobInfo.getChildJobId());
        existJobInfo.setTriggerNextTime(nextTrigerTime);
        cylJobInfoDao.update(existJobInfo);
        return ResponseModel.SUCCESS;
    }

    @Override
    public ResponseModel<String> remove(int id) {
        CylJobInfo cylJobInfo = cylJobInfoDao.loadById(id);
        if (cylJobInfo == null) {
            return ResponseModel.SUCCESS;
        }

        cylJobInfoDao.delete(id);
        cylJobLogDao.delete(id);
        cylJobLogGlueDao.deleteByJobId(id);
        return ResponseModel.SUCCESS;
    }

    @Override
    public ResponseModel<String> start(int id) {
        CylJobInfo cylJobInfo = cylJobInfoDao.loadById(id);
        
        // next trigger time (5s后生效，避开预读周期）
        long nextTriggerTime = 0;
        try {
            nextTriggerTime = new CronExpression(cylJobInfo.getJobCron())
                    .getNextValidTimeAfter(new Date(System.currentTimeMillis() + 5000)).getTime();
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "cron表达式非法, " + e.getMessage());
        }
        cylJobInfo.setTriggerStatus(1);
        cylJobInfo.setTriggerLastTime(0);
        cylJobInfo.setTriggerNextTime(nextTriggerTime);
        cylJobInfoDao.update(cylJobInfo);
        return ResponseModel.SUCCESS;
    }

    @Override
    public ResponseModel<String> stop(int id) {
        CylJobInfo cylJobInfo = cylJobInfoDao.loadById(id);
        cylJobInfo.setTriggerStatus(0);
        cylJobInfo.setTriggerNextTime(0);
        cylJobInfo.setTriggerLastTime(0);

        cylJobInfoDao.update(cylJobInfo);
        return ResponseModel.SUCCESS;
    }

    @Override
    public Map<String, Object> dashboardInfo() {
        System.out.println(11112222);
        System.out.println(cylJobInfoDao);
        int jobInfoCount = cylJobInfoDao.findAllCount();
        int jobLogCount = cylJobLogDao.triggerCountByHandleCode(-1);
        int jobLogSuccessCount = cylJobLogDao.triggerCountByHandleCode(ResponseModel.SUCCESS_CODE);
        System.out.println(jobInfoCount  + "-" + jobLogCount + "-" + jobLogSuccessCount);
        //execuor count
        Set<String> executerAddress = new HashSet<>();
        List<CylJobGroup> groupList = cylJobGroupDao.findAll();
        if (groupList != null && !groupList.isEmpty()) {
            for (CylJobGroup cylJobGroup : groupList) {
                if (cylJobGroup.getRegistryList() != null && !cylJobGroup.getRegistryList().isEmpty()) {
                    executerAddress.addAll(cylJobGroup.getRegistryList());
                }
            }
        }

        int executorCount = executerAddress.size();
        Map<String, Object> dashboardMap = new HashMap<>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        
        return dashboardMap;
    }

    private static final String TRIGGER_CHART_DATA_CACHE = "trigger_chart_data_cache";
    @Override
    public ResponseModel<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        /*// get cache
		String cacheKey = TRIGGER_CHART_DATA_CACHE + "_" + startDate.getTime() + "_" + endDate.getTime();
		Map<String, Object> chartInfo = (Map<String, Object>) LocalCacheUtil.get(cacheKey);
		if (chartInfo != null) {
			return new ReturnT<Map<String, Object>>(chartInfo);
		}*/
      
        //process
        List<String> triggerDayList = new ArrayList<>();
        List<Integer> triggerDayCountRunningList = new ArrayList<>();
        List<Integer> triggerDayCountSucList = new ArrayList<>();
        List<Integer> triggerDayCountFailList = new ArrayList<>();

        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<Map<String, Object>> triggerCountMapAll = cylJobLogDao.triggerCountByDay(startDate, endDate);
        if (triggerCountMapAll != null && triggerCountMapAll.size() > 0) {
            for (Map<String, Object> item : triggerCountMapAll) {
                String day = String.valueOf(item.get("triggerDay"));
                int triggerDayCount = Integer.valueOf(String.valueOf(item.get("triggerDayCount")));
                int triggerDayCountRunning = Integer.valueOf(String.valueOf(item.get("triggerDayCountRunning")));
                int triggerDayCountSuc = Integer.valueOf(String.valueOf(item.get("triggerDayCountSuc")));
                int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;

                triggerDayList.add(day);
                triggerDayCountRunningList.add(triggerDayCountRunning);
                triggerDayCountSucList.add(triggerDayCountSuc);
                triggerDayCountFailList.add(triggerDayCountFail);

                triggerCountRunningTotal += triggerDayCount;
                triggerCountSucTotal += triggerDayCountSuc;
                triggerCountFailTotal += triggerDayCountFail;
            }
        } else {
            for (int i = 4; i > -1; i++) {
                triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(new Date(), -i)));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);

        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);
        
        
		/*// set cache
		LocalCacheUtil.set(cacheKey, result, 60*1000);     // cache 60s*/

        return new ResponseModel<>(result);
    }
}
