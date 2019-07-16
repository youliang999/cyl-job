package com.cyl.api.service;

import com.cyl.api.dao.CylJobUserDao;
import com.cyl.api.model.CylJobUser;
import com.cyl.api.model.ResponseModel;
import com.cyl.api.util.CookieUtil;
import com.cyl.api.util.JacksonUtil;
import java.math.BigInteger;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class LoginService {
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    
    public static final String LOGIN_IDENTITY_KEY = "CYL_JOB_LOGIN_IDENTITY";
    
    @Resource
    private CylJobUserDao cylJobUserDao;
    
    private String makeToken(CylJobUser cylJobUser) {
        String tokenJson = JacksonUtil.writeValueAsString(cylJobUser);
        String tokenHex = new BigInteger(tokenJson.getBytes()).toString(16);
        return tokenHex;
    }

    private CylJobUser parseToken(String tokenHex) {
        CylJobUser cylJobUser = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());
            cylJobUser = JacksonUtil.readValue(tokenJson, CylJobUser.class);
        }
        return cylJobUser;
    }

    //login
    public ResponseModel<String> login(HttpServletRequest request, HttpServletResponse response, String username,
            String password, boolean ifRemember) {
        //valid param
        System.out.println("211221");
        if (username == null || username.trim().length() == 0 || password == null || password.trim().length() == 0) {
            return new ResponseModel<>(500, "账号或密码为空");
        }
        
        //valid password
        CylJobUser cylJobUser = cylJobUserDao.loadByUserName(username);
        if (cylJobUser == null) {
            return new ResponseModel<>(500, "账号或密码错误");
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(cylJobUser.getPassword())) {
            return new ResponseModel<>(500, "账号或者密码错误");
        }

        String loginToken = makeToken(cylJobUser);

        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
        return ResponseModel.SUCCESS;
    }

    //login out
    public ResponseModel<String> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ResponseModel.SUCCESS;
    }

    //login out
    public CylJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            CylJobUser cookieUser = null;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                CylJobUser dbUser = cylJobUserDao.loadByUserName(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }
    
}
