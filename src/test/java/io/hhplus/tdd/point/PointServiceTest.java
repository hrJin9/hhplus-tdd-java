package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {
    @InjectMocks
    private PointService pointService;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    @Test
    @DisplayName("포인트 내역이 존재하지 않는 아이디로 조회할 경우 0포인트를 반환한다.")
    void selectPoint_returnsNewPoint() {
        // given
        long id = 1L;
        given(userPointTable.selectById(id))
                .willReturn(UserPoint.empty(id));

        // when
        UserPoint userPoint = pointService.selectPointById(id);

        // then
        assertThat(userPoint.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("존재하는 id로 조회하면 포인트 내역을 반환한다.")
    void selectPoint_returnsPoint() {
        // given
        long id = 1L;
        long amount = 10L;
        UserPoint savedUserPoint = new UserPoint(id, amount, System.currentTimeMillis());

        given(userPointTable.selectById(id))
                .willReturn(savedUserPoint);

        // when
        UserPoint userPoint = pointService.selectPointById(id);

        // then
        assertThat(userPoint.point()).isEqualTo(savedUserPoint.point());
    }

    @Test
    @DisplayName("충전 시 최대 충전 금액을 초과한 경우 에러를 던진다.")
    void chargePoint_throwError() {
        // given
        long id = 1L;
        long savedPoint = 900000000L;
        long amount = 600000000L;

        UserPoint savedUserPoint = new UserPoint(id, savedPoint, System.currentTimeMillis());

        given(userPointTable.selectById(id))
                .willReturn(savedUserPoint);

        // when, then
        assertThatThrownBy(() -> pointService.chargePoint(id, amount))
                .hasMessage("최대 충전 가능 금액을 초과하였습니다.");
    }

    @Test
    @DisplayName("충전이 정상적으로 작동한다.")
    void chargePoint_returnsChargedPoint() {
        // given
        long id = 1L;
        long savedPoint = 10L;
        long amount = 100L;
        UserPoint savedUserPoint = new UserPoint(id, savedPoint, System.currentTimeMillis());
        UserPoint savingUserPoint = new UserPoint(id, savedPoint + amount, System.currentTimeMillis());

        given(userPointTable.selectById(id))
                .willReturn(savedUserPoint);
        given(userPointTable.insertOrUpdate(id, savedPoint + amount))
                .willReturn(savingUserPoint);

        // when
        UserPoint userPoint = pointService.chargePoint(id, amount);

        // then
        assertThat(userPoint.point()).isEqualTo(savedPoint + amount);
    }

    @Test
    @DisplayName("포인트 사용 시 포인트가 부족할 경우 에러를 던진다.")
    void usePoint_throwError() {
        // given
        long id = 1L;
        long savedPoint = 10L;
        long usingPoint = 900L;

        UserPoint savedUserPoint = new UserPoint(id, savedPoint, System.currentTimeMillis());

        given(userPointTable.selectById(id))
                .willReturn(savedUserPoint);

        // when, then
        assertThatThrownBy(() -> pointService.usePoint(id, usingPoint))
                .hasMessage("잔고가 부족합니다.");
    }

    @Test
    @DisplayName("포인트가 정상적으로 사용된다.")
    void usePoint_returnsUserPoint() {
        // given
        long id = 1L;
        long savedPoint = 10L;
        long amount = 5L;

        UserPoint savedUserPoint = new UserPoint(id, savedPoint, System.currentTimeMillis());
        UserPoint savingUserPoint = new UserPoint(id, savedPoint - amount, System.currentTimeMillis());

        given(userPointTable.selectById(id))
                .willReturn(savedUserPoint);
        given(userPointTable.insertOrUpdate(id, savedPoint - amount))
                .willReturn(savingUserPoint);
        // when
        UserPoint userPoint = pointService.usePoint(id, amount);

        // then
        assertThat(userPoint.point()).isEqualTo(savedPoint - amount);
    }

    @Test
    @DisplayName("포인트 내역이 정상적으로 조회된다.")
    void asdf() {
        // given
        long id = 1L;
        long userId = 1L;

        PointHistory pointHistory1 = new PointHistory(id, userId, 10L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory pointHistory2 = new PointHistory(id++, userId, 5L, TransactionType.USE, System.currentTimeMillis());

        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(pointHistory1, pointHistory2));

        // when
        List<PointHistory> pointHistories = pointService.selectPointHistoriesById(userId);

        // then
        assertThat(pointHistories).containsAll(List.of(pointHistory1, pointHistory2));
    }
}
