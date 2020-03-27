package com.zsw.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by zhangshaowei on 2020/3/26.
 */
@FeignClient(name = "redis-services" )
@Component
public interface TestServices {
    @RequestMapping(value = "/declare_test/getKey" , method = RequestMethod.GET)
    String getValue(@RequestParam("key") String key);
}
