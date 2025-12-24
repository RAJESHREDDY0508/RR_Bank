import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { transactionsAPI } from '../api/transactions';
import { Transaction, TransferRequest, TransactionFilters } from '../types';

interface TransactionState {
  transactions: Transaction[];
  recentTransactions: Transaction[];
  selectedTransaction: Transaction | null;
  loading: boolean;
  error: string | null;
  filters: TransactionFilters;
  pagination: {
    page: number;
    pageSize: number;
    totalPages: number;
    totalElements: number;
  };
}

const initialState: TransactionState = {
  transactions: [],
  recentTransactions: [],
  selectedTransaction: null,
  loading: false,
  error: null,
  filters: {
    startDate: null,
    endDate: null,
    transactionType: null,
    minAmount: null,
    maxAmount: null,
    searchTerm: '',
  },
  pagination: {
    page: 0,
    pageSize: 10,
    totalPages: 0,
    totalElements: 0,
  },
};

// Async thunks
export const fetchTransactions = createAsyncThunk(
  'transactions/fetchAll',
  async (
    { accountNumber, page = 0, size = 10 }: { accountNumber: string; page?: number; size?: number },
    { rejectWithValue }
  ) => {
    try {
      const response = await transactionsAPI.getAccountTransactions(accountNumber, page, size);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch transactions');
    }
  }
);

export const fetchRecentTransactions = createAsyncThunk(
  'transactions/fetchRecent',
  async (accountNumber: string, { rejectWithValue }) => {
    try {
      const response = await transactionsAPI.getAccountTransactions(accountNumber, 0, 5);
      return response.data.content;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch recent transactions');
    }
  }
);

export const fetchTransactionById = createAsyncThunk(
  'transactions/fetchById',
  async (transactionId: string, { rejectWithValue }) => {
    try {
      const response = await transactionsAPI.getTransactionById(transactionId);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch transaction');
    }
  }
);

export const transferFunds = createAsyncThunk(
  'transactions/transfer',
  async (transferData: TransferRequest, { rejectWithValue }) => {
    try {
      const response = await transactionsAPI.transfer(transferData);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Transfer failed');
    }
  }
);

const transactionSlice = createSlice({
  name: 'transactions',
  initialState,
  reducers: {
    setFilters: (state, action: PayloadAction<Partial<TransactionFilters>>) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearFilters: (state) => {
      state.filters = initialState.filters;
    },
    setPage: (state, action: PayloadAction<number>) => {
      state.pagination.page = action.payload;
    },
    setPageSize: (state, action: PayloadAction<number>) => {
      state.pagination.pageSize = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch transactions
    builder
      .addCase(fetchTransactions.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchTransactions.fulfilled, (state, action) => {
        state.loading = false;
        state.transactions = action.payload.content;
        state.pagination = {
          page: action.payload.number,
          pageSize: action.payload.size,
          totalPages: action.payload.totalPages,
          totalElements: action.payload.totalElements,
        };
      })
      .addCase(fetchTransactions.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Fetch recent transactions
      .addCase(fetchRecentTransactions.fulfilled, (state, action) => {
        state.recentTransactions = action.payload;
      })
      // Fetch transaction by ID
      .addCase(fetchTransactionById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchTransactionById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedTransaction = action.payload;
      })
      .addCase(fetchTransactionById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Transfer funds
      .addCase(transferFunds.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(transferFunds.fulfilled, (state, action) => {
        state.loading = false;
        // Add the new transaction to the list
        state.transactions.unshift(action.payload);
        state.recentTransactions.unshift(action.payload);
        if (state.recentTransactions.length > 5) {
          state.recentTransactions.pop();
        }
      })
      .addCase(transferFunds.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setFilters, clearFilters, setPage, setPageSize, clearError } = transactionSlice.actions;
export default transactionSlice.reducer;
