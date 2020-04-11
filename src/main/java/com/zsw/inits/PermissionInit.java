package com.zsw.inits;

import com.zsw.annotation.Permission;
import com.zsw.annotation.PermissionDescription;
import com.zsw.entity.common.InitPermission;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by zhangshaowei on 2020/4/11.
 */
@Component
@Order(1)
public class PermissionInit  implements CommandLineRunner, ApplicationContextAware {
    private static ApplicationContext context;
    @Override
    public void run(String... args) throws Exception {
        //1扫描所有权限url
        List<InitPermission> list = scan();
    }

    private List<InitPermission>  scan(){
        ArrayList<InitPermission> list = new ArrayList<InitPermission>();
        // 获取所有beanNames
        String[] beanNames = context.getBeanNamesForType(FeignClient.class);
        for (String beanName : beanNames) {
            FeignClient feignClient = context.findAnnotationOnBean(beanName, FeignClient.class);
            //判断该类是否含有FeignClient注解
            if (feignClient != null) {
                Method[] methods = context.getBean(beanName).getClass().getDeclaredMethods();
                for (Method method : methods) {
                    //判断该方法是否有permission注解
                    if (method.isAnnotationPresent(Permission.class) && method.isAnnotationPresent(PermissionDescription.class)) {
                        Permission permission = method.getAnnotation(Permission.class);
                        PermissionDescription permissionDescription = method.getAnnotation(PermissionDescription.class);
                        String code = permission.value();
                        String name = permissionDescription.name();
                        String description = permissionDescription.description();
                        // do something
                        list.add(new InitPermission(code,name,description));
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
