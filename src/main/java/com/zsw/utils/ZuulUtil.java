package com.zsw.utils;

import com.google.gson.Gson;
import com.netflix.zuul.context.RequestContext;
import com.zsw.entitys.common.ResponseJson;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by zhangshaowei on 2020/4/25.
 */
public class ZuulUtil {
    //非拦截地址
    private static List<String> paths = null;

    public static List<String> getPaths() {
        if (paths == null) {
            synchronized (ZuulUtil.class) {
                if (paths == null) {
                    paths = new ArrayList<>();

                    paths.add("/user-services"
                            + UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_login);

                    paths.add("/user-services"
                            +UserStaticURLUtil.userController
                            + UserStaticURLUtil.userController_resetPassWord);


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

    public static Boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String uri=request.getRequestURI();
        PathMatcher matcher = new AntPathMatcher();
        Optional<String> optional =getPaths().stream().filter(t->matcher.match(t,uri)).findFirst();
        return !optional.isPresent();
    }

    public static void reject(String message,RequestContext ctx){
        ResponseJson json = new ResponseJson();
        Gson gson = new Gson();
        json.setMessage(message);
        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(401);
        ctx.getResponse().setContentType("application/json;charset=UTF-8");
        ctx.setResponseBody(gson.toJson(json));
    }

}
