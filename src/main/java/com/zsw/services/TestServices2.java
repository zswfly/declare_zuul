package com.zsw.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by zhangshaowei on 2020/4/2.
 */
@FeignClient(name = "user-services" )
@Component
public interface TestServices2 {
    @RequestMapping(value = "/declare_test/isUser" , method = RequestMethod.GET)
    String isUser(@RequestParam("userName") String userName,@RequestParam("passWord") String passWord);
}
