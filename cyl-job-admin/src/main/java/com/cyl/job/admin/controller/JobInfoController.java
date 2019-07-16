package com.cyl.job.admin.controller;


import com.cyl.api.dao.CylJobGroupDao;
import com.cyl.api.enums.ExecutorBlockStrategyEnum;
import com.cyl.api.enums.ExecutorRouteStrategyEnum;
import com.cyl.api.enums.TriggerTypeEnum;
import com.cyl.api.glue.GlueTypeEnum;
import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobUser;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.service.CylJobService;
import com.cyl.api.service.LoginService;
import com.cyl.job.core.helper.JobTriggerPoolHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

    @Resource
    private CylJobGroupDao cylJobGroupDao;
    @Resource
    private CylJobService cylJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model,
            @RequestParam(required = false, defaultValue = "-1") int jobGroup) {
        
        //枚举-字典
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());
        
        //执行器列表
        List<CylJobGroup> jobGroupList_all = cylJobGroupDao.findAll();
        
        //filter group
        List<CylJobGroup> jobGroupList = filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList == null || jobGroupList.size() == 0) {
            throw new RuntimeException("不存在有效执行器，请联系管理员");
        }
        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);
        return "jobinfo/jobinfo.index";
    }

    public static List<CylJobGroup> filterJobGroupByRole(HttpServletRequest request,
            List<CylJobGroup> jobGroupList_all) {
        List<CylJobGroup> cylJobGroupList = new ArrayList<>();
        if (jobGroupList_all != null && jobGroupList_all.size() > 0) {
            CylJobUser loginUser = (CylJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
            if (loginUser.getRole() == 1) {
                cylJobGroupList = jobGroupList_all;
            } else {
                List<String> groupIdStrs = new ArrayList<>();
                if (loginUser.getPermission() != null && loginUser.getPermission().trim().length() > 0) {
                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
                }
                for (CylJobGroup groupItem : jobGroupList_all) {
                    if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
                        cylJobGroupList.add(groupItem);
                    }
                }
            } 
        }
        return cylJobGroupList;
    }

    public static void validPermission(HttpServletRequest request, int jobGroup) {
        CylJobUser loginUser = (CylJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (!loginUser.validPermission(jobGroup)) {
            throw new RuntimeException("权限拦截" + "[username=" + loginUser.getUsername() + "]");
        }
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {
        return cylJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }

    @RequestMapping("/add")
    @ResponseBody
    public ResponseModel<String> add(CylJobInfo cylJobInfo) {
        return cylJobService.add(cylJobInfo);
    }

    @RequestMapping("/update")
    @ResponseBody
    public ResponseModel<String> update(CylJobInfo cylJobInfo) {
        return cylJobService.update(cylJobInfo);
    }

    @RequestMapping("remove")
    @ResponseBody
    public ResponseModel<String> remove(int id) {
        return cylJobService.remove(id);
    }

    @RequestMapping("/stop")
    @ResponseBody
    public ResponseModel<String> stop(int id) {
        return cylJobService.stop(id);
    }

    @RequestMapping("/start")
    @ResponseBody
    public ResponseModel<String> start(int id) {
        return cylJobService.start(id);
    }

    @RequestMapping("/trigger")
    @ResponseBody
    public ResponseModel<String> triggerJob(int id, String executorParam) {
        //force cover job param
        if (executorParam == null) {
            executorParam = "";
        }

        JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, executorParam);
        return ResponseModel.SUCCESS;
    }
    
}
