package com.linkeddata.portal.script;

import com.linkeddata.portal.utils.RdfUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;

import java.util.ArrayList;
import java.util.List;

/**
 * virtuoso数据处理
 *
 * @author 陈锟
 * @since 2023年6月14日18:20:58
 */
public class VirtuosoDeal {

    public static void main(String[] args) {
        removePubchemInClinicaltrials();
    }

    /**
     * 在临床试验数据集中删除关联pubchem的所有三元组；
     * 执行前先备份临床试验数据集
     *
     * @author 陈锟
     * @since 2023年6月14日18:20:58
     */
    public static void removePubchemInClinicaltrials() {
        VirtGraph virtGraph = new VirtGraph("Drug", "jdbc:virtuoso://10.0.90.212:1114", "dba", "0dabigta1357");
        VirtModel model = new VirtModel(virtGraph);
        Model resultModel = RdfUtils.sparqlConstruct("CONSTRUCT {\n" +
                "  ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                "}\n" +
                "WHERE {\n" +
                "  SERVICE <http://clinicaltrials.semweb.csdb.cn/sparql> {\n" +
                "    ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                "    FILTER (CONTAINS(STR(?chem), 'http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID') )\n" +
                "  }\n" +
                "}");
        List<Statement> removeStatementList = new ArrayList<>();
        StmtIterator stmtIterator = resultModel.listStatements();
        while (stmtIterator.hasNext()) {
            removeStatementList.add(stmtIterator.nextStatement());
        }
        model.remove(removeStatementList);
    }

}
