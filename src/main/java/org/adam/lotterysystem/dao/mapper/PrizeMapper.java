package org.adam.lotterysystem.dao.mapper;

import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrizeMapper {

    @Insert("insert into prize(name, image_url, price, description, gmt_create, gmt_modified) " +
            "values(#{name}, #{imageUrl}, #{price}, #{description}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PrizeDO prizeDO);

    @Select("select count(1) from prize")
    int countPrizes();

    @Select("select * from prize order by id desc limit #{offset}, #{pageSize}")
    List<PrizeDO> selectPrizeList(
            @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
}
