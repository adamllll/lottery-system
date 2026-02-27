package org.adam.lotterysystem.dao.mapper;

import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityPrizeMapper {

    @Insert("<script>" +
            "INSERT INTO activity_prize(activity_id, prize_id, prize_amount, prize_tiers, status) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.activityId}, #{item.prizeId}, #{item.prizeAmount}, #{item.prizeTiers}, #{item.status})" +
            "</foreach>" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchInsert(@Param("items") List<ActivityPrizeDO> activityPirzeDOList);
}
