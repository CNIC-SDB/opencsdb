package com.linkeddata.portal.co_analysis.controller;

import com.linkeddata.portal.co_analysis.service.impl.ResourceServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 协同分析接口
 *
 * @author : gaoshuai
 * @date : 2022/12/23 15:11
 */
@Api(tags = "协同分析公共接口")
@RestController
@RequestMapping("/piflow")
public class PublicInterface {

    @Resource
    private ResourceServiceImpl resourceService;

    /**
     * 检索列表
     *
     * @param keyword      查询关键词
     * @param dataCenterId 数据中心id
     * @param pageNum      当前页码
     * @param pageSize     每页条数
     * @return
     */
    @ApiOperation("检索列表")
    @PostMapping("/getResourceList")
    public String getResourceList(String keyword, String dataCenterId, Integer pageNum, Integer pageSize) {
        String json = resourceService.getResourceList(keyword, dataCenterId, pageNum, pageSize);
        return json;
    }

    /**
     * 根据id检索单个信息
     *
     * @param id
     * @return
     */
    @ApiOperation("根据id检索单个信息")
    @PostMapping("/getResourceById")
    public String getResourceById(String id) {
        String json = resourceService.getResourceById(id);
        return json;
    }

}
