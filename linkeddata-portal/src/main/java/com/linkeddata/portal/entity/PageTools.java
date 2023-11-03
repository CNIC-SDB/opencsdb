package com.linkeddata.portal.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 列表 实体类
 *
 * @author wangzhiliang
 */
@Data
@ApiModel(value = "分页展示数据")
public class PageTools<T> {
    @ApiModelProperty("每页显示的条数")
    private Integer initSize = 5;
    @ApiModelProperty("总共有多少条数据")
    private Integer countNum;
    @ApiModelProperty("当前是多少页")
    private Integer currentPage;
    @ApiModelProperty("总共有多少页")
    private Integer countPage;
    @ApiModelProperty("上一页")
    private Integer prePage;
    @ApiModelProperty("下一页")
    private Integer nextPage;
    @ApiModelProperty("页面数据")
    private T pageList;

    /**
     * 无参构造
     */
    public PageTools() {
    }

    public PageTools(Integer initSize, Integer countNum, Integer currentPage) {
        this.initSize = initSize;
        this.countNum = countNum;
        this.currentPage = currentPage;
        // 1 5 / 5 2 6 / 5
        int num = this.countNum / this.initSize;
        // 如果总条数除以每页显示的条数等于0 ,则为两个数的商否则商 + 1
        this.countPage = this.countNum % this.initSize == 0 ? num : num + 1;
        // 如果当前页小于等于1 , 前一页就等于1 ,否则 当前页 - 1
        this.prePage = this.currentPage <= 1 ? 1 : this.currentPage - 1;
        // 如果当前页大于等于总页码, 下一页等于总页面 ,否则 当前页 + 1
        this.nextPage = this.currentPage >= this.countPage ? this.countPage : this.currentPage + 1;
    }

    public PageTools(Integer initSize, Integer countNum, Integer currentPage, T pageList) {
        this.initSize = initSize;
        this.countNum = countNum;
        this.currentPage = currentPage;
        this.pageList = pageList;
        // 1 5 / 5 2 6 / 5
        int num = this.countNum / this.initSize;
        // 如果总条数除以每页显示的条数等于0 ,则为两个数的商否则商 + 1
        this.countPage = this.countNum % this.initSize == 0 ? num : num + 1;
        // 如果当前页小于等于1 , 前一页就等于1 ,否则 当前页 - 1
        this.prePage = this.currentPage <= 1 ? 1 : this.currentPage - 1;
        // 如果当前页大于等于总页码, 下一页等于总页面 ,否则 当前页 + 1
        this.nextPage = this.currentPage >= this.countPage ? this.countPage : this.currentPage + 1;
    }

    /**
     * 冲在构造方法 回传 page list
     */
    public PageTools(T pageList) {
        this.pageList = pageList;
    }

}
