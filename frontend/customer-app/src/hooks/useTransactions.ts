import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './useRedux';
import {
  fetchTransactions,
  fetchRecentTransactions,
  fetchTransactionById,
  transferFunds,
  setFilters,
  clearFilters,
  setPage,
  setPageSize,
  clearError,
} from '../store/transactionSlice';
import { TransferRequest, TransactionFilters } from '../types';

export const useTransactions = () => {
  const dispatch = useAppDispatch();
  const {
    transactions,
    recentTransactions,
    selectedTransaction,
    loading,
    error,
    filters,
    pagination,
  } = useAppSelector((state) => state.transactions);

  const loadTransactions = async (accountNumber: string, page?: number, size?: number) => {
    try {
      await dispatch(fetchTransactions({ accountNumber, page, size })).unwrap();
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const loadRecentTransactions = async (accountNumber: string) => {
    try {
      await dispatch(fetchRecentTransactions(accountNumber)).unwrap();
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const loadTransactionById = async (transactionId: string) => {
    try {
      await dispatch(fetchTransactionById(transactionId)).unwrap();
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const transfer = async (data: TransferRequest) => {
    try {
      await dispatch(transferFunds(data)).unwrap();
      return true;
    } catch (err) {
      return false;
    }
  };

  const updateFilters = (newFilters: Partial<TransactionFilters>) => {
    dispatch(setFilters(newFilters));
  };

  const resetFilters = () => {
    dispatch(clearFilters());
  };

  const changePage = (page: number) => {
    dispatch(setPage(page));
  };

  const changePageSize = (size: number) => {
    dispatch(setPageSize(size));
  };

  const clearTransactionError = () => {
    dispatch(clearError());
  };

  return {
    transactions,
    recentTransactions,
    selectedTransaction,
    loading,
    error,
    filters,
    pagination,
    loadTransactions,
    loadRecentTransactions,
    loadTransactionById,
    transfer,
    updateFilters,
    resetFilters,
    changePage,
    changePageSize,
    clearError: clearTransactionError,
  };
};
