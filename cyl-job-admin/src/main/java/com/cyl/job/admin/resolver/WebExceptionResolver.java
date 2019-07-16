package com.cyl.job.admin.resolver;

import com.cyl.api.model.ResponseModel;
import com.cyl.api.util.JacksonUtil;
import com.cyl.job.admin.exception.CylJobException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
public class WebExceptionResolver implements HandlerExceptionResolver {
    private static final Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class);
    
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        if (!(ex instanceof CylJobException)) {
            logger.error("WebExceptionResolver: {}", ex);
        }
        
        //if json
        boolean isJson = false;
        HandlerMethod method = (HandlerMethod) handler;
        ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
        if (responseBody != null) {
            isJson = true;
        }
        
        //error result
        ResponseModel<String> errorResult = new ResponseModel<>(ResponseModel.FAIL_CODE,
                ex.toString().replaceAll("\n", "<br/>"));
        
        //response
        ModelAndView mv = new ModelAndView();
        if (isJson) {
            response.setContentType("application/json;charset=utf-8");
            try {
                response.getWriter().print(JacksonUtil.writeValueAsString(errorResult));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            return mv;
        } else {
            mv.addObject("exceptionMsg", errorResult.getMsg());
            mv.setViewName("/common/common.exception");
            return mv;
        } 
    }
}
