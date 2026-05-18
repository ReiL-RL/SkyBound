package me.reil.skybound.api.bank;

import java.util.UUID;

/**
 * Represents a bank transaction record.
 */
public interface BankTransaction {

    /** Player who performed the transaction. */
    UUID getPlayerId();

    /** Transaction type. */
    TransactionType getType();

    /** Amount. */
    double getAmount();

    /** Timestamp. */
    long getTimestamp();

    /** Optional description. */
    String getDescription();

    enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        UPGRADE_PURCHASE,
        BOOSTER_PURCHASE
    }
}
