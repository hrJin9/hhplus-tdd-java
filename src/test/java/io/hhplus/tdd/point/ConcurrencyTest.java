package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConcurrencyTest {
    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setup() {
        long id = 1L;
        long amount = 100L;
        userPointTable.insertOrUpdate(id, amount);
    }

    @Test
    @DisplayName("동시성 환경 테스트")
    void 같은_유저가_한번에_충전요청() throws InterruptedException {
        // given
        long id = 1L;
        long amount = 100L;
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(id, amount);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        // 스레드 완료 대기
        countDownLatch.await();

        // then
        UserPoint userPoint = pointService.selectPointById(id);
        assertThat(userPoint.point()).isEqualTo(100L + amount * threadCount);
    }

}
