package com.zsw.inits;

import com.zsw.annotations.Permission;
import com.zsw.annotations.PermissionDescription;
import com.zsw.entitys.user.InitPermission;
import com.zsw.services.IBaseService;
import com.zsw.utils.UserStaticURLUtil;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by zhangshaowei on 2020/4/11.
 */
@Component
@Order(9)
public class PermissionInit  implements CommandLineRunner, ApplicationContextAware {

    private static ApplicationContext context;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public void run(String... args) throws Exception {
        //1扫描所有权限url
        List<InitPermission> initPermissionList = scan();
        //2发送请求修改
        ResponseEntity<Integer> result = restTemplate.postForEntity(
                "http://user-services"
                + UserStaticURLUtil.permissionController
                + UserStaticURLUtil.permissionController_initPermission
                ,initPermissionList,Integer.class);
        result.getBody();
    }

    private List<InitPermission>  scan(){
        ArrayList<InitPermission> list = new ArrayList<InitPermission>();

        // 获取所有beanNames
        String[] beanNames = context.getBeanNamesForType(IBaseService.class);
        for (String beanName : beanNames) {
            Method[] methods = context.getBean(beanName).getClass().getDeclaredMethods();

            for (Method method : methods) {
                //注意Action是注解类
                Permission permission = AnnotationUtils.findAnnotation(method, Permission.class);
                PermissionDescription permissionDescription = AnnotationUtils.findAnnotation(method, PermissionDescription.class);
                //判断该方法是否有permission注解
                if (permission!=null && permissionDescription!=null) {
                    String code = permission.value();
                    String name = permissionDescription.name();
                    String description = permissionDescription.description();
                    // do something
                    list.add(new InitPermission(code,name,description));
                }
            }
        }
        return list;
    }

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
