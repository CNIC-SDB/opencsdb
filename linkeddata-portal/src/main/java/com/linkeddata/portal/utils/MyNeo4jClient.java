package com.linkeddata.portal.utils;

import lombok.Getter;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

/**
 * @author jinbao
 * @since 2023/4/22
 */
@Getter
public class MyNeo4jClient implements AutoCloseable {

    private final Driver driver;

    public MyNeo4jClient(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }
}
