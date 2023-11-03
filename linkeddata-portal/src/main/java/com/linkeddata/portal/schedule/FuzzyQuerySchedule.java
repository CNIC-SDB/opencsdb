//package com.linkeddata.portal.schedule;
//
//import com.linkeddata.portal.entity.mongo.Dataset;
//import com.linkeddata.portal.utils.RdfUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import javax.annotation.Resource;
//import java.util.List;
//@Slf4j
//@Configuration  //标记配置类
//@EnableScheduling   //开启定时任务
//public class FuzzyQuerySchedule {
//    @Resource
//    private MongoTemplate mongoTemplate;
//        @Scheduled(cron = "0 */1 * * * ?")
//        private void myTasks() {
//            String sparql = "";
//            try{
//
//                List<Dataset> dataSetList = mongoTemplate.findAll(Dataset.class);
//                for (Dataset dataSet: dataSetList
//                ) {
//                    sparql = dataSet.getSparql();
//                    RdfUtils.queryTriple(sparql, "SELECT DISTINCT ?s WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label . FILTER ( CONTAINS ( ?label, 'w' ) ) . } LIMIT 1");
//                }
//            }catch (Exception e){
//                log.info("定时访问 <" + sparql + "> 出现错误");
//            }
//
//
//        }
//}
//数据库的原因，现在不需要做轮询了，注释掉
