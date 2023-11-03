package com.linkeddata.portal.script.pubchem.entity;

import lombok.Data;

/**
 * 过程所化合物表，新表，compound
 *
 * @author chenkun
 * @since 2023年6月27日18:22:28
 */
@Data
public class Compound {

    // cid
    private int cid;

    // iupac_name
    private String iupacName;

    // molecular_formula
    private String molecularFormula;

    // molecular_weight
    private double molecularWeight;

    // inchikey
    private String inChIKey;

    // inchi
    private String inChI;

    // smiles
    private String smiles;

}
