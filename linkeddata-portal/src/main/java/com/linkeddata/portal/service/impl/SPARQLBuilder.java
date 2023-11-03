package com.linkeddata.portal.service.impl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路径检索,x和y有无关联,x和y都是同一类,生成sparql查询语句
 *
 * @author : gaoshuai
 * @date : 2023/3/17 9:56
 */
@Component
public class SPARQLBuilder {
    // 每条sparql语句查询结果限制
    private static final int LIMIT = 50;
    // 最大路径长度
    private static final int MAX_DISTANCE = 2;

    /**
     * 生成sparql语句的最外层方法，
     *
     * @param object1 资源1
     * @param object2 资源2
     * @return
     */
    public List buildQueries(String object1, String object2, String endPoint, Integer questionType) {

        return getQueries(object1, object2, MAX_DISTANCE, LIMIT, null, null, 0, endPoint, questionType);
    }

    /**
     * Return a set of queries to find relations between two objects.
     * 生成所有可能路径的sparql查询语句，主要分为两部分，一、A 同向一路到B的，二、有一次反向的有中间节点的语句
     *
     * @param object1     First object.
     * @param object2     Second object.
     * @param maxDistance The maximum distance up to which we want to search.
     * @param limit       The maximum number of results per SPARQL query (=LIMIT).
     * @return A two dimensional array of the form $array[$distance][$queries].
     */
    private List<String> getQueries(String object1, String object2, int maxDistance, int limit, List ignoredObjects, List ignoredProperties, int avoidCycles, String endPoint, Integer questionType) {
        Map options = new HashMap();
        options.put("object1", object1);
        options.put("object2", object2);
        options.put("limit", limit);
        options.put("ignoredObjects", ignoredObjects);
        options.put("ignoredProperties", ignoredProperties);
        options.put("avoidCycles", avoidCycles);
        List<String> list = new ArrayList();
        for (int distance = 1; distance <= maxDistance; distance++) {
            list.add(direct(object1, object2, distance, options, endPoint, questionType));
            // 下面查的是需要中间节点的，也就是有反向记录的，有中间节点middle的，和我们设计的穷举法不太一样
            // a从1开始，小于等于最大距离
            for (int a = 1; a <= distance; a++) {
                // b从1开始，小于等于最大距离
                for (int b = 1; b <= distance; b++) {
                    // 判断middle节点出现的位置，例如A到B路径长度为5，middle可能出现在距离A 1 距离B 4，距离A 2，距离B 3，距离A 3 距离B 2的位置。
                    if ((a + b) == distance) {
                        list.add(connectedViaAMiddleObject(object1, object2, a, b, true, options, endPoint, questionType));
                        list.add(connectedViaAMiddleObject(object1, object2, a, b, false, options, endPoint, questionType));
                    }
                }
            }
        }
        return list;
    }

    /**
     * 拼接同一方向路径的查询语句
     * 生成从 $object1 to $object2的sparql语句，同方向一路到达的语句
     *
     * @param object1  起点
     * @param object2  终点
     * @param distance 路径长度
     * @return
     */
    private String direct(String object1, String object2, int distance, Map options, String endPoint, Integer questionType) {
        Map vars = new HashMap();
        List objList = new ArrayList();
        List predList = new ArrayList();
        // distance是路径的距离，外层调用的方法会进行循环从1到maxDistance
        if (distance == 1) {
            // 如果距离是1 直接拼接 start ?pf1 object 并将pf1放入谓语列表中
            String retval = this.uri(object1) + " ?pf1 " + this.uri(object2) + " .";
            predList.add("?pf1");
            vars.put("pred", predList);
            if (questionType == 3) {
                retval = this.uri(object1) + " ?pf1 " + " ?of1 . ?end a " + this.uri(object2) + ".";
            }
            return completeQuery(retval, options, vars, endPoint, questionType);
        } else {
            // 如果距离大于1，生成同向的查询语句
            StringBuilder query = new StringBuilder(uri(object1) + " ?pf1 ?of1 " + ".\n");
            predList.add("?pf1");
            objList.add("?of1");
            // 循环遍历最大距离次数 ，生成路径中间的 o1 p2 o2. o2 p3 o3. 这些
            for (int i = 1; i < distance - 1; i++) {
                query.append("?of").append(i).append(" ?pf").append(i + 1).append(" ?of").append(i + 1).append(".\n");
                predList.add("?pf" + (i + 1));
                objList.add("?of" + (i + 1));
            }
            if (questionType == 3) {
                query.append("?of").append(distance - 1).append(" ?pf").append(distance).append(" ").append("?end").append(" . ");
                query.append(" ?end a ").append(uri(object2)).append(" .");
            } else {
                query.append("?of").append(distance - 1).append(" ?pf").append(distance).append(" ").append(uri(object2)).append(" .");
            }

            predList.add("?pf" + distance);
            vars.put("obj", objList);
            vars.put("pred", predList);
            return completeQuery(query.toString(), options, vars, endPoint, questionType);
        }
    }

