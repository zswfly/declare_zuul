package com.zsw.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.zsw.entitys.common.Result;
import com.zsw.utils.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangshaowei on 2020/4/25.
 */
@Component
public class UserJwtAuthPreFilter extends ZuulFilter {


    private static final Logger LOG = LoggerFactory.getLogger(UserLoginAddJwtPostFilter.class);

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    RestTemplate restTemplate;
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
        return ZuulUtil.shouldFilter() && !ZuulUtil.isAdminPaths();
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
            Object rememberToken = claims.get("rememberToken");
            Object companyId = claims.get("companyId");

            if(tokenUserId == null || StringUtils.isEmpty(tokenUserId.toString())
                ||Integer.valueOf(NumberUtils.toInt(tokenUserId.toString(), 0)) < 1){
                //userId 有问题
                ZuulUtil.reject("token验证失败 !!!!!!",ctx);
                LOG.error("token验证失败:tokenAdminUserId  {};", tokenUserId);
            }else{
                ctx.addZuulRequestHeader("userId", tokenUserId.toString());
            }

            if(rememberToken == null || StringUtils.isEmpty(rememberToken.toString())){
                //rememberToken 有问题
                ZuulUtil.reject("token验证失败 !!!!!!",ctx);
                LOG.error("token验证失败:rememberToken  {};", rememberToken);
            }else{
                //验证码校验
                Map<String, String > param = new HashMap<>();
                param.put("rememberToken",rememberToken.toString());
                param.put("userId",tokenUserId.toString());
                ResponseEntity<Boolean> checkUserTokenResult  = this.restTemplate.postForEntity(
                        CommonStaticWord.HTTP + CommonStaticWord.userServices
                                + UserStaticURLUtil.userController
                                + UserStaticURLUtil.userController_checkRememberToken
                        ,param,Boolean.class);
                if(checkUserTokenResult == null
                        || checkUserTokenResult.getBody() == null
                        || !checkUserTokenResult.getBody()
                        ){
                    ZuulUtil.reject("token验证失败 !!!!!!",ctx);
                    LOG.error("token验证失败:checkUserTokenResult  {};", checkUserTokenResult);
                }else{
                    ctx.addZuulRequestHeader("rememberToken", rememberToken.toString());
                }

            }


            //选择公司时不用校验
            if(!ZuulUtil.isNotCheckCompanyHostPaths()) {

                if (tokenHostUrl == null || StringUtils.isEmpty(tokenHostUrl.toString())
                        ||companyId == null || StringUtils.isEmpty(companyId.toString())
                        ||Integer.valueOf(NumberUtils.toInt(companyId.toString(), 0)) < 1) {
                    //服务器地址 有问题
                    ZuulUtil.reject("token验证失败 !!!!!!", ctx);
                    LOG.error("token验证失败:checkUserTokenResult  {};", tokenHostUrl);
                }else{
                    ctx.addZuulRequestHeader("companyId", companyId.toString());
                }
            }




            return null;
        } catch (ExpiredJwtException expiredJwtEx) {
            ctx.setSendZuulResponse(false);
            ZuulUtil.reject("token过期",ctx);
            LOG.error("error", expiredJwtEx);
        } catch (Exception ex) {
            //不对请求进行路由
            ctx.setSendZuulResponse(false);
            ZuulUtil.reject("token验证失败",ctx);
            LOG.error("error", ex);
        }
        return null;
    }



}
