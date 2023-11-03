// 后续改为调用 SemanticSearchUtils.dealPrefixReturnShort
// 改为从mongo表中读取，不然每次新增类都需要改代码 update by chenkun 2023年10月18日17:36:18

//package com.linkeddata.portal.Enum;
//
//import lombok.Getter;
//
//import java.io.UnsupportedEncodingException;
//import java.net.URLDecoder;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * 命名空间前缀枚举类
// *
// * @author wangzhiliang
// */
//public enum PrefixEnum {
//
//    //9.6课题 对地观测、气象 、冰川冻土
//    met("http://met.semweb.csdb.cn/resource/", "met:"),
//    cryosphere("http://cryosphere.semweb.csdb.cn/resource/", "cryosphere:"),
//    geoss("http://geoss.semweb.csdb.cn/resource/", "geoss:"),
//    dataset("http://skos.semweb.csdb.cn/vocabulary/dataset#", "dataset:"),
//    schema("https://schema.org/", "schema:"),
//
//    // 应用示范
//    //化合物
//    chemdb("http://chemdb.semweb.csdb.cn/resource/", "chemdb:"),
//    compound("http://rdf.ncbi.nlm.nih.gov/pubchem/compound/", "compound:"),
//    //植物
//    plant("https://www.plantplus.cn/plantsw/resource/", "plant:"),
//    dsw("http://purl.org/dsw/", "dsw:"),
//    dwc("http://rs.tdwg.org/dwc/terms/", "dwc:"),
//    dwciri("http://rs.tdwg.org/dwc/iri/", "dwciri:"),
//    gbif("https://www.gbif.org/species/", "gbif:"),
//    openbiodiv("https://openbiodiv.net/", "openbiodiv:"),
//    //微生物
//    ncov("http://nmdc.cn/ontology/ncov/", "ncov:"),
//    micro("http://micro.semweb.csdb.cn/resource/", "micro:"),
//    NCBI("http://purl.obolibrary.org/obo/NCBITaxon_", "NCBITaxon:"),
//    //理化所
//    xtipc("http://xtipc.semweb.csdb.cn/resource/", "xtipc:"),
//    repr("https://w3id.org/reproduceme#","repr:"),
//    //人口健康
//    ncmi("http://ncmi.semweb.csdb.cn/resource/","ncmi:"),
//    //pubmed
//    pubmed("http://pubmed.semweb.csdb.cn/resource/","pubmed:"),
//    //clinicaltrials.gov
//    clinicaltrials("http://clinicaltrials.semweb.csdb.cn/resource/","clinicaltrials:"),
//    //mesh
//    mesh("http://id.nlm.nih.gov/mesh/","mesh:"),
//    //青藏
//    tpdc("http://tpdc.semweb.csdb.cn/resource/","tpdc:"),
//    tpdcOntology("http://tpdc.semweb.csdb.cn/ontology/","tpdcOntology:"),
//    //动物所
//    ioz("http://ioz.semweb.csdb.cn/resource/","ioz:"),
//    //有机所
//    organchem("http://organchem.semweb.csdb.cn/resource/","organchem:"),
//    //版纳所
//    xtbg("http://xtbg.semweb.csdb.cn/resource/","xtbg:"),
//
//    //公共
//    SIO("http://semanticscience.org/resource/", "sio:"),
//    //    CHEMINF("http://semanticscience.org/resource/CHEMINF_", "CHEMINF:"),
//    CHEBI("http://purl.obolibrary.org/obo/CHEBI_", "CHEBI:"),
//    dbo("http://dbpedia.org/ontology/", "dbo:"),
//    dbr("http://dbpedia.org/resource/", "dbr:"),
//    obo("http://purl.obolibrary.org/obo/", "obo:"),
//    xsd("http://www.w3.org/2001/XMLSchema#", "xsd:"),
//    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:"),
//    rdfs("http://www.w3.org/2000/01/rdf-schema#", "rdfs:"),
//    owl("http://www.w3.org/2002/07/owl#", "owl:"),
//    dcterms("http://purl.org/dc/terms/", "dcterms:"),
//    skos("http://www.w3.org/2004/02/skos/core#", "skos:"),
//    cito("http://purl.org/spar/cito/", "cito:"),
//    nci("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#", "nci:"),
//    snomedct("http://purl.bioontology.org/ontology/SNOMEDCT/", "snomedct:"),
//    wd("https://www.wikidata.org/wiki/", "wd:"),
//    bao("http://www.bioassayontology.org/bao#", "bao:"),
//    ndfrt("http://purl.bioontology.org/ontology/NDFRT/", "ndfrt:"),
//    geodata("http://sws.geonames.org/", "geodata:"); // geonames
//
//
//    @Getter
//    private String prefix;
//    @Getter
//    private String prefixShort;
//
//    PrefixEnum(String prefix, String prefixShort) {
//        this.prefix = prefix;
//        this.prefixShort = prefixShort;
//    }
//
//    /**
//     * 把传入uri进行替换成缩写格式
//     *
//     * @param uri uri
//     * @author wangzhiliang
//     * @date 20220909
//     */
//    public static String dealPrefixReturnShort(String uri) {
//        if (uri == null) {
//            return uri;
//        }
//        try {
//            uri = URLDecoder.decode(uri, "UTF-8");
//            for (PrefixEnum prefix : PrefixEnum.values()) {
//                if (uri.contains(prefix.getPrefix())) {
//                    Pattern p = Pattern.compile(prefix.getPrefix());
//                    Matcher m = p.matcher(uri);
//                    return m.replaceAll(prefix.getPrefixShort());
//                }
//            }
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return uri;
//    }
//
//
//}
