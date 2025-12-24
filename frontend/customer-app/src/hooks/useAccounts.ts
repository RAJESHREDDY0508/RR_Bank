import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './useRedux';
import {
  fetchAccounts,
  fetchAccountById,
  depositFunds,
  withdrawFunds,
  setSelectedAccount,
  clearSelectedAccount,
  clearError,
} from '../store/accountSlice';
import { DepositRequest, WithdrawRequest } from '../types';

export const useAccounts = () => {
  const dispatch = useAppDispatch();
  const { accounts, selectedAccount, loading, error, totalBalance } = useAppSelector(
    (state) => state.accounts
  );

  const loadAccounts = async () => {
    try {
      await dispatch(fetchAccounts()).unwrap();
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const loadAccountById = async (accountNumber: string) => {
    try {
      await dispatch(fetchAccountById(accountNumber)).unwrap();
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const deposit = async (data: DepositRequest) => {
    try {
      await dispatch(depositFunds(data)).unwrap();
      return true;
    } catch (err) {
      return false;
    }
  };

  const withdraw = async (data: WithdrawRequest) => {
    try {
      await dispatch(withdrawFunds(data)).unwrap();
      return true;
    } catch (err) {
      return false;
    }
  };

  const selectAccount = (account: any) => {
    dispatch(setSelectedAccount(account));
  };

  const clearAccount = () => {
    dispatch(clearSelectedAccount());
  };

  const clearAccountError = () => {
    dispatch(clearError());
  };

  // Auto-load accounts on mount
  useEffect(() => {
    if (accounts.length === 0) {
      loadAccounts();
    }
  }, []);

  return {
    accounts,
    selectedAccount,
    loading,
    error,
    totalBalance,
    loadAccounts,
    loadAccountById,
    deposit,
    withdraw,
    selectAccount,
    clearAccount,
    clearError: clearAccountError,
  };
};
