package com.zsw.filters;

import com.netflix.client.ClientException;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zsw.utils.JwtUtil;
import com.zsw.utils.ZuulUtil;
import io.jsonwebtoken.Claims;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by zhangshaowei on 2020/5/7.
 */
public class CustomHostRoutingFilter extends SimpleHostRoutingFilter{

    private static final Logger LOG = LoggerFactory.getLogger(CustomHostRoutingFilter.class);

    @Autowired
    JwtUtil jwtUtil;

    private static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("/{2,}");
    private final Timer connectionManagerTimer = new Timer("SimpleHostRoutingFilter.connectionManagerTimer", true);
    private boolean sslHostnameValidationEnabled;
    private boolean forceOriginalQueryStringEncoding;

    @Autowired
    private ProxyRequestHelper helper;

    private ZuulProperties.Host hostProperties;
    private ApacheHttpClientConnectionManagerFactory connectionManagerFactory;
    private ApacheHttpClientFactory httpClientFactory;
    private HttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient=null;

    public CloseableHttpClient instanceCloseableHttpClient() {
        if(null == httpClient) {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            this.httpClient=client;
        }
        return httpClient;
    }
    private boolean customHttpClient = false;
    private boolean useServlet31 = true;

    public void onApplicationEvent(EnvironmentChangeEvent event) {
        this.onPropertyChange(event);
    }

    /** @deprecated */
    @Deprecated
    public void onPropertyChange(EnvironmentChangeEvent event) {
        if(!this.customHttpClient) {
            boolean createNewClient = false;
            Iterator var3 = event.getKeys().iterator();

            while(var3.hasNext()) {
                String key = (String)var3.next();
                if(key.startsWith("zuul.host.")) {
                    createNewClient = true;
                    break;
                }
            }

            if(createNewClient) {
                try {
                    this.httpClient.close();
                } catch (IOException var6) {
                    LOG.error("error closing client", var6);
                }

                try {
                    this.connectionManager.shutdown();
                } catch (RuntimeException var5) {
                    LOG.error("error shutting down connection manager", var5);
                }

                this.connectionManager = this.newConnectionManager();
                this.httpClient = this.newClient();
            }
        }

    }

    public CustomHostRoutingFilter(ProxyRequestHelper helper, ZuulProperties properties, ApacheHttpClientConnectionManagerFactory connectionManagerFactory, ApacheHttpClientFactory httpClientFactory) {
        super(helper, properties, connectionManagerFactory, httpClientFactory);
        instanceCloseableHttpClient();
        this.hostProperties = properties.getHost();
        this.sslHostnameValidationEnabled = properties.isSslHostnameValidationEnabled();
        this.forceOriginalQueryStringEncoding = properties.isForceOriginalQueryStringEncoding();
        this.connectionManagerFactory = connectionManagerFactory;
        this.httpClientFactory = httpClientFactory;
        this.checkServletVersion();
    }



    @PostConstruct
    private void initialize() {
        if(!this.customHttpClient) {
            this.connectionManager = this.newConnectionManager();
            this.httpClient = this.newClient();
            this.connectionManagerTimer.schedule(new TimerTask() {
                public void run() {
                    if(CustomHostRoutingFilter.this.connectionManager != null) {
                        CustomHostRoutingFilter.this.connectionManager.closeExpiredConnections();
                    }
                }
            }, 30000L, 5000L);
        }

    }

    @PreDestroy
    public void stop() {
        this.connectionManagerTimer.cancel();
    }

    public String filterType() {
        return "route";
    }

