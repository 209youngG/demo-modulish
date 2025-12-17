package com.demomodulish;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
    @Test
    void verifyModularity() {
        ApplicationModules.of(DemoModulishApplication.class).verify();
    }

    @Test
    void verifyModularityAndExistence() {
        // 1. 모듈 전체 구조 분석
        var modules = ApplicationModules.of(DemoModulishApplication.class);

        // 2. [기본] 구조적 검증 (순환참조 등)
        modules.verify();

        // 3. [추가] 특정 모듈이 존재하는지 강제로 확인 (TDD: 이게 없으면 실패함)
        boolean hasOrder = modules.stream()
                .anyMatch(module -> module.getIdentifier().toString().equals("order"));
        boolean hasInventory = modules.stream()
                .anyMatch(module -> module.getIdentifier().toString().equals("inventory"));

        if (!hasOrder || !hasInventory) {
            throw new AssertionError("❌ 필수 모듈(order, inventory)이 아직 생성되지 않았습니다!");
        }
    }
}
