package com.zsw.services;

import com.zsw.annotation.Permission;
import com.zsw.annotation.PermissionDescription;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by zhangshaowei on 2020/3/26.
 */
@FeignClient(name = "cache-services" )
@Component
public interface TestServices {
    @RequestMapping(value = "/declare_test/getKey" , method = RequestMethod.GET)
    @Permission("cache.test")
    @PermissionDescription(name="cache测试权限",description = "测试用的")
    String getValue(@RequestParam("key") String key);
}
