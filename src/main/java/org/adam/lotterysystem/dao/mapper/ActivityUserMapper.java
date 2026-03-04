package org.adam.lotterysystem.dao.mapper;

import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityUserMapper {

    @Insert("<script>" +
            "INSERT INTO activity_user(activity_id, user_id, user_name, status) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.activityId}, #{item.userId}, #{item.userName}, #{item.status})" +
            "</foreach>" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchInsert(@Param("items") List<ActivityUserDO> activityUserDOList);

    @Select("select * from activity_user where activity_id = #{activityId}")
    List<ActivityUserDO> selectByActivityId(@Param("activityId") Long activityId);
}
