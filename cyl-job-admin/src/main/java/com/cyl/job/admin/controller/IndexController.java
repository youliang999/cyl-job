package com.cyl.job.admin.controller;

import com.cyl.api.model.ResponseModel;
import com.cyl.api.service.CylJobService;
import com.cyl.api.service.LoginService;
import com.cyl.job.admin.annotation.PermissionLimit;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
    @Resource
    private CylJobService cylJobService;
    @Resource
    private LoginService loginService;


    @RequestMapping("/")
    public String index(Model model) {
        Map<String, Object> dashboardMap = cylJobService.dashboardInfo();
        model.addAllAttributes(dashboardMap);
        return "index";
    }

    @RequestMapping("/chartInfo")
    @ResponseBody
    public ResponseModel<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        ResponseModel<Map<String, Object>> chartInfo = cylJobService.chartInfo(startDate, endDate);
        return chartInfo;
    }

    @RequestMapping("/toLogin")
    @PermissionLimit(limit=false)
    public String toLogin(HttpServletRequest request, HttpServletResponse response) {
        if (loginService.ifLogin(request, response) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @RequestMapping(value="login", method= RequestMethod.POST)
    @ResponseBody
    @PermissionLimit(limit=false)
    public ResponseModel<String> loginDo(HttpServletRequest request, HttpServletResponse response, String userName, String password, String ifRemember){
        boolean ifRem = (ifRemember!=null && ifRemember.trim().length()>0 && "on".equals(ifRemember))?true:false;
        return loginService.login(request, response, userName, password, ifRem);
    }

    @RequestMapping(value = "logout", method = RequestMethod.POST)
    @ResponseBody
    @PermissionLimit(limit = false)
    public ResponseModel<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return loginService.logout(request, response);
    }

    @RequestMapping("help")
    public String help() {
        return "help";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
