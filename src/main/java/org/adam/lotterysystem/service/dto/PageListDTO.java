package org.adam.lotterysystem.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageListDTO<T> {

    private Integer total; // 总记录数

    private List<T> records; // 当前页记录列表

    // 构造函数
    public PageListDTO(Integer total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
