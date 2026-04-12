package org.adam.lotterysystem.dao.mapper;

import jakarta.validation.constraints.NotNull;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.apache.ibatis.annotations.*;

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

    @Select("select * from activity_prize where activity_id = #{activityId}")
    List<ActivityPrizeDO> selectByActivityId(@Param("activityId") Long activityId);

    @Select("select * from activity_prize where activity_id = #{activityId} and prize_id = #{prizeId}")
    ActivityPrizeDO selectByActivityPrizeId(@Param("activityId") Long activityId, @Param("prizeId") Long prizeId);

    @Select("select * from activity_prize where activity_id = #{activityId} and prize_id = #{prizeId} for update")
    ActivityPrizeDO selectByActivityPrizeIdForUpdate(@Param("activityId") Long activityId,
                                                     @Param("prizeId") Long prizeId);

    @Select("select count(1) from activity_prize where activity_id = #{activityId} and status = #{status}")
    int countPrize(@Param("activityId") Long activityId,
                   @Param("status") String status);

    @Update("update activity_prize set status = #{status} where activity_id = #{activityId} and prize_id = #{prizeId}")
    void updateStatus(@Param("activityId") Long activityId,
                      @Param("prizeId") Long prizeId,
                      @Param("status") String status);
}
