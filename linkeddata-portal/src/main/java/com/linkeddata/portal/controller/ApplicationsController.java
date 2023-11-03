package com.linkeddata.portal.controller;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.ResponseData;
import com.linkeddata.portal.entity.mongo.ApplicationDetail;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.resource.ResourceList;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.service.ApplicationsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 二级门户处理
 * @author hanmenghang
 * @date : 2022/11/02
 */
@Api(tags = "二级门户接口")
@RequestMapping("/app")
@Controller
public class ApplicationsController {
    @Autowired
    private ApplicationsService applicationsService;

    @ApiOperation("获取二级门户列表")
    @PostMapping("/list")
    @ResponseBody
    public ResponseData<PageTools<List<Applications>>> findApplications(@RequestBody ResourceListRequest request) {
        return ResponseData.success(applicationsService.findApplications(request));
    }
    @ApiOperation("获取二级门户详情")
    @PostMapping("/detail")
    @ResponseBody
    public ResponseData<ApplicationDetail> getAppByDataCenterId(@RequestBody ResourceListRequest request) {
        return ResponseData.success(applicationsService.getAppByDataCenterId(request));
    }
}
