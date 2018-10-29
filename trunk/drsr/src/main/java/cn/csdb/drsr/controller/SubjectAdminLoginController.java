package cn.csdb.drsr.controller;

import cn.csdb.drsr.service.SubjectAdminLoginService;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/subjectAdmin")
public class SubjectAdminLoginController {
    @Resource
    private SubjectAdminLoginService subjectAdminLoginService;

    private static final Logger logger = LogManager.getLogger(SubjectAdminLoginController.class);

    @RequestMapping(value = "/validateLogin")
    @ResponseBody
    public String validateLogin(HttpServletRequest request, @RequestParam(name = "userName", required = true) String userName, @RequestParam(name = "password", required = true) String password) {
        logger.info("enterring validateLogin");
        logger.info("userName = " + userName + ", password = " + password);

        String loginNotice = "";
        int loginStatus = 0; // log success or not， 0 ：success, 1: failed, notice : username or password is wrong
        loginStatus = subjectAdminLoginService.validateLogin(userName, password);

        if (loginStatus == 0)
        {
            loginNotice = "登录失败：用户名或者密码错误";
            logger.info("loginStatus = " + loginStatus + ", loginNotice = " + loginNotice);
            return loginNotice;
        }
        else
        {
            loginNotice = "登录成功！";
            return "redirect:/drsr/";
        }
    }

    @RequestMapping(value = "/login")
    @ResponseBody
    public ModelAndView login(HttpServletRequest request)
    {
        ModelAndView mv = new ModelAndView("login");
        return  mv;
    }
}
