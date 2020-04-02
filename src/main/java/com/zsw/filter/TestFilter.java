package com.zsw.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zsw.services.TestServices;
import com.zsw.services.TestServices2;
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
public class TestFilter extends ZuulFilter{
    /**
     *
     */
    @Autowired
    private TestServices testServices;

    @Autowired
    private TestServices2 testServices2;

    //非拦截地址
    private List<String> paths;
    public TestFilter() {
        super();
        paths = new ArrayList<>();
        paths.add("/**/isUser");
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
        Optional<String> optional =paths.stream().filter(t->matcher.match(t,uri)).findFirst();
        return !optional.isPresent();
        //return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        Object accessToken = request.getParameter("accessToken");

        if(accessToken == null
                || StringUtils.isEmpty(accessToken.toString())
                || testServices.getValue(accessToken.toString()) == null
        ){
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            return null;
        }

        return null;
    }
}
