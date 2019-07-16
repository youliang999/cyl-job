package com.cyl.job.admin.controller;

import com.cyl.api.dao.CylJobInfoDao;
import com.cyl.api.dao.CylJobLogGlueDao;
import com.cyl.api.glue.GlueTypeEnum;
import com.cyl.api.model.CylJobInfo;
import com.cyl.api.model.CylJobLogGlue;
import com.cyl.api.model.ResponseModel;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/jobcode")
public class JobCodeController {

    @Resource
    private CylJobInfoDao cylJobInfoDao;
    @Resource
    private CylJobLogGlueDao cylJobLogGlueDao;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, int jobId) {
        CylJobInfo cylJobInfo = cylJobInfoDao.loadById(jobId);
        List<CylJobLogGlue> cylJobLogGlueList = cylJobLogGlueDao.findByJobId(jobId);

        if (cylJobInfo == null) {
            throw new RuntimeException("任务id非法");
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(cylJobInfo.getGlueType())) {
            throw new RuntimeException("该任务非GLUE模式");
        }
        
        //valid permission
        JobInfoController.validPermission(request, cylJobInfo.getJobGroup());
        
        // Glue类型-字典
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());
        model.addAttribute("jobInfo", cylJobInfo);
        model.addAttribute("jobLogGlues", cylJobLogGlueList);
        return "jobcode/jobcode.index";
    }

    @RequestMapping("/save")
    @ResponseBody
    public ResponseModel<String> save(Model model, int id, String glueSource, String glueRemark) {
        //valid
        if (glueRemark == null) {
            return new ResponseModel<String>(ResponseModel.FAIL_CODE, "请输入源码备注");
        }
        if (glueRemark.length() < 4 || glueRemark.length() > 100) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "源码备注长度限制为4~100)");
        }
        CylJobInfo exist_jobInfo = cylJobInfoDao.loadById(id);
        if (exist_jobInfo == null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "任务id非法");
        }
        
        //update new code
        exist_jobInfo.setGlueSource(glueSource);
        exist_jobInfo.setGlueRemark(glueRemark);
        exist_jobInfo.setGlueUpdatetime(new Date());
        cylJobInfoDao.update(exist_jobInfo);
        
        //log old code
        CylJobLogGlue cylJobLogGlue = new CylJobLogGlue();
        cylJobLogGlue.setJobId(exist_jobInfo.getId());
        cylJobLogGlue.setGlueType(exist_jobInfo.getGlueType());
        cylJobLogGlue.setGlueSource(glueSource);
        cylJobLogGlue.setGlueRemark(glueRemark);
        cylJobLogGlueDao.save(cylJobLogGlue);
        
        //remove code backup more than 30
        cylJobLogGlueDao.removeOld(exist_jobInfo.getId(), 30);
        return ResponseModel.SUCCESS;
    }
    

}
