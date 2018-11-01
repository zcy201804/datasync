package cn.csdb.drsr.controller;

import cn.csdb.drsr.service.LoginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {
    @Resource
    private LoginService loginService;

    private static final Logger logger = LogManager.getLogger(LoginController.class);

    @RequestMapping(value = "/validateLogin")
    @ResponseBody
    public int validateLogin(HttpServletRequest request, @RequestParam(required = true) String userName, @RequestParam(required = true) String password) {
        logger.info("enterring validateLogin");
        logger.info("userName = " + userName + ", password = " + password);

        String loginNotice = "";
        int loginStatus = 0; // log success or not， 0 ：success, 1: failed, notice : username or password is wrong
        loginStatus = loginService.validateLogin(userName, password);

       return loginStatus;
    }
}
