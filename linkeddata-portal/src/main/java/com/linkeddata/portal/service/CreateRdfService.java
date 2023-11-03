package com.linkeddata.portal.service;

import com.linkeddata.portal.entity.script.generate.GenerateOntology;

/**
 * 创建 rdf service 层
 *
 * @author wangzhiliang
 * @date 2023/2/20 10:22
 */
public interface CreateRdfService {
    /**
     * 生成 rdf 本体文件
     *
     * @param generateOntology 生成文件请求参数
     * @return 执行结果
     * @author wangzhiliang
     * @date 2023/2/20
     */
    String generateOntology(GenerateOntology generateOntology);
}
