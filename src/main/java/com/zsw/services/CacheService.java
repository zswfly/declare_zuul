package com.zsw.services;

import com.zsw.utils.UserStaticURLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by zhangshaowei on 2020/4/17.
 */
@Service
public class CacheService implements IBaseService{

    @Autowired
    RestTemplate restTemplate;

    public String getToken(String userId){
        ResponseEntity<String> result = restTemplate.postForEntity(
                "http://user-services"
                        + UserStaticURLUtil.permissionController
                        + UserStaticURLUtil.permissionController_initPermission
                ,userId,String.class);
        return result.getBody();
    }
}
