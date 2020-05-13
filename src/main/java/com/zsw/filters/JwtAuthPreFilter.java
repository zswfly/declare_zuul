package com.zsw.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.zsw.entitys.common.Result;
import com.zsw.utils.JwtUtil;
import com.zsw.utils.ZuulUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangshaowei on 2020/4/25.
 */
@Component
public class JwtAuthPreFilter extends ZuulFilter {
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtUtil jwtUtil;
    /**
     * pre：路由之前
     * routing：路由之时
     * post： 路由之后
     * error：发送错误调用
     *
     * @return
     */
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }
    /**
     * filterOrder：过滤的顺序 序号配置可参照 https://blog.csdn.net/u010963948/article/details/100146656
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return 1;
    }
    /**
     * shouldFilter：逻辑是否要过滤
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return ZuulUtil.shouldFilter();
    }
    /**
     * 执行过滤器逻辑，验证token
     *
     * @return
     */
    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        //HttpServletResponse response = ctx.getResponse();
        String token = request.getHeader("token");
        Claims claims;
        try {
            //解析没有异常则表示token验证通过，如有必要可根据自身需求增加验证逻辑
            claims = jwtUtil.parseJWT(token);
            //log.info("token : {} 验证通过", token);
            //对请求进行路由
            ctx.setSendZuulResponse(true);
            //请求头加入userId，传给业务服务
            Object tokenUserId = claims.get("userId");
            Object tokenHostUrl = claims.get("hostUrl");

            if(tokenUserId == null || StringUtils.isEmpty(tokenUserId.toString())){
                //userId 有问题
                ZuulUtil.reject("token验证失败 !!!!!!",ctx);
            }else{
                ctx.addZuulRequestHeader("userId", tokenUserId.toString());
            }

            //选择公司时不用校验
            if(!ZuulUtil.isNotCheckCompanyHostPaths()) {
                if (tokenHostUrl == null || StringUtils.isEmpty(tokenHostUrl.toString())) {
                    //服务器地址 有问题
                    ZuulUtil.reject("token验证失败 !!!!!!", ctx);
                }
            }




            return null;
        } catch (ExpiredJwtException expiredJwtEx) {
            //log.error("token : {} 过期", token );
            //不对请求进行路由
            ctx.setSendZuulResponse(false);
            ZuulUtil.reject("token过期",ctx);
        } catch (Exception ex) {
            //不对请求进行路由
            ctx.setSendZuulResponse(false);
            ZuulUtil.reject("token验证失败",ctx);
        }
        return null;
    }
    /**
     * 将异常信息响应给前端
     */
    private void responseError(RequestContext ctx, Integer code, String message) {
        HttpServletResponse response = ctx.getResponse();
        Result errResult = new Result();
        errResult.setCode(code);
        errResult.setMessage(message);
        ctx.setResponseBody(toJsonString(errResult));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=utf-8");
    }
    private String toJsonString(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            //log.error("json序列化失败", e);
            return null;
        }
    }

}
