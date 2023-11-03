package com.linkeddata.portal.script.pubchem.entity;

import lombok.Data;

/**
 * 过程所化合物表，新表，synonyms，一对多
 *
 * @author chenkun
 * @since 2023年6月27日18:24:15
 */
@Data
public class Synonym {

    // cid
    private int cid;

    // synonym
    private String synonym;

}
