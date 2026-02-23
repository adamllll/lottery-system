package org.adam.lotterysystem.dao.mapper;

import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PrizeMapper {

    @Insert("insert into prize(name, image_url, price, description, gmt_create, gmt_modified) " +
            "values(#{name}, #{imageUrl}, #{price}, #{description}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PrizeDO prizeDO);
}
