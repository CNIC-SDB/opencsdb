package com.linkeddata.portal.script;

import com.linkeddata.portal.entity.script.generate.GenerateOntology;
import com.linkeddata.portal.service.CreateRdfService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 创建 RDF 接口
 *
 * @author wangzhiliang
 * @date 2023/2/20 9:41
 */
@RestController
@Api(tags = "创建RDF接口")
public class CreateRdf {
    @Resource
    private CreateRdfService createRdfService;

    /**
     * 根据参数生成对应的本体文件
     *
     * @author wangzhiliang
     * @date 2023/2/20 9:41
     */
    @ApiOperation(value = "生成rdf 本体文件",  notes = "注意： 传入 base 参数 样例:http://xxx.xxx.xx/ontology/")
    @PostMapping("/generateOntology")
    @ResponseBody
    public String generateOntology(@RequestBody GenerateOntology generateOntology) {
        return createRdfService.generateOntology(generateOntology);
    }
}
