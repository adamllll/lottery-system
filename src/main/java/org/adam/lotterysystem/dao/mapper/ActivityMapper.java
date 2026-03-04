package org.adam.lotterysystem.dao.mapper;

import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityMapper {

    @Insert("insert into activity(activity_name, description, status) " +
            "values(#{activityName}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ActivityDO activityDO);

    @Select("select count(1) from activity")
    int count(PageParam param);

    @Select("select * from activity order by id desc limit #{offset}, #{pageSize}")
    List<ActivityDO> selectActivityList(
            @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    @Select("select * from activity where id = #{activityId}")
    ActivityDO selectById(@Param("activityId") Long activityId);
}
