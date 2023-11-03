package com.linkeddata.portal.utils.threadpool;

import com.linkeddata.portal.utils.RdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.OWL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 化合物关联 Mesh 词表多线程关系
 *
 * @author wangzhiliang
 */
@Slf4j
public class ChemSameAsMeshRunAble implements Callable<Object> {
    private String fusekiIp ;
    private String fusekiGraph ;
    private String chemSparql;
    private String chemEndPoint;

    /**
     *
     * @param chemSparql  每个线程的查询语句
     * @author wangzhiliang
     * @date 20230215
     */
    public ChemSameAsMeshRunAble(String chemSparql, String fusekiIp, String fusekiGraph ,String  chemEndPoint) {
        this.chemSparql = chemSparql;
        this.fusekiIp = fusekiIp;
        this.fusekiGraph = fusekiGraph;
        this.chemEndPoint = chemEndPoint;

    }

    @Override
    public Object call() throws Exception {
        log.info("执行线程开始.....");
        try {
            //查询化合端点
            ResultSet resultSet = RdfUtils.queryTriple(chemEndPoint,chemSparql);
            List<Statement> statements = new ArrayList<>();
            if(null != resultSet){
                while (resultSet.hasNext()){
                    QuerySolution querySolution  = resultSet.nextSolution();
                    Resource chemSubj = querySolution.getResource("subj");
                    RDFNode  cas = querySolution.get("cas");
                    String meshSparql = "SELECT ?mesh from  <http://id.nlm.nih.gov/mesh> WHERE {\n" +
                            "\t?mesh <http://id.nlm.nih.gov/mesh/vocab#relatedRegistryNumber> ?object.\n" +
                            "\t filter (contains(?object,'"+cas+" (')) \n" +
                            "}";

                    ResultSet meshResult = RdfUtils.queryTriple("https://id.nlm.nih.gov/mesh/sparql",meshSparql);
                    if(null != meshResult){
                        while (meshResult.hasNext()){
                            QuerySolution meshSolution = meshResult.nextSolution();
                            Resource mesSubj = meshSolution.getResource("mesh");
                            statements.add(new StatementImpl(chemSubj, OWL.sameAs,mesSubj));
                        }
                    }
                }
            }
            RdfUtils.saveTriPle(statements,fusekiIp,fusekiGraph);
            log.info(" 存储图完成  {} 三元组数 共计 {} 个",fusekiGraph, statements.size());

            log.info("执行线程结束.....");
        }catch (Exception e){
            log.info("执行线程异常了");
        }

        return null;
    }
}
