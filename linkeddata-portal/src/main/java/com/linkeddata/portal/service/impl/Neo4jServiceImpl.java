package com.linkeddata.portal.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.linkeddata.portal.entity.neo4j.Relation;
import com.linkeddata.portal.entity.semanticSearch.PathInfo;
import com.linkeddata.portal.utils.MyNeo4jClient;
import lombok.NonNull;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;

/**
 * @author jinbao
 * @since 2023/4/19
 */
@Service
public class Neo4jServiceImpl {


    @org.springframework.beans.factory.annotation.Value("${spring.neo4j.uri}")
    private String neo4jURI;
    @org.springframework.beans.factory.annotation.Value("${spring.neo4j.authentication.username}")
    private String username;
    @org.springframework.beans.factory.annotation.Value("${spring.neo4j.authentication.password}")
    private String password;

    /**
     * 判断节点是否在Neo4j中
     *
     * @param nodeName 节点uri
     * @return
     * @author gaoshuai
     */
    public Boolean checkNode(String nodeName) {
        String queryStartNodeString = """
                MATCH (s:Point) where s.name="$nodeName" RETURN s
                """
                .replace("$nodeName", nodeName);
        try (final MyNeo4jClient neo4jClient = new MyNeo4jClient(neo4jURI, username, password);
             final Session session = neo4jClient.getDriver().session()) {
            final Result result = session.run(queryStartNodeString);
            return result.hasNext() ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static void checkNode(String nodeName, String sparqlURI, Session session) {
        String queryStartNodeString = """
                MATCH (s:Point) where s.name="$nodeName" RETURN s
                """
                .replace("$nodeName", nodeName);
        final Result result = session.run(queryStartNodeString);
        // 不存在则新增节点
        if (!result.hasNext()) {
            String createNodeString = """
                    CREATE (n:Point {name:"$nodeName"})
                    """
                    .replace("$nodeName", nodeName);
            session.run(createNodeString);
        }
    }

    private static void checkRelation(String startNodeName, String relationName, String endNodeName, String sparqlURI, Session session) {
        String queryRelationString = """
                MATCH p=(s:Point)-[:out_going *1..1{sparqlURI:"$sparqlURI",name:"$relationName"}]->(e:Point)
                where s.name="$startNodeName" and e.name="$endNodeName" return p
                """
                .replace("$relationName", relationName)
                .replace("$startNodeName", startNodeName)
                .replace("$endNodeName", endNodeName)
                .replace("$sparqlURI", sparqlURI);
        final Result result = session.run(queryRelationString);
        // 不存在则新增关系
        if (!result.hasNext()) {
            String createRelation = """
                    MATCH (s:Point {name:"$startNodeName"}), (e:Point {name:"$endNodeName"})
                    CREATE (s)-[:out_going {sparqlURI:"$sparqlURI",name:"$relationName"}]->(e)
                    """
                    .replace("$startNodeName", startNodeName)
                    .replace("$endNodeName", endNodeName)
                    .replace("$relationName", relationName)
                    .replace("$sparqlURI", sparqlURI);
            session.run(createRelation);
        }
    }


    public void savePoint(List<List<String>> csv) {
        for (List<String> line : csv) {
            final String startNodeName = line.get(0);
            final String relationName = line.get(1);
            final String endNodeName = line.get(2);
            final String sparqlURI = line.get(3);

            try (final MyNeo4jClient neo4jClient = new MyNeo4jClient(neo4jURI, username, password);
                 final Session session = neo4jClient.getDriver().session()) {

                // 检查节点是否存在。不存在则新建
                checkNode(startNodeName, sparqlURI, session);
                checkNode(endNodeName, sparqlURI, session);

                // 检查关系是否存在。不存在则新建
                checkRelation(startNodeName, relationName, endNodeName, sparqlURI, session);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * 根据起点名称、终点名称、和查询深度查询路径。
     * 例如查询 p1-[]-p6
     * [
     * [{start:"p1",end:"p4",relation:"关系1",sparqlURI:"##"},{start:"p4",end:"p5",relation:"关系2",sparqlURI:"##"},{start:"p5",end:"p6",relation:"关系3",sparqlURI:"##"}],
     * [{start:"p1",end:"p4",relation:"关系1",sparqlURI:"##"},{start:"p5",end:"p4",relation:"关系2",sparqlURI:"##"},{start:"p5",end:"p6",relation:"关系3",sparqlURI:"##"}],
     * [{start:"p1",end:"p4",relation:"关系1",sparqlURI:"##"},{start:"p4",end:"p6",relation:"关系2",sparqlURI:"##"}],
     * ]
     * p1->p4->p5->p6
     * p1->p4<-p5->p6
     * p1->p4->p6
     *
     * @param startNodeName {@link Relation} 的 start 属性
     * @param endNodeName   {@link Relation} 的 end 属性
     * @param depth         查询深度
     */
    public List<List<Relation>> findByStartAndEnd(@NonNull @NotBlank String startNodeName
            , @NonNull @NotBlank String endNodeName
            , @NonNull @NotBlank String depth
            , @NonNull List<String> sparqlURIList) {
        if (sparqlURIList.size() < 1) {
            throw new RuntimeException(MessageFormat.format("端点数组不能为空, sparqlURIList:{0}", JSONObject.toJSONString(sparqlURIList)));
        }
        List<List<Relation>> result = new LinkedList<>();
        try (MyNeo4jClient neo4jClient = new MyNeo4jClient(neo4jURI, username, password);
             final Session session = neo4jClient.getDriver().session()) {
            String queryString = """
                    MATCH p=(s:Point)-[r *1..$depth]-(e:Point)
                    WHERE s.name="$startNodeName" AND e.name ="$endNodeName"
                    AND ALL(rel IN relationships(p) WHERE rel.sparqlURI IN $sparqlURIList)
                    AND ALL(node IN nodes(p)[1..-1] WHERE node <> s AND node <> e) return p
                    """
                    .replace("$depth", depth)
                    .replace("$startNodeName", startNodeName)
                    .replace("$endNodeName", endNodeName)
                    .replace("$sparqlURIList", JSONObject.toJSONString(sparqlURIList));
            final Result queryResult = session.run(queryString);
            while (queryResult.hasNext()) {
                final Value value = queryResult.next().get(0);
                // 节点集合
                final Iterable<Node> nodes = value.asPath().nodes();
                List<Node> nodeList = new LinkedList<>();
                nodes.iterator().forEachRemaining(nodeList::add);

                // 关系集合
                List<Relationship> relationshipList = new LinkedList<>();
                final Iterable<Relationship> relationships = value.asPath().relationships();
                relationships.iterator().forEachRemaining(relationshipList::add);

                // 分段信息 一组(点 边 点)
                List<Path.Segment> segmentList = new LinkedList<>();
                value.asPath().iterator().forEachRemaining(segmentList::add);
                List<Relation> relationList = new LinkedList<>();
                Relation relation;
                for (int i = 0; i < segmentList.size(); i++) {
                    final Path.Segment segment = segmentList.get(i);
                    if (Long.compare(segment.relationship().startNodeId(), segment.start().id()) == 0) {
                        relation = new Relation(segment.start().get("name").asString()
                                , segment.end().get("name").asString()
                                , segment.relationship().get("name").asString()
                                , segment.relationship().get("sparqlURI").asString());
                    } else {
                        relation = new Relation(segment.end().get("name").asString()
                                , segment.start().get("name").asString()
                                , segment.relationship().get("name").asString()
                                , segment.relationship().get("sparqlURI").asString());
                    }
                    relationList.add(relation);
                }
                result.add(relationList);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 根据一条路径生成 sparql 语句
     *
     * @param relation 路径对象
     * @return
     */
    public String getSparql(PathInfo relation) {
        final String startURI = relation.getStartIri();
        final String endURI = relation.getEndIri();
        final List<Relation> relationList = relation.getPath();

        // 路径为空
        if (Objects.isNull(relationList) || relationList.size() < 1) {
            return null;
        }
        //路径只有一步
        if (relationList.size() == 1) {
            return onlyOneStep(relation);
        }

        // 路径大于一步
        String query = """
                CONSTRUCT {
                    $construct
                }
                WHERE {
                    $where
                }
                """;
        StringBuilder construct = new StringBuilder();
        StringBuilder where = new StringBuilder();
        final HashMap<String, String> dynamicVariableNameMap = new LinkedHashMap<>();
        for (int i = 1; i < relationList.size(); i++) {
            final Relation previous = relationList.get(i - 1);
            final String previousRelationStartName = previous.getStart();
            getDynamicVariable(previousRelationStartName, dynamicVariableNameMap);
            final String previousRelationEndName = previous.getEnd();
            getDynamicVariable(previousRelationEndName, dynamicVariableNameMap);

            final Relation next = relationList.get(i);
            final String nextRelationStartName = next.getStart();
            getDynamicVariable(nextRelationStartName, dynamicVariableNameMap);
            final String nextRelationEndName = next.getEnd();
            getDynamicVariable(nextRelationEndName, dynamicVariableNameMap);

            // 起始第一段关系特殊处理
            if (i == 1) {
                final String s = dynamicVariableNameMap.get(previousRelationStartName);
                final String p = previous.getRelation();
                final String o = dynamicVariableNameMap.get(previousRelationEndName);
                construct.append(s).append(" <").append(p).append("> ").append(o).append(" .\n");

                // 判断起点的类型
                String startClassName;
                if (previousRelationEndName.equals(nextRelationStartName) || previousRelationEndName.equals(nextRelationEndName)) {
                    startClassName = previousRelationStartName;
                } else {
                    startClassName = previousRelationEndName;
                }
                where.append("SERVICE SILENT <").append(previous.getSparqlURI()).append("> { \n")
                        .append("FILTER (").append(dynamicVariableNameMap.get(startClassName)).append("=<").append(startURI).append(">)\n")
                        .append(s).append(" <").append(p).append("> ").append(o).append(" .\n");
            }

            // 生成中间的 construct 语句
            final String s = dynamicVariableNameMap.get(nextRelationStartName);
            final String p = next.getRelation();
            final String o = dynamicVariableNameMap.get(nextRelationEndName);

            // 结尾的 construct 单独处理
            construct.append(s).append(" <").append(p).append("> ").append(o).append(" .\n");
            if ((i == relationList.size() - 1) && endIsClass(endURI, next)) { // endURI 是类的情况
                if (endURI.equals(nextRelationEndName)) {
                    construct.append(o).append(" <http://end> <http://end> .");
                } else {
                    construct.append(s).append(" <http://end> <http://end> .");
                }
            }

            // 生成中间的 where 语句
            // 相邻两个关系中 sparqlURI 相同则合并条件
            if (next.getSparqlURI().equals(previous.getSparqlURI())) {
                where.append(s).append(" <").append(p).append("> ").append(o).append(" .\n");
            } else { // sparqlURI 不同则中断并补全上一个条件，开启一个新的条件
                where.append("}\n");
                where.append("SERVICE SILENT <").append(next.getSparqlURI()).append("> { \n")
                        .append(s).append(" <").append(p).append("> ").append(o).append(" .\n");
            }

            // 末尾最后一段关系特殊处理
            if ((i == relationList.size() - 1)) {
                if (!endIsClass(endURI, next)) { // 当终点是实体的时候才执行
                    // 判断终点的类型
                    String endClassName;
                    if (nextRelationEndName.equals(previousRelationStartName) || nextRelationEndName.equals(previousRelationEndName)) {
                        endClassName = nextRelationStartName;
                    } else {
                        endClassName = nextRelationEndName;
                    }
                    where.append("FILTER (").append(dynamicVariableNameMap.get(endClassName)).append("=<").append(endURI).append(">)\n}");
                } else { // 判断是否要补全 SERVICE 语句
                    if (!where.substring(where.length() - 1, where.length()).equals("}")) {
                        where.append("\n}");
                    }
                }
            }
        }
        final StringBuilder result = new StringBuilder(query.replace("$construct", construct).replace("$where", where))
                .append("LIMIT 20");
        return result.toString();
    }

    private String onlyOneStep(PathInfo pathInfo) {
        final String startURI = pathInfo.getStartIri();
        final String endURI = pathInfo.getEndIri();
        final List<Relation> relationList = pathInfo.getPath();
        final Relation relation = relationList.get(0);
        final HashMap<String, String> dynamicVariableNameMap = new LinkedHashMap<>();
        getDynamicVariable(startURI, dynamicVariableNameMap);
        getDynamicVariable(endURI, dynamicVariableNameMap);

        String ifEndIsClass2Construct = "";
        String ifEndIsClass2Where = "";
        if (endIsClass(endURI, relation)) { // 终点为类
            if (Objects.equals(endURI, relation.getEnd())) {
                ifEndIsClass2Construct = dynamicVariableNameMap.get(endURI) + " <$isEnd> <$isEnd> .".replace("$isEnd", "http://end");
            } else {
                ifEndIsClass2Construct = dynamicVariableNameMap.get(startURI) + " <$isEnd> <$isEnd> .".replace("$isEnd", "http://end");
            }
        } else {
            ifEndIsClass2Where = "FILTER (" + dynamicVariableNameMap.get(endURI) + " = <$endIRI>)".replace("$endIRI", endURI);
        }
        String query = """
                CONSTRUCT {
                    $s1 <$relation> $s2 .
                    $ifEndIsClass2Construct
                }
                WHERE {
                    SERVICE SILENT <$sparql> {
                        FILTER ($s2 = <$startIRI>)
                        $s1 <$relation> $s2 .
                        $ifEndIsClass2Where
                    }
                } LIMIT 20
                """
                .replace("$s1", dynamicVariableNameMap.get(startURI))
                .replace("$s2", dynamicVariableNameMap.get(endURI))
                .replace("$relation", relation.getRelation())
                .replace("$sparql", relation.getSparqlURI())
                .replace("$startIRI", startURI)
                .replace("$ifEndIsClass2Construct", ifEndIsClass2Construct)
                .replace("$ifEndIsClass2Where", ifEndIsClass2Where);
        return query;
    }

    public String getSparql2(PathInfo pathInfo) {
        final String startIri = pathInfo.getStartIri();
        final String endIri = pathInfo.getEndIri();
        String endClassIri = pathInfo.getEndClassIri();
        final List<Relation> relationList = pathInfo.getPath();
        // 路径为空
        if (Objects.isNull(relationList) || relationList.size() < 1) {
            return null;
        }

        String sparqlQuery = """
                CONSTRUCT {
                $construct
                }
                WHERE {
                $where
                } limit 20
                """;
        StringBuilder construct = new StringBuilder();
        StringBuilder where = new StringBuilder();
        Relation relation;
        Relation nextRelation;
        final HashMap<String, String> paramNameMap = new LinkedHashMap<>(); // 变量表
        final int endIndex = Math.max((relationList.size() - 2), 0); // 遍历末尾点
        final int stopIndex = Math.max((relationList.size() - 1), 1); // 遍历终止点
        for (int i = 0; i < stopIndex; i++) {
            relation = relationList.get(i);
            try {
                nextRelation = relationList.get(i + 1);
            } catch (IndexOutOfBoundsException e) {
                nextRelation = relationList.get(relationList.size() - 1);
            }
            final String relationStart = relation.getStart();
            final String relationEnd = relation.getEnd();
            final String nextRelationStart = nextRelation.getStart();
            final String nextRelationEnd = nextRelation.getEnd();

            getDynamicVariable(relationStart, paramNameMap);
            getDynamicVariable(relationEnd, paramNameMap);
            getDynamicVariable(nextRelationStart, paramNameMap);
            getDynamicVariable(nextRelationEnd, paramNameMap);

            if (i == 0) { // 首个关系
                String c = """
                        $s <$p> $o .
                        """
                        .replace("$s", paramNameMap.get(relationStart))
                        .replace("$p", relation.getRelation())
                        .replace("$o", paramNameMap.get(relationEnd));
                construct.append(c);

                final String startClassIri = whichIsStart(relation, nextRelation);

                String w = """
                        SERVICE SILENT <$sparqlEndpoint> {
                        FILTER ( $startClassIri = <$startIri> )
                        $s <$p> $o .
                        """
                        .replace("$sparqlEndpoint", relation.getSparqlURI())
                        .replace("$startClassIri", paramNameMap.get(startClassIri))
                        .replace("$startIri", startIri)
                        .replace("$s", paramNameMap.get(relationStart))
                        .replace("$p", relation.getRelation())
                        .replace("$o", paramNameMap.get(relationEnd));
                where.append(w);
            }

                // 中间关系
                String c = """
                        $s <$p> $o .
                        """
                        .replace("$s", paramNameMap.get(nextRelationStart))
                        .replace("$p", nextRelation.getRelation())
                        .replace("$o", paramNameMap.get(nextRelationEnd));
                construct.append(c);

                if (nextRelation.getSparqlURI().equals(relation.getSparqlURI())) { // 相邻路径在同一个sparql端点中
                    String w = """
                            $s <$p> $o .
                            """
                            .replace("$s", paramNameMap.get(nextRelationStart))
                            .replace("$p", nextRelation.getRelation())
                            .replace("$o", paramNameMap.get(nextRelationEnd));
                    where.append(w);
                } else { // 相邻路径不在同一个sparql端点中
                    String w = """
                            }
                            SERVICE SILENT <$sparqlEndpoint> {
                            $s <$p> $o .
                            """
                            .replace("$sparqlEndpoint", nextRelation.getSparqlURI())
                            .replace("$s", paramNameMap.get(nextRelationStart))
                            .replace("$p", nextRelation.getRelation())
                            .replace("$o", paramNameMap.get(nextRelationEnd));
                    where.append(w);
                }


            if (i == endIndex) { // 结尾关系
                if (Objects.nonNull(endClassIri)) { // 终点是一个类，问句3
                    String c_ = """
                            $s <http://end> <http://end> .
                            """
                            .replace("$s", paramNameMap.get(endClassIri));
                    construct.append(c_);
                    String w = """
                            $s <$p> $o .
                            }
                            """
                            .replace("$s", paramNameMap.get(nextRelationStart))
                            .replace("$p", nextRelation.getRelation())
                            .replace("$o", paramNameMap.get(nextRelationEnd));
                    where.append(w);
                } else { // 终点是一个实体
                    String c_ = """
                            $s <$p> $o .
                            """
                            .replace("$s", paramNameMap.get(nextRelationStart))
                            .replace("$p", nextRelation.getRelation())
                            .replace("$o", paramNameMap.get(nextRelationEnd));
                    construct.append(c_);
                    endClassIri = whichIsEnd(relation, nextRelation);
                    String w = """
                            $s <$p> $o .
                            FILTER ($endClassIri = <$endIri>) .
                            }
                            """
                            .replace("$s", paramNameMap.get(nextRelationStart))
                            .replace("$p", nextRelation.getRelation())
                            .replace("$o", paramNameMap.get(nextRelationEnd))
                            .replace("$endClassIri", paramNameMap.get(endClassIri))
                            .replace("$endIri", endIri);
                    where.append(w);
                }
            }

        }
        return distinctLine(sparqlQuery.replace("$construct", construct.toString()).replace("$where", where.toString()));
    }

    /**
     * 只有一步深度路径的语句去重
     *
     * @param sparql
     * @return
     */
    private String distinctLine(String sparql) {
        final StringBuilder s = new StringBuilder();
        final String[] split = sparql.split("\n");
        for (int i = 0; i < split.length; i++) {
            final String l1 = split[i];
            s.append(l1).append("\n");
            try {
                if (l1.equals(split[i + 1])) { //相邻两个句子完全相同则跳过第二句
                    i++;
                }
            } catch (Exception e) {
                System.out.println(l1);
            }
        }
        return s.toString();
    }

    private String whichIsEnd(Relation relation, Relation nextRelation) {
        if (Objects.equals(relation, nextRelation)) {
            return nextRelation.getEnd();
        }
        if (Objects.equals(nextRelation.getStart(), relation.getStart()) || Objects.equals(nextRelation.getStart(), relation.getEnd())) {
            return nextRelation.getEnd();
        }
        return nextRelation.getStart();
    }

    private String whichIsStart(Relation relation, Relation nextRelation) {
        if (Objects.equals(relation, nextRelation)) {
            return relation.getStart();
        }
        if (Objects.equals(relation.getEnd(), nextRelation.getStart()) || Objects.equals(relation.getEnd(), nextRelation.getEnd())) {
            return relation.getStart();
        }
        return relation.getEnd();
    }

    private boolean endIsClass(String endURI, Relation relation) {
        if (endURI.equals(relation.getStart()) || endURI.equals(relation.getEnd())) {
            return true;
        }
        return false;
    }

    /**
     * 根据 URI 动态生成变量
     *
     * @param URI
     * @param map
     */
    private void getDynamicVariable(String URI, Map<String, String> map) {
        if (Objects.isNull(map.get(URI))) {
            final int size = map.size();
            map.put(URI, "?s" + Math.addExact(size, 1));
        }
    }
}
