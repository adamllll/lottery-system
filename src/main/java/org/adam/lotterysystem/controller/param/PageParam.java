package org.adam.lotterysystem.controller.param;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageParam implements Serializable {

    // 当前页
    private Integer currentPage = 1; // 默认页码为1

    // 每页记录数
    private Integer pageSize = 10; // 默认每页10条记录

    // 分页查询的偏移量
    public Integer offset() {
        return (currentPage - 1) * pageSize;
    }
}
