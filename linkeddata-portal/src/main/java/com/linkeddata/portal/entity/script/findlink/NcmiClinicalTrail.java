package com.linkeddata.portal.entity.script.findlink;

import lombok.Data;

import java.util.List;

/**
 * 人口健康实体类
 * @author wangzzhiliang
 * @date 20230211
 */
@Data
public class NcmiClinicalTrail {
    private String nctNumber;
    private String title;
    private String state;
    private List<String> conditions;
    private List<String> interventions;
    private String locations;
    private String  url;
}