    public int filterOrder() {
        return 100;
    }

    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getRouteHost() != null && RequestContext.getCurrentContext().sendZuulResponse();
    }

    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
        MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
        String verb = this.getVerb(request);
        InputStream requestEntity = this.getRequestBody(request);
        if(this.getContentLength(request) < 0L) {
            context.setChunkedRequestBody();
        }

        //自定义____________________________________________________________________________________________________________________________start
        String uri = getUri(request);
        //自定义____________________________________________________________________________________________________________________________end
        this.helper.addIgnoredHeaders(new String[0]);

        try {
            CloseableHttpResponse response = this.forward(this.httpClient, verb, uri, request, headers, params, requestEntity);
            this.setResponse(response);
            return null;
        } catch (Exception var9) {
            throw new ZuulRuntimeException(this.handleException(var9));
        }
    }

    protected ZuulException handleException(Exception ex) {
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        Throwable cause = ex;
        String message = ex.getMessage();
        ClientException clientException = this.findClientException(ex);
        if(clientException != null) {
            if(clientException.getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
                statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
            }

            cause = clientException;
            message = clientException.getErrorType().toString();
        }

        return new ZuulException((Throwable)cause, "Forwarding error", statusCode, message);
    }

    protected ClientException findClientException(Throwable t) {
        return t == null?null:(t instanceof ClientException?(ClientException)t:this.findClientException(t.getCause()));
    }

    protected void checkServletVersion() {
        try {
            HttpServletRequest.class.getMethod("getContentLengthLong", new Class[0]);
            this.useServlet31 = true;
        } catch (NoSuchMethodException var2) {
            this.useServlet31 = false;
        }

    }

    protected void setUseServlet31(boolean useServlet31) {
        this.useServlet31 = useServlet31;
    }

    protected HttpClientConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    protected HttpClientConnectionManager newConnectionManager() {
        return this.connectionManagerFactory.newConnectionManager(!this.sslHostnameValidationEnabled, this.hostProperties.getMaxTotalConnections(), this.hostProperties.getMaxPerRouteConnections(), this.hostProperties.getTimeToLive(), this.hostProperties.getTimeUnit(), (RegistryBuilder)null);
    }

    protected CloseableHttpClient newClient() {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(this.hostProperties.getConnectionRequestTimeoutMillis()).setSocketTimeout(this.hostProperties.getSocketTimeoutMillis()).setConnectTimeout(this.hostProperties.getConnectTimeoutMillis()).setCookieSpec("ignoreCookies").build();
        return this.httpClientFactory.createBuilder().setDefaultRequestConfig(requestConfig).setConnectionManager(this.connectionManager).disableRedirectHandling().build();
    }

    private CloseableHttpResponse forward(CloseableHttpClient httpclient, String verb, String uri, HttpServletRequest request, MultiValueMap<String, String> headers, MultiValueMap<String, String> params, InputStream requestEntity) throws Exception {
        Map<String, Object> info = this.helper.debug(verb, uri, headers, params, requestEntity);
        URL host = RequestContext.getCurrentContext().getRouteHost();
        //HttpHost httpHost = this.getHttpHost(host);
        HttpHost httpHost = this.getHttpHost(host,request);
        uri = StringUtils.cleanPath(MULTIPLE_SLASH_PATTERN.matcher(host.getPath() + uri).replaceAll("/"));
        long contentLength = this.getContentLength(request);
        ContentType contentType = null;
        if(request.getContentType() != null) {
            contentType = ContentType.parse(request.getContentType());
        }

        InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength, contentType);
        HttpRequest httpRequest = this.buildHttpRequest(verb, uri, entity, headers, params, request);
        LOG.debug(httpHost.getHostName() + " " + httpHost.getPort() + " " + httpHost.getSchemeName());
        CloseableHttpResponse zuulResponse = this.forwardRequest(httpclient, httpHost, httpRequest);
        this.helper.appendDebug(info, zuulResponse.getStatusLine().getStatusCode(), this.revertHeaders(zuulResponse.getAllHeaders()));
        return zuulResponse;
    }

    protected HttpRequest buildHttpRequest(String verb, String uri, InputStreamEntity entity, MultiValueMap<String, String> headers, MultiValueMap<String, String> params, HttpServletRequest request) {
        String uriWithQueryString = uri + (this.forceOriginalQueryStringEncoding?this.getEncodedQueryString(request):this.helper.getQueryString(params));
        String var9 = verb.toUpperCase();
        byte var10 = -1;
        switch(var9.hashCode()) {
            case 79599:
                if(var9.equals("PUT")) {
                    var10 = 1;
                }
                break;
            case 2461856:
                if(var9.equals("POST")) {
                    var10 = 0;
                }
                break;
            case 75900968:
                if(var9.equals("PATCH")) {
                    var10 = 2;
                }
                break;
            case 2012838315:
                if(var9.equals("DELETE")) {
                    var10 = 3;
                }
        }

        Object httpRequest;
        switch(var10) {
            case 0:
                HttpPost httpPost = new HttpPost(uriWithQueryString);
                httpRequest = httpPost;
                httpPost.setEntity(entity);
                break;
            case 1:
                HttpPut httpPut = new HttpPut(uriWithQueryString);
                httpRequest = httpPut;
                httpPut.setEntity(entity);
                break;
            case 2:
                HttpPatch httpPatch = new HttpPatch(uriWithQueryString);
                httpRequest = httpPatch;
                httpPatch.setEntity(entity);
                break;
            case 3:
                BasicHttpEntityEnclosingRequest entityRequest = new BasicHttpEntityEnclosingRequest(verb, uriWithQueryString);
                httpRequest = entityRequest;
                entityRequest.setEntity(entity);
                break;
            default:
                httpRequest = new BasicHttpRequest(verb, uriWithQueryString);
                LOG.debug(uriWithQueryString);
        }

        ((HttpRequest)httpRequest).setHeaders(this.convertHeaders(headers));
        return (HttpRequest)httpRequest;
    }

    private String getEncodedQueryString(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null?"?" + query:"";
    }

    private MultiValueMap<String, String> revertHeaders(Header[] headers) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap();
        Header[] var3 = headers;
        int var4 = headers.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Header header = var3[var5];
            String name = header.getName();
            if(!map.containsKey(name)) {
                map.put(name, new ArrayList());
            }

            ((List)map.get(name)).add(header.getValue());
        }

        return map;
    }

    private Header[] convertHeaders(MultiValueMap<String, String> headers) {
        List<Header> list = new ArrayList();
        Iterator var3 = headers.keySet().iterator();

        while(var3.hasNext()) {
            String name = (String)var3.next();
            Iterator var5 = ((List)headers.get(name)).iterator();

            while(var5.hasNext()) {
                String value = (String)var5.next();
                list.add(new BasicHeader(name, value));
            }
        }

        return (Header[])list.toArray(new BasicHeader[0]);
    }

    private CloseableHttpResponse forwardRequest(CloseableHttpClient httpclient, HttpHost httpHost, HttpRequest httpRequest) throws IOException {
        return httpclient.execute(httpHost, httpRequest);
    }



    protected InputStream getRequestBody(HttpServletRequest request) {
        Object requestEntity = null;

        try {
            requestEntity = (InputStream)RequestContext.getCurrentContext().get("requestEntity");
            if(requestEntity == null) {
                requestEntity = request.getInputStream();
            }
        } catch (IOException var4) {
            LOG.error("error during getRequestBody", var4);
        }

        return (InputStream)requestEntity;
    }

    private String getVerb(HttpServletRequest request) {
        String sMethod = request.getMethod();
        return sMethod.toUpperCase();
    }

    private void setResponse(HttpResponse response) throws IOException {
        RequestContext.getCurrentContext().set("zuulResponse", response);
        this.helper.setResponse(response.getStatusLine().getStatusCode(), response.getEntity() == null?null:response.getEntity().getContent(), this.revertHeaders(response.getAllHeaders()));
    }

    protected void addIgnoredHeaders(String... names) {
        this.helper.addIgnoredHeaders(names);
    }

    boolean isSslHostnameValidationEnabled() {
        return this.sslHostnameValidationEnabled;
    }

    protected long getContentLength(HttpServletRequest request) {
        if(this.useServlet31) {
            return request.getContentLengthLong();
        } else {
            String contentLengthHeader = request.getHeader("Content-Length");
            if(contentLengthHeader != null) {
                try {
                    return Long.parseLong(contentLengthHeader);
                } catch (NumberFormatException var4) {
                    ;
                }
            }

            return (long)request.getContentLength();
        }
    }


    private String getUri(HttpServletRequest request){
        return ZuulUtil.isBussinessServicesPaths()?request.getRequestURI():this.helper.buildZuulRequestURI(request);
    }

    //自定义____________________________________________________________________________________________________________________________start

//    private HttpHost getHttpHost(URL host) {
//        HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
//        return httpHost;
//    }
    private HttpHost getHttpHost(URL host,HttpServletRequest request) {
        HttpHost httpHost = null;
        if(!ZuulUtil.isBussinessServicesPaths()){
            String token = request.getHeader("token");
            Claims claims = jwtUtil.parseJWT(token);
            String tokenHostUrl = claims.get("hostUrl").toString();
            String[] tokenHostUrlArray = tokenHostUrl.split(":");
            httpHost = new HttpHost(
                    tokenHostUrlArray[0]
                    , Integer.valueOf(tokenHostUrlArray[1])
                    , host.getProtocol());
        }else{
            httpHost = new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
        }

        return httpHost;
    }
//自定义____________________________________________________________________________________________________________________________end
}