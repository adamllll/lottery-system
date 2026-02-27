package org.adam.lotterysystem.dao.mapper;

import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface ActivityMapper {

    @Insert("insert into activity(activity_name, description, status) " +
            "values(#{activityName}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ActivityDO activityDO);
}
