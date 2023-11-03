package com.linkeddata.portal.script.pubchem;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 下载pubchem中的化合物信息，补充到过程所
 * 配置：spring.profiles.active=chemdb
 */
@Controller
@RequestMapping("/downloadPubchem")
public class DownloadPubchemController {

    @Resource
    private DownloadPubchemService downloadPubchemService;

    /**
     * 测试URL：
     *      curl "http://10.0.85.83:9901/downloadPubchem/downloadPubchemCompounds?threadName=1&tableName=1&threadStartCid=1&threadEndCid=1000000"
     *      curl "http://10.0.85.83:9902/downloadPubchem/downloadPubchemCompounds?threadName=2&tableName=1&threadStartCid=1000001&threadEndCid=2000000"
     *      curl "http://10.0.85.83:9903/downloadPubchem/downloadPubchemCompounds?threadName=3&tableName=1&threadStartCid=2000001&threadEndCid=3000000"
     *      curl "http://10.0.85.83:9904/downloadPubchem/downloadPubchemCompounds?threadName=4&tableName=1&threadStartCid=3000001&threadEndCid=4000000"
     *      curl "http://10.0.85.83:9905/downloadPubchem/downloadPubchemCompounds?threadName=5&tableName=1&threadStartCid=4000001&threadEndCid=5000000"
     *
     *      curl "http://10.0.85.83:9901/downloadPubchem/downloadPubchemCompounds?threadName=1&tableName=1&threadStartCid=5000001&threadEndCid=10000000"
     *      curl "http://10.0.85.83:9902/downloadPubchem/downloadPubchemCompounds?threadName=2&tableName=2&threadStartCid=10000001&threadEndCid=15000000"
     *      curl "http://10.0.85.83:9903/downloadPubchem/downloadPubchemCompounds?threadName=3&tableName=2&threadStartCid=15000001&threadEndCid=20000000"
     *
     * @param threadName     线程名称，1,2,3,...
     * @param tableName     要保存的mongo表名，如果`tableName=1`则会保存到表`compound_1`
     * @param threadStartCid 线程开始CID
     * @param threadEndCid   线程截止CID
     * @author chenkun
     * @since 2023年6月27日18:23:54
     */
    /*
    mongo创建索引脚本：
        for (var i = 1; i <= 5; i++) {
            db.getCollection('compound_' + i).createIndex({ cid: 1 })
            db.getCollection('compound_' + i).createIndex({ iupac_name: 1 })
            db.getCollection('compound_' + i).createIndex({ molecular_formula: 1 })
            db.getCollection('compound_' + i).createIndex({ molecular_weight: 1 })
            db.getCollection('synonym_' + i).createIndex({ cid: 1 })
            db.getCollection('synonym_' + i).createIndex({ synonym: 1 })
        }
     */
    @RequestMapping("/downloadPubchemCompounds")
    @ResponseBody
    public Object downloadPubchemCompounds(
            @RequestParam("threadName") int threadName,
            @RequestParam("tableName") int tableName,
            @RequestParam("threadStartCid") int threadStartCid,
            @RequestParam("threadEndCid") int threadEndCid) {
        try {
            downloadPubchemService.downloadPubchemCompounds(threadName, tableName, threadStartCid, threadEndCid);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "success";
    }

    @RequestMapping("/test")
    @ResponseBody
    public Object test(
            @RequestParam("threadName") int threadName,
            @RequestParam("tableName") int tableName,
            @RequestParam("threadStartCid") int threadStartCid,
            @RequestParam("threadEndCid") int threadEndCid) {
        System.out.println("test：threadName=" + threadName + "，tableName=" + tableName + "，threadStartCid=" + threadStartCid + "，threadEndCid=" + threadEndCid);
        return "success";
    }

}
