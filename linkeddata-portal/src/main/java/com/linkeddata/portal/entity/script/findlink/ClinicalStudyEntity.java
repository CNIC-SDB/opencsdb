package com.linkeddata.portal.entity.script.findlink;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * XML解析提取字段对象
 *
 * @author wangzhiliang
 * @date 20230208
 */
@Data
public class ClinicalStudyEntity {
    /**
     * 研究编号
     */
    private String nct;
    /**
     * 研究链接
     * */
    private String url;
    /**
     * 研究主题
     */
    private String briefTitle;
    /**
     * 研究状况
     */
    private List<String> conditions;
    /**
     * 研究措施
     */
    private List<Map<String,Object>> drugs;
     /**
     * 研究地点
     */
    private  List<String> locations;
     /**
     * 研究简要总结
     */
    private String briefSummary;
    /**
     * 研究意图
     */
    private String primaryPurpose;

}
