package com.cyl.job.admin.controller;

import com.cyl.job.admin.annotation.PermissionLimit;
import com.cyl.job.core.service.AdminBiz;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Deprecated
public class JobApiController implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        
    }

    @RequestMapping(AdminBiz.MAPPING)
    @PermissionLimit(limit = false)
    public void api(HttpServletRequest request, HttpServletResponse response) {
//        CylJobSchedule.
    }
}
