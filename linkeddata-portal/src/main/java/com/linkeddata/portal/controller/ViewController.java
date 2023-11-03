package com.linkeddata.portal.controller;

import com.linkeddata.portal.repository.DataViewDao;
import com.linkeddata.portal.utils.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 统计每个页面访问量，前端控制访问每个页面都调用这个接口，实现访问每个页面都记录访问量
 *
 * @author : gaoshuai
 * @date : 2022/10/25 11:01
 */
@Api(tags = "浏览量相关接口")
@RestController
public class ViewController {
    @Resource
    private DataViewDao dataViewDao;

    @ApiOperation("记录访问请求")
    @GetMapping("/addViewCount")
    public void addViewCount(HttpServletRequest request, String url, String datacenterId) {
        String ipAddr = StringUtil.getIpAddr(request);
        dataViewDao.saveViewCount(ipAddr, url, datacenterId);
    }


}
