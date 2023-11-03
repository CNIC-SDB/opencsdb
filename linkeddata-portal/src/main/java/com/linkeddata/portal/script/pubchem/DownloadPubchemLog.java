package com.linkeddata.portal.script.pubchem;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("download_pubchem_log")
@Data
public class DownloadPubchemLog {

    private int threadName;
    private int startCid;
    private int endCid;

    // 请求结果
    private boolean resultProperty;
    private boolean resultSynonym;
    // 是否抛异常
    private boolean exceptionFlag;

    // 执行用时
    private long execSeconds;
    // 执行时间
    private Date createDate;

}
