package com.RRBank.banking.exception;

/**
 * Insufficient Funds Exception
 * Thrown when account has insufficient balance for withdrawal/transfer
 */
public class InsufficientFundsException extends BusinessException {
    
    public InsufficientFundsException(String accountNumber, double balance, double requestedAmount) {
        super(String.format("Insufficient funds in account %s. Balance: %.2f, Requested: %.2f",
                accountNumber, balance, requestedAmount));
    }
    
    public InsufficientFundsException(String message) {
        super(message);
    }
}
