package com.zsw.filters;

import com.google.gson.Gson;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zsw.entitys.common.ResponseJson;
import com.zsw.services.CacheService;
import com.zsw.utils.CommonStaticWord;
import com.zsw.utils.UserStaticURLUtil;
import com.zsw.utils.ZuulUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by zhangshaowei on 2020/3/26.
 */
@Component
public class URLFilter extends ZuulFilter{
    /**
     *
     */
    @Autowired
    CacheService cacheService;


    //非拦截地址
    private List<String> paths;
    public URLFilter() {
        super();
        paths = new ArrayList<>();

        paths.add("/user-services"
                        +UserStaticURLUtil.userController
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

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String uri=request.getRequestURI();
        PathMatcher matcher = new AntPathMatcher();
        Optional<String> optional =ZuulUtil.getPaths().stream().filter(t->matcher.match(t,uri)).findFirst();
        return !optional.isPresent();
        //return true; //是否过滤
        //return false;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        //系统专用url禁止访问
        String uri=request.getRequestURI();
        if (uri.indexOf(CommonStaticWord.System_Url) > -1){
            ZuulUtil.reject("系统专用URL",ctx);
            return null;
        }
//        String token = request.getParameter("token");
//        String userId = request.getParameter("userId");
//        String token = request.getHeader("token");
//        String userId = request.getHeader("userId");
//
//        if(token == null
//                || StringUtils.isEmpty(token)
//        ){
//            reject("没有token",ctx);
//        }else if(userId == null
//                || StringUtils.isEmpty(userId)){
//            reject("没有userId",ctx);
//        }else if(!token.equals(this.cacheService.getToken(userId))){
//            reject("token错误",ctx);
//        }

        return null;
    }



}
