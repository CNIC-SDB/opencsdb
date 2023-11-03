package com.linkeddata.portal.entity.script.findlink;

import lombok.Data;

/**
 * 人口健康疾病实体
 * @author wangzzhiliang
 * @date 20230211
 */
@Data
public class NcmiClinicalDrug {
    private String title;
    private String drugBankNum;
    private String mechanism;
    private String reference;
    private String sourceUrl;
    private String attachment;
}
