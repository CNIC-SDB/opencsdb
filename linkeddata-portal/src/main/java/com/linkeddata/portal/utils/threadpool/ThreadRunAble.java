package com.linkeddata.portal.utils.threadpool;

import com.linkeddata.portal.Enum.RdfFormatEnum;
import com.linkeddata.portal.entity.ExportRequest;
import com.linkeddata.portal.utils.RdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;

import java.io.*;
import java.util.concurrent.Callable;

@Slf4j
public class ThreadRunAble implements Callable<Object> {
    /**
     * 需要处理的list
     */
    private ExportRequest request;


    public ThreadRunAble(ExportRequest request) {
        this.request = request;

    }


    @Override
    public Object call() {
        try {
            log.info("执行线程开始.....");
            //查询你全图内容
            Model model = RdfUtils.queryAllGraph(request.getExportSparql(),request.getExportGraph());
            log.info("查询图完成");
            //追加维护的命令空间
            log.info(" 追加命令空间");
            model.setNsPrefix("compound", "http://chemdb.semweb.csdb.cn/resource/compound/");
            model.setNsPrefix("ontology", "http://chemdb.semweb.csdb.cn/resource/ontology/");
            model.setNsPrefix("descriptor", "http://chemdb.semweb.csdb.cn/resource/descriptor/" );
            model.setNsPrefix("reference", "http://chemdb.semweb.csdb.cn/resource/reference/");
            model.setNsPrefix("SIO", "http://semanticscience.org/resource/SIO_");
            model.setNsPrefix("CHEMINF", "http://semanticscience.org/resource/CHEMINF_");
            model.setNsPrefix("CHEBI", "http://purl.obolibrary.org/obo/CHEBI_" );
            model.setNsPrefix("dbo", "http://dbpedia.org/ontology/");
            model.setNsPrefix("dbr", "http://dbpedia.org/resource/" );
            model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#" );
            model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
            model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#" );
            model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
            model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
            model.setNsPrefix("cito", "http://purl.org/spar/cito/" );
            String[] tripleFormat = {"TURTLE","RDF/XML","N-TRIPLE","JSON-LD"};
            for (String format: tripleFormat) {
                log.info("生成 {} 格式文件中",format);
                String path = request.getExportPath()+File.separator+ request.getExportFileName() + File.separator+ request.getExportFileName()+"."+ RdfFormatEnum.getSkosType(format);
                File file = new File(request.getExportPath()+File.separator+ request.getExportFileName());
                //创建文件夹
                if (!file.exists()) {
                    file.mkdirs();
                }
                model.write(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"), format);
                log.info("生成 {} 格式文件结束",format);
            }
            log.info("文件已经全部生成完成");
            log.info("执行线程结束.....");
        }catch (Exception e){
            log.info("写入文件异常了 ");
            e.printStackTrace();
        }
        return null;
    }
}
