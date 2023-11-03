package com.linkeddata.portal.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

/**
 * es客户端配置
 */
@Configuration
public class EsClientConfig  extends AbstractElasticsearchConfiguration {
    @Value("${spring.elasticsearch.url}")
    private String url ;
    @Override
    public RestHighLevelClient elasticsearchClient() {
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(url)
                .build();

        return RestClients.create(clientConfiguration).rest();
    }


}
