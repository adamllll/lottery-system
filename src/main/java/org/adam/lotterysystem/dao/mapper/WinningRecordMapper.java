package org.adam.lotterysystem.dao.mapper;

import jakarta.validation.constraints.NotNull;
import org.adam.lotterysystem.dao.dataobject.WinningRecordDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WinningRecordMapper {

    @Insert("<script>" +
            "INSERT INTO winning_record(activity_id, activity_name, prize_id, prize_name, prize_tier, winner_id, winner_name, winner_email, winner_phone_number, winning_time) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.activityId}, #{item.activityName}, #{item.prizeId}, #{item.prizeName}, #{item.prizeTier}, #{item.winnerId}, #{item.winnerName}, #{item.winnerEmail}, #{item.winnerPhoneNumber}, #{item.winningTime})" +
            "</foreach>" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchInsert(@Param("items") List<WinningRecordDO> winningRecordDOS);

    @Select("select * from winning_record where activity_id = #{activityId} order by winning_time desc")
    List<WinningRecordDO> selectByActivityId(@Param("activityId") Long activityId);

    @Select("select count(1) from winning_record where activity_id = #{activityId} and prize_id = #{prizeId}")
    int countByAPId(@Param("activityId") Long activityId,
                   @Param("prizeId") Long prizeId);

    // 根据活动 Id 和奖品 Id 删除中奖记录
    @Delete("<script>" +
            "delete from winning_record where activity_id = #{activityId} " +
            "<if test=\"prizeId != null\"> " +
            "and prize_id = #{prizeId} " +
            "</if>" +
            "</script>")
    void deleteByActivityIdAndPrizeId(
            @Param("activityId") Long activityId,
            @Param("prizeId") Long prizeId);

    @Select("<script>" +
            "select * from winning_record where activity_id = #{activityId} " +
            "<if test=\"prizeId != null\"> " +
            "and prize_id = #{prizeId} " +
            "</if>" +
            "order by winning_time desc" +
            "</script>")
    List<WinningRecordDO> selectByActivityIdOrPrizeId(@Param("activityId") Long activityId,
                                                      @Param("prizeId") Long prizeId);
}
