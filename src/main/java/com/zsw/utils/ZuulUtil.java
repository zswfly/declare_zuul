package com.zsw.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.netflix.zuul.context.RequestContext;
import com.zsw.entitys.common.ResponseJson;
import com.zsw.entitys.common.Result;
import org.slf4j.Logger;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by zhangshaowei on 2020/4/25.
 */
public class ZuulUtil {

    public static void main(String[] args) {
        List<String> test = new ArrayList<>();
        test.add(
                 "**"
                + UserStaticURLUtil.admin
                + "/**"
                );
//        String uri = "/"
//                + CommonStaticWord.userServices
////                + UserStaticURLUtil.adminUserController
////                + "/adminUserssss"
//                + UserStaticURLUtil.adminUserController_loginOut
//                ;
        String uri = "http://localhost:9529/api/dev/user-services/admin/adminUser/getAdminUser/1";

        System.out.println();
        System.out.println(uri);
        System.out.println(test);
        System.out.println(matchUrlTest(uri,test));
    }


    //非拦截地址
    private static List<String> paths = null;
    private static List<String> adminPaths = null;
    private static List<String> userLoginPaths = null;
    private static List<String> adminUserLoginPaths = null;
    private static List<String> selectUserCompanyPaths = null;
    private static List<String> bussinessServicesPaths = null;
    private static List<String> notCheckCompanyHostPaths = null;




    public static List<String> getPaths() {
        if (paths == null) {
            synchronized (ZuulUtil.class) {
                if (paths == null) {
                    paths = new ArrayList<>();

                    paths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_login);

                    //重设密码没token
                    paths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_resetPassWord);

                    paths.add(
                            "/"
                                    + CommonStaticWord.userServices
                                    + UserStaticURLUtil.adminUserController
                                    + UserStaticURLUtil.adminUserController_login);

                    //重设密码没token
                    paths.add(
                            "/"
                                    + CommonStaticWord.userServices
                                    + UserStaticURLUtil.adminUserController
                                    + UserStaticURLUtil.adminUserController_resetPassWord);


                    //发送短信不设没token
                    paths.add(
                            "/"
                                    + CommonStaticWord.messageServices
                                    + MessageStaticURLUtil.hwyMessageController
                                    + MessageStaticURLUtil.hwyMessageController_sendVerifyCode);

                    paths.add("/**/*.css");
                    paths.add("/**/*.jpg");
                    paths.add("/**/*.png");
                    paths.add("/**/*.gif");
                    paths.add("/**/*.js");
                    paths.add("/**/*.svg");
                }
            }
        }
        return paths;
    }

    public static List<String> getAdminPaths() {
        if (adminPaths == null) {
            synchronized (ZuulUtil.class) {
                if (adminPaths == null) {
                    adminPaths = new ArrayList<>();
                    adminPaths.add("**"
                            + UserStaticURLUtil.admin
                            + "/**"
                    );
                }
            }
        }
        return adminPaths;
    }


    public static List<String> getUserLoginPaths() {
        if (userLoginPaths == null) {
            synchronized (ZuulUtil.class) {
                if (userLoginPaths == null) {
                    userLoginPaths = new ArrayList<>();
                    userLoginPaths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_login
                    );

                }
            }
        }
        return userLoginPaths;
    }

    public static List<String> getAdminUserLoginPaths() {
        if (adminUserLoginPaths == null) {
            synchronized (ZuulUtil.class) {
                if (adminUserLoginPaths == null) {
                    adminUserLoginPaths = new ArrayList<>();
                    adminUserLoginPaths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.adminUserController
                            + UserStaticURLUtil.adminUserController_login
                    );

                }
            }
        }
        return adminUserLoginPaths;
    }



    public static List<String> getSelectUserCompanyPaths() {
        if (selectUserCompanyPaths == null) {
            synchronized (ZuulUtil.class) {
                if (selectUserCompanyPaths == null) {
                    selectUserCompanyPaths = new ArrayList<>();
                    selectUserCompanyPaths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_selectUserCompany
                    );

                }
            }
        }
        return selectUserCompanyPaths;
    }

    public static List<String> getBussinessServicesPaths() {
        if (bussinessServicesPaths == null) {
            synchronized (ZuulUtil.class) {
                if (bussinessServicesPaths == null) {
                    bussinessServicesPaths = new ArrayList<>();
                    bussinessServicesPaths.add(
                            "/**/"
                            + CommonStaticWord.messageServices
                            + "/**"
                    );

                    bussinessServicesPaths.add(
                            "/**/"
                                    + CommonStaticWord.userServices
                                    + "/**"
                    );

                }
            }
        }
        return bussinessServicesPaths;
    }


    public static List<String> getNotCheckCompanyHostPaths() {
        if (notCheckCompanyHostPaths == null) {
            synchronized (ZuulUtil.class) {
                if (notCheckCompanyHostPaths == null) {
                    notCheckCompanyHostPaths = new ArrayList<>();
                    notCheckCompanyHostPaths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_selectUserCompany
                    );
                    notCheckCompanyHostPaths.add(
                            "/"
                            + CommonStaticWord.userServices
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_getUserCompanys
                    );
                }
            }
        }
        return notCheckCompanyHostPaths;
    }

    public static Boolean shouldFilter() {
        return !matchUrl(getPaths());
    }

    public static Boolean isAdminPaths() {
        return matchUrl(getAdminPaths());
    }

    public static Boolean isUserLogin() {
        return matchUrl(getUserLoginPaths());
    }

    public static Boolean isAdminUserLogin() {
        return matchUrl(getAdminUserLoginPaths());
    }

    public static Boolean isSelectUserCompany() {
        return matchUrl(getSelectUserCompanyPaths());
    }

    public static Boolean isBussinessServicesPaths() {
        return matchUrl(getBussinessServicesPaths());
    }

    public static Boolean isNotCheckCompanyHostPaths() {
        return matchUrl(getNotCheckCompanyHostPaths());
    }


    public  static Boolean matchUrl(List<String> urls){
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String uri=request.getRequestURI();
        PathMatcher matcher = new AntPathMatcher();
        Optional<String> optional =urls.stream().filter(t->matcher.match(t,uri)).findFirst();
        return optional.isPresent();
    }


    public  static Boolean matchUrlTest(String uri,List<String> urls){
        PathMatcher matcher = new AntPathMatcher();
        Optional<String> optional =urls.stream().filter(t->matcher.match(t,uri)).findFirst();
        return optional.isPresent();
    }

    public static void reject(String message,RequestContext ctx){
        ResponseJson json = new ResponseJson();
        Gson gson = new Gson();
        json.setMessage(message);
        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(ResponseCode.Code_Bussiness_Error);
        ctx.getResponse().setContentType("application/json;charset=UTF-8");
        ctx.setResponseBody(gson.toJson(json));
    }










    /**
     * 将异常信息响应给前端
     */
    private void  responseError(RequestContext ctx, Integer code, String message, ObjectMapper objectMapper,Logger LOG ) {
        HttpServletResponse response = ctx.getResponse();
        Result errResult = new Result();
        errResult.setCode(code);
        errResult.setMessage(message);
        ctx.setResponseBody(toJsonString(errResult,objectMapper,LOG));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=utf-8");
    }
    private String toJsonString(Object o, ObjectMapper objectMapper,Logger LOG ) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            //log.error("json序列化失败", e);
            LOG.error("error", e);
            return null;
        }
    }

}
