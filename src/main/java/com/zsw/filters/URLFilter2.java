package com.zsw.filters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.zsw.utils.ZuulUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by zhangshaowei on 2020/6/21.
 */
//TODO 临时添加
//@Component
public class URLFilter2 extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(URLFilter2.class);


    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 101;
    }

    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getRouteHost() != null && RequestContext.getCurrentContext().sendZuulResponse();
    }

    @Override
    public Object run() {
        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            HttpServletRequest request = requestContext.getRequest();
            HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request){
                @Override
                public String getRequestURI() {
                    return ZuulUtil.replaceUrl(request);
                }
            };

            requestContext.setRequest(requestWrapper);

            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


}
