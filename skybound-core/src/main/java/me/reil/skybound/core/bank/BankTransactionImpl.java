package me.reil.skybound.core.bank;

import me.reil.skybound.api.bank.BankTransaction;

import java.util.UUID;

public final class BankTransactionImpl implements BankTransaction {

    private final UUID playerId;
    private final TransactionType type;
    private final double amount;
    private final long timestamp;
    private final String description;

    public BankTransactionImpl(UUID playerId, TransactionType type, double amount, long timestamp, String description) {
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description;
    }

    @Override public UUID getPlayerId() { return playerId; }
    @Override public TransactionType getType() { return type; }
    @Override public double getAmount() { return amount; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public String getDescription() { return description; }
}
