package com.zsw.filters;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.zsw.entitys.common.Result;
import com.zsw.utils.JwtUtil;
import com.zsw.utils.ResponseCode;
import com.zsw.utils.ZuulUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by zhangshaowei on 2020/4/25.
 */
@Component
public class SelectUserCompanyPostFilter extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SelectUserCompanyPostFilter.class);


    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtUtil jwtUtil;
//    @Autowired
//    DataFilterConfig dataFilterConfig;
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
        return FilterConstants.POST_TYPE;
    }
    /**
     * filterOrder：过滤的顺序
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 2;
    }
    /**
     * shouldFilter：这里可以写逻辑判断，是否要过滤
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return ZuulUtil.isSelectUserCompany();
    }
    /**
     * 执行过滤器逻辑，登录成功时给响应内容增加token
     *
     * @return
     */
    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        try {
            InputStream stream = ctx.getResponseDataStream();
            String body = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
            Result<HashMap<String, Object>> result = objectMapper.readValue(body, new TypeReference<Result<HashMap<String,Object>>>() {
            });
            //result.getCode() == ResponseCode.Code_1 表示选择成功
            if (ResponseCode.Code_200.equals(result.getCode()) ) {
                HashMap<String, Object> jwtClaims = new HashMap<String, Object>() {{
                    put("userId", result.getData().get("userId"));
                    put("rememberToken", result.getData().get("rememberToken"));
                    put("hostUrl", result.getData().get("hostUrl"));
                    put("companyId", result.getData().get("companyId"));
                }};
                Date expDate = DateTime.now().plusDays(7).toDate(); //过期时间 7 天
                String token = jwtUtil.createJWT(expDate, jwtClaims);
                //body json增加token
                result.getData().put("token", token);

                //result.getData().remove("userId");
                //result.getData().remove("companyId");
                result.getData().remove("hostUrl");
                result.getData().remove("rememberToken");

                //序列化body json,设置到响应body中
                body = objectMapper.writeValueAsString(result);
                ctx.setResponseBody(body);
                //响应头设置token
                ctx.addZuulResponseHeader("token", token);
            }else{
                ZuulUtil.reject(result.getMessage(),ctx);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("error", e);
            ZuulUtil.reject("系统错误请联系工作人员",ctx);
        }
        return null;
    }
}