package com.zsw.services;

import com.zsw.annotations.Permission;
import com.zsw.annotations.PermissionDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by zhangshaowei on 2020/4/15.
 */
@Service
public class TestService3 implements IBaseService{
    @Autowired
    RestTemplate restTemplate;

    @Permission("cache.test3")
    @PermissionDescription(name="cache3测试权限",description = "测试用的3")
    public void test(){
    }
}
