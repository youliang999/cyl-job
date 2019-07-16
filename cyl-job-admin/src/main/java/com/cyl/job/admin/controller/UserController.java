package com.cyl.job.admin.controller;

import com.cyl.api.dao.CylJobGroupDao;
import com.cyl.api.dao.CylJobUserDao;
import com.cyl.api.model.CylJobGroup;
import com.cyl.api.model.CylJobUser;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.service.LoginService;
import com.cyl.api.util.I18nUtil;
import com.cyl.job.admin.annotation.PermissionLimit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private CylJobUserDao cylJobUserDao;
    @Resource
    private CylJobGroupDao cylJobGroupDao;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {
        //执行器列表
        List<CylJobGroup> groupList = cylJobGroupDao.findAll();
        model.addAttribute("groupList", groupList);
        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            String username, int role) {
        //page list
        List<CylJobUser> list = cylJobUserDao.pageList(start, length, username, role);
        int list_count = cylJobUserDao.pageListCount(start, length, username, role);
        
        //package result 
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ResponseModel<String> add(CylJobUser cylJobUser) {

        // valid username
        if (!StringUtils.hasText(cylJobUser.getUsername())) {
            return new ResponseModel<String>(ResponseModel.FAIL_CODE,
                    "请输入用户名");
        }
        cylJobUser.setUsername(cylJobUser.getUsername().trim());
        if (!(cylJobUser.getUsername().length() >= 4 && cylJobUser.getUsername().length() <= 20)) {
            return new ResponseModel<String>(ResponseModel.FAIL_CODE, "长度限制" + "[4-20]");
        }
        // valid password
        if (!StringUtils.hasText(cylJobUser.getPassword())) {
            return new ResponseModel<String>(ResponseModel.FAIL_CODE,
                    "请输入密码");
        }
        
        //md5 password
        cylJobUser.setPassword(DigestUtils.md5DigestAsHex(cylJobUser.getPassword().getBytes()));
        
        //check repeat
        CylJobUser existUser = cylJobUserDao.loadByUserName(cylJobUser.getUsername());
        if (existUser != null) {
            return new ResponseModel<>(ResponseModel.FAIL_CODE, "账号重复");
        }
        
        //write
        cylJobUserDao.save(cylJobUser);
        return ResponseModel.SUCCESS;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ResponseModel<String> update(HttpServletRequest request, CylJobUser xxlJobUser) {

        // avoid opt login seft
        CylJobUser loginUser = (CylJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getUsername().equals(xxlJobUser.getUsername())) {
            return new ResponseModel<String>(ResponseModel.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        // valid password
        if (StringUtils.hasText(xxlJobUser.getPassword())) {
            xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
            if (!(xxlJobUser.getPassword().length()>=4 && xxlJobUser.getPassword().length()<=20)) {
                return new ResponseModel<String>(ResponseModel.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
            }
            // md5 password
            xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));
        } else {
            xxlJobUser.setPassword(null);
        }

        // write
        cylJobUserDao.update(xxlJobUser);
        return ResponseModel.SUCCESS;
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ResponseModel<String> remove(HttpServletRequest request, int id) {

        // avoid opt login seft
        CylJobUser loginUser = (CylJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getId() == id) {
            return new ResponseModel<String>(ResponseModel.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        cylJobUserDao.delete(id);
        return ResponseModel.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ResponseModel<String> updatePwd(HttpServletRequest request, String password){

        // valid password
        if (password==null || password.trim().length()==0){
            return new ResponseModel<String>(ResponseModel.FAIL.getCode(), "密码不可为空");
        }
        password = password.trim();
        if (!(password.length()>=4 && password.length()<=20)) {
            return new ResponseModel<String>(ResponseModel.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        // md5 password
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // update pwd
        CylJobUser loginUser = (CylJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);

        // do write
        CylJobUser existUser = cylJobUserDao.loadByUserName(loginUser.getUsername());
        existUser.setPassword(md5Password);
        cylJobUserDao.update(existUser);

        return ResponseModel.SUCCESS;
    }

}
