package com.demomodulish.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {
}
