package cn.csdb.portal.controller;

import cn.csdb.portal.model.User;
import cn.csdb.portal.service.CheckUserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shiba on 2018/11/6.
 */
@Controller
public class CheckUserController {
    @Resource
    private CheckUserService checkUserService;

    @RequestMapping("/login")
        public String login(User user, HttpServletRequest request) {

            //获取当前用户
            Subject subject = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken(user.getLoginId(), user.getPassword());
            try {
                if(user.getUserName()==null&&user.getPassword()==null){
                    return "loginNew";
                }else {
                    //为当前用户进行认证，授权
                    subject.login(token);
                    User u = checkUserService.getByUserName(user.getLoginId());
                    boolean flag = false;
                    boolean status = u.getGroups().contains(",");
                    Set<String> roles = new HashSet<>();
                    if(status){
                        String[] group = u.getGroups().split(",");
                        for(String str : group) {
                            roles.add(str);
                            if (str.equals("系统管理员")) {
                                flag = true;
                                break;
                            } else if (str.equals("主题库管理员")) {
                                flag = true;
                            } else {

                            }
                        }
                    }else{
                        roles.add(u.getGroups());
                        if (u.getGroups().equals("系统管理员")) {
                            flag = true;
                        } else if (u.getGroups().equals("主题库管理员")) {
                            flag = true;
                        } else {

                        }
                    }
                    if(!flag){
                        request.setAttribute("errorMsg", "请为账号赋予角色！");
                        return "loginNew";
                    }
                    if (u.getSubjectCode() != null) {
                        cn.csdb.portal.model.Subject sub = checkUserService.getSubjectByCode(u.getSubjectCode());
                        request.getSession().setAttribute("DbName", sub.getDbName());
                        request.getSession().setAttribute("SubjectName", sub.getSubjectName());
                        request.getSession().setAttribute("SubjectCode", sub.getSubjectCode());
                        request.getSession().setAttribute("FtpFilePath", sub.getFtpFilePath());
                        request.getSession().setAttribute("userName", u.getUserName());
                        request.getSession().setAttribute("LoginId", u.getLoginId());
                        request.getSession().setAttribute("roles", roles);
                    }else{
                        request.getSession().setAttribute("userName", u.getUserName());
                        request.getSession().setAttribute("LoginId", u.getLoginId());
                    }
                    return "redirect:/loginSuccess";
                }

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("user", user);
                if(user.getUserName()==null&&user.getPassword()==null){
                    request.setAttribute("errorMsg", "");
                }else {
                    request.setAttribute("errorMsg", "用户名或密码错误！");
                }
                return "loginNew";
            }
        }

        @RequestMapping("/loginSuccess")
        public String loginSuccess() {
            return "index";
        }
}