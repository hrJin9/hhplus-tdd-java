package io.hhplus.tdd.point;

import io.hhplus.tdd.ApiException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ReentrantLock lock = new ReentrantLock();
    private static final Long MAX_POINT = 1000000000L; // 최대 잔고 설정

    /**
     * 특정 유저의 포인트를 조회한다.
     * @param id
     * @return
     */
    public UserPoint selectPointById(long id) {
        UserPoint userPoint = userPointTable.selectById(id);
        return userPoint;
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회한다.
     * @param id
     * @return
     */
    public List<PointHistory> selectPointHistoriesById(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * 특정 유저의 포인트를 충전한다.
     * @param id
     * @param amount
     * @return
     */

    public UserPoint chargePoint(long id, long amount) {
        lock.lock();
        try {
            log.info("amount = {}", amount);

            // 기존의 충전 내역을 가져온다.
            UserPoint userPoint = userPointTable.selectById(id);

            // 최대 충전 금액 여부 확인
            long savingPoint = userPoint.point() + amount;
            if(savingPoint > MAX_POINT) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "최대 충전 가능 금액을 초과하였습니다.");
            }

            // 충전 결과를 저장
            UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, savingPoint);

            // 충전 내역을 저장
            pointHistoryTable.insert(savedUserPoint.id(), savedUserPoint.point(), TransactionType.CHARGE, savedUserPoint.updateMillis());

            return savedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 특정 유저의 포인트를 사용한다.
     * @param id
     * @param amount
     * @return
     */
    public UserPoint usePoint(long id, long amount) {
        lock.lock();
        try {
            log.info("amount = {}", amount);
            // 기존의 충전 내역을 가져온다.
            UserPoint userPoint = userPointTable.selectById(id);

            // 충전된 포인트보다 사용 포인트가 많으면 에러를 던진다.
            if(userPoint.point() < amount) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "잔고가 부족합니다.");
            }

            // 충전한 결과를 저장한다.
            UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, userPoint.point() - amount);

            // 충전 내역을 저장한다.
            pointHistoryTable.insert(savedUserPoint.id(), savedUserPoint.point(), TransactionType.USE, savedUserPoint.updateMillis());

            return savedUserPoint;
        } finally {
            lock.unlock();
        }
    }
}