    /**
     * 拼接有中间节点的语句
     * Return a set of queries to find relations between two objects,
     * which are connected via a middle objects.
     * $dist1 and $dist2 give the distance between the first and second object to the middle
     * they have ti be greater that 1
     * <p>
     * Patterns:
     * if $toObject is true then:
     * PATTERN												DIST1	DIST2
     * first-->?middle<--second 						  	1		1
     * first-->?of1-->?middle<--second						2		1
     * first-->?middle<--?os1<--second 						1		2
     * first-->?of1-->middle<--?os1<--second				2		2
     * first-->?of1-->?of2-->middle<--second				3		1
     * <p>
     * if $toObject is false then (reverse arrows)
     * first<--?middle-->second
     * <p>
     * the naming of the variables is "pf" and "of" because predicate from "f"irst object
     * and "ps" and "os" from "s"econd object
     *
     * @param first    First object.    第一个节点
     * @param second   Second object.   第二个节点
     * @param dist1    Distance of first object from middle 第一个节点到middle的距离
     * @param dist2    Distance of second object from middle 第二个节点到middle的距离
     * @param toObject Boolean reverses the direction of arrows. 是否反转箭头方向
     * @return the SPARQL Query as a String
     */
    private String connectedViaAMiddleObject(String first, String second, int dist1, int dist2, Boolean toObject, Map options, String endPoint, Integer questionType) {
        Map vars = new HashMap();
        List<String> predList = new ArrayList<>();
        List<String> objList = new ArrayList<>();
        objList.add("?middle");
        // 谓语p，宾语o的后缀，拼接为pf,of
        String fs = "f";
        // tmpdist 是 A 到middle的距离
        int tmpdist = dist1;
        // 用于记录生成第二次反向的路径， twice=0的时候处理A->middle，twice=1的时候处理B->middle
        int twice = 0;
        // coreQuery核心查询语句
        StringBuilder coreQuery = new StringBuilder();
        // 第一个对象，相当于中间路程的起点
        String object = first;
        while (twice < 2) {
            // 如果 A到middle距离是1，核心的路程就是  object1  ?pfs1  ?middle
            if (tmpdist == 1) {
                if (questionType == 3 && object.equals(second)) {
                    coreQuery.append(this.toPattern("?end ", "?p" + fs + "1", "?middle", toObject));
                    coreQuery.append(this.toPattern("?end ", " a ", uri(object), true));
                } else {
                    coreQuery.append(this.toPattern(uri(object), "?p" + fs + "1", "?middle", toObject));
                }
                // 将谓语存放到Array中
                predList.add("?p" + fs + "1");
                vars.put("pred", predList);
            } else {
                // 如果 A到middle距离大于1，
                // object ?pfs1 ?ofs1，第一步的路径肯定还是这个
                coreQuery.append(this.toPattern(uri(object), "?p" + fs + "1", "?o" + fs + "1", toObject));
                // 将谓语存放到Array中
                predList.add("?p" + fs + "1");
                // tmpdist = dist1,如果A到middle的距离是3
                for (int x = 1; x < tmpdist; x++) {
                    String s = "?o" + fs + "" + x;
                    String p = "?p" + fs + "" + (x + 1);
                    // 将 谓语、宾语放入Array
                    objList.add(s);
                    predList.add(p);
                    // 如果等于A到middle的距离了，终点就是middle了
                    if ((x + 1) == tmpdist) {
                        coreQuery.append(toPattern(s, p, "?middle", toObject));
                    } else {
                        coreQuery.append(toPattern(s, p, "?o" + fs + "" + (x + 1), toObject));
                    }
                }
                vars.put("pred", predList);
                vars.put("obj", objList);
            }
            twice++;
            fs = "s";
            tmpdist = dist2;
            object = second;

        }//end while
        /* 将生成的
         first p1 o1 .
         o1 p2 o2 .
         o2 p3 o3 .
         o3 p4 middle.
         second ps1 os1
         os1 ps2 os2
         os2 ps3 middle.
         拼接到 select * where {}
         */
        return completeQuery(coreQuery.toString(), options, vars, endPoint, questionType);
    }

