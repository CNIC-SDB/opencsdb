package com.linkeddata.portal.script.pubchem.entity;

import lombok.Data;

/**
 * 过程所化合物表，新表，cas，一对多
 *
 * @author chenkun
 * @since 2023年6月27日18:21:59
 */
@Data
public class Cas {

    // cid
    private int cid;

    // cas
    private String cas;

}
