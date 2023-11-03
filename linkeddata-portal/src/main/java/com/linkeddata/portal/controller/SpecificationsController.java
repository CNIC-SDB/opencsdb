package com.linkeddata.portal.controller;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.ResponseData;
import com.linkeddata.portal.entity.mongo.Specifications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.service.SpecificationsService;
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
 * 词表处理
 * @author hanmenghang
 * @date : 2022/11/02
 */
@Api(tags = "词表接口")
@RequestMapping("/spe")
@Controller
public class SpecificationsController {
    @Autowired
    private SpecificationsService specificationsService;

    @ApiOperation("获取词表列表")
    @PostMapping("/list")
    @ResponseBody
    public ResponseData<PageTools<List<Specifications>>> findSpecifications(@RequestBody ResourceListRequest request) {
        return ResponseData.success(specificationsService.findSpecifications(request));
    }
}
