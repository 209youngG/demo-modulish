package com.demomodulish.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
    /**
     * 특정 상품의 재고를 조회하며 비관적 락(Pessimistic Write Lock)을 획득합니다.
     * <p>
     * 동시성 이슈를 방지하기 위해 조회 시점에 Row Lock을 걸어 다른 트랜잭션의 접근을 막습니다.
     * 유통기한이 임박한 순서(FIFO)로 정렬하여 가져오며, 재고가 0인 항목은 제외합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryItem b WHERE b.productId = :productId AND b.quantity > 0 ORDER BY b.expirationDate ASC")
    List<InventoryItem> findAllByProductIdWithLock(String productId);

    List<InventoryItem> findAllByProductId(String productId);
}
