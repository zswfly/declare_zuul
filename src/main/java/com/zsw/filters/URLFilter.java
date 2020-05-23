package com.zsw.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zsw.utils.JwtUtil;
import com.zsw.utils.ZuulUtil;
import io.jsonwebtoken.Claims;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by zhangshaowei on 2020/3/26.
 */
//@Component
public class URLFilter extends ZuulFilter{

    private static final Logger LOG = LoggerFactory.getLogger(URLFilter.class);



    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtUtil jwtUtil;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        return ZuulUtil.isBussinessServicesPaths();
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();
        String token = request.getHeader("token");
        Claims claims;
        try {
            //解析没有异常则表示token验证通过，如有必要可根据自身需求增加验证逻辑
            claims = jwtUtil.parseJWT(token);
            //log.info("token : {} 验证通过", token);
            //对请求进行路由
            //请求头加入userId，传给业务服务
            Object tokenUserId = claims.get("userId");
            Object tokenHostUrl = claims.get("hostUrl");
            String stringUrl =
                            "http://"+
                            tokenHostUrl.toString() +
                            //"/message-services" +
                            request.getRequestURI();

            ctx.setSendZuulResponse(false);
            ctx.put(FilterConstants.REQUEST_URI_KEY, stringUrl);
            ctx.put(FilterConstants.FORWARD_TO_KEY, stringUrl);
            ctx.setResponseStatusCode(HttpStatus.SC_TEMPORARY_REDIRECT);
            request.getRequestDispatcher(stringUrl).forward(request, response);
//            response.sendRedirect(stringUrl);
//            URI uri1=new URI("http://localhost:5556");
//            ctx.setRouteHost(uri1.toURL());


            return null;
        }catch (Exception e){
            e.printStackTrace();
            LOG.error("error", e);
            ZuulUtil.reject("系统错误请联系工作人员",ctx);
        }


        return null;
    }

//else if(ZuulUtil.isBussinessServicesPaths()){
//        String stringUrl = "http://"
//                + tokenHostUrl.toString()
//                + request.getRequestURI();
//
//        ctx.setSendZuulResponse(false);
//        ctx.put(FilterConstants.FORWARD_TO_KEY, stringUrl);
//        //ctx.setResponseStatusCode(HttpStatus.SC_TEMPORARY_REDIRECT);
//        //request.getRequestDispatcher(stringUrl).forward(request, response);
//        //ctx.getResponse().sendRedirect(stringUrl);
//    }

}