    /**
     * 给传来的参数加上<>,变为IRI形式
     *
     * @param uri
     * @return
     */
    private String uri(String uri) {
        return "<" + uri + ">";
    }

    /**
     * 拼接sparql核心查询语句，例如添加prefixes等
     * 给传来的 o1 pf1 of2. of2 pf2 of3. 这些陈述拼接select * where{ } limit 10
     *
     * @param coreQuery
     * @return
     */
    private String completeQuery(String coreQuery, Map options, Map vars, String endPoint, Integer questionType) {
        String constructProject = coreQuery;
        if (questionType == 3) {
            constructProject += " ?end <http://end> <http://end>";
        }
        String completeQuery = "";
        completeQuery += "construct {" + constructProject + "} WHERE { " +
                " SERVICE SILENT <" + endPoint + "> { " + "\n";
        completeQuery += coreQuery + "\n";
        completeQuery += this.generateFilter(options, vars) + "\n";
        String limit = "";
        if (options.containsKey("limit")) {
            limit = " LIMIT " + options.get("limit");
        }
        completeQuery += " } }" + limit;
        // 将换行替换为空格，两个空格替换为1个空格
        completeQuery = completeQuery.replace("\n", " ");
        String old = "";
        while (old != completeQuery) {
            old = completeQuery;
            completeQuery = completeQuery.replace("  ", " ");
        }
        return completeQuery;
    }

    /**
     * 反转 s p o 顺序 为 o p s ,为true不反转，false反转
     * Helper function to reverse the order
     */
    private String toPattern(String s, String p, String o, Boolean toObject) {
        if (toObject) {
            return s + " " + p + " " + o + " . \n";
        } else {
            return o + " " + p + " " + s + " . \n";
        }
    }

    /**
     * 过滤器，应该是过滤一些忽略的节点，拼接filter等
     **/
    private String generateFilter(Map options, Map vars) {
        List filterterms = new ArrayList();
        List<String> predList = (List) vars.get("pred");
        for (String pred : predList) {
            if (options != null && options.containsKey("ignoredProperties") && options.containsKey("ignoredProperties")
                    && options.containsKey("ignoredProperties") && options.containsKey("ignoredProperties")) {
                List<String> ignoredPropertiesList = (List<String>) options.get("ignoredProperties");
                if (ignoredPropertiesList != null && ignoredPropertiesList.size() > 0) {
                    for (String ignored : ignoredPropertiesList) {
                        filterterms.add(pred + " != " + uri(ignored) + " ");
                    }
                }
            }
        }
        List<String> objList = null;
        if (vars.containsKey("obj")) {
            objList = (List<String>) vars.get("obj");
            if (objList != null && !objList.isEmpty()) {
                for (String obj : objList) {
                    // ignore literals
                    filterterms.add("!isLiteral(" + obj + ")");
                    // ignore objects
                    if (options != null && options.containsKey("ignoredProperties") && options.containsKey("ignoredProperties")
                            && options.containsKey("ignoredProperties") && options.containsKey("ignoredProperties")) {
                        List<String> ignoredObjectsList = (List<String>) options.get("'ignoredObjects'");
                        if (null != ignoredObjectsList && !ignoredObjectsList.isEmpty()) {
                            for (String ignored2 : ignoredObjectsList) {
                                filterterms.add(obj + " != ' + uri(ignored2) + ' ");
                            }
                        }

                    }
                    if (options != null && options.containsKey("avoidCycles")) {
                        // object variables should not be the same as object1 or object2
                        Integer avoidCycles = (Integer) options.get("avoidCycles");
                        if (avoidCycles > 0) {
                            String object1 = (String) options.get("object1");
                            String object2 = (String) options.get("object2");
                            filterterms.add(obj + " != " + uri(object1) + ' ');
                            filterterms.add(obj + " != " + uri(object2) + ' ');
                        }
                        // object variables should not be the same as any other objectvariables
                        if (avoidCycles > 1) {
                            for (String otherObj : objList) {
                                if (obj != otherObj) {
                                    filterterms.add(obj + " != " + otherObj + " ");
                                }
                            }
                        }
                    }
                }
            }
        }
        if (filterterms.size() == 0) {
            return "";
        }
        return "FILTER " + expandTerms(filterterms, "&&") + ". ";
    }


    /**
     * puts bracket around the (filterterms) and concatenates them with &&
     */
    private String expandTerms(List terms, String operator) {
        String result = "";
        for (int x = 0; x < terms.size(); x++) {
            result += "(" + terms.get(x) + ")";
            result += (x + 1 == terms.size()) ? "" : " " + operator + " ";
            result += "\n";
        }
        return "(" + result + ")";
    }

}
