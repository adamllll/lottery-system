package org.adam.lotterysystem;

import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JWTUtil;
import org.adam.lotterysystem.controller.DrawPrizeController;
import org.adam.lotterysystem.controller.handler.GlobalExceptionHandler;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.dto.DrawPrizeDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DrawPrizeController.class)
@Import(GlobalExceptionHandler.class)
class DrawPrizeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DrawPrizeService drawPrizeService;

    @Test
    void testDrawPrizeRejectsNonAdminUser() throws Exception {
        String token = JWTUtil.genJwt(Map.of("identity", UserIdentityEnum.NORMAL.name()));

        mockMvc.perform(post("/draw-prize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("user_token", token)
                        .content("{\"activityId\":1,\"prizeId\":18}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("仅管理员可以执行抽奖"));
    }

    @Test
    void testDrawPrizeReturnsProcessingCode() throws Exception {
        Mockito.when(drawPrizeService.executeDraw(any()))
                .thenThrow(new ServiceException(202, "当前奖项正在处理中"));

        String token = JWTUtil.genJwt(Map.of("identity", UserIdentityEnum.ADMIN.name()));

        mockMvc.perform(post("/draw-prize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("user_token", token)
                        .content("{\"activityId\":1,\"prizeId\":18}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.msg").value("当前奖项正在处理中"));
    }

    @Test
    void testDrawPrizeReturnsMappedResultForAdmin() throws Exception {
        DrawPrizeDTO dto = new DrawPrizeDTO();
        dto.setActivityId(1L);
        dto.setPrizeId(18L);
        dto.setPrizeName("手机");
        dto.setPrizeTier(ActivityPrizeTiersEnum.FIRST_PRIZE);
        dto.setWinningTime(new Date());

        DrawPrizeDTO.WinnerDTO winner = new DrawPrizeDTO.WinnerDTO();
        winner.setUserId(43L);
        winner.setUserName("lisi");
        dto.setWinnerList(List.of(winner));

        Mockito.when(drawPrizeService.executeDraw(any())).thenReturn(dto);

        String token = JWTUtil.genJwt(Map.of("identity", UserIdentityEnum.ADMIN.name()));

        mockMvc.perform(post("/draw-prize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("user_token", token)
                        .content("{\"activityId\":1,\"prizeId\":18}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.prizeId").value(18))
                .andExpect(jsonPath("$.data.prizeName").value("手机"))
                .andExpect(jsonPath("$.data.prizeTier").value(ActivityPrizeTiersEnum.FIRST_PRIZE.getMessage()))
                .andExpect(jsonPath("$.data.winnerList[0].userId").value(43))
                .andExpect(jsonPath("$.data.winnerList[0].userName").value("lisi"));
    }
}
