import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { accountsAPI } from '../api/accounts';
import { Account, DepositRequest, WithdrawRequest } from '../types';

interface AccountState {
  accounts: Account[];
  selectedAccount: Account | null;
  loading: boolean;
  error: string | null;
  totalBalance: number;
}

const initialState: AccountState = {
  accounts: [],
  selectedAccount: null,
  loading: false,
  error: null,
  totalBalance: 0,
};

// Async thunks
export const fetchAccounts = createAsyncThunk(
  'accounts/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      const response = await accountsAPI.getAllAccounts();
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch accounts');
    }
  }
);

export const fetchAccountById = createAsyncThunk(
  'accounts/fetchById',
  async (accountNumber: string, { rejectWithValue }) => {
    try {
      const response = await accountsAPI.getAccountById(accountNumber);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch account');
    }
  }
);

export const depositFunds = createAsyncThunk(
  'accounts/deposit',
  async ({ accountNumber, amount, description }: DepositRequest, { rejectWithValue }) => {
    try {
      const response = await accountsAPI.deposit(accountNumber, amount, description);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Deposit failed');
    }
  }
);

export const withdrawFunds = createAsyncThunk(
  'accounts/withdraw',
  async ({ accountNumber, amount, description }: WithdrawRequest, { rejectWithValue }) => {
    try {
      const response = await accountsAPI.withdraw(accountNumber, amount, description);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Withdrawal failed');
    }
  }
);

const accountSlice = createSlice({
  name: 'accounts',
  initialState,
  reducers: {
    setSelectedAccount: (state, action: PayloadAction<Account>) => {
      state.selectedAccount = action.payload;
    },
    clearSelectedAccount: (state) => {
      state.selectedAccount = null;
    },
    updateAccountBalance: (state, action: PayloadAction<{ accountNumber: string; balance: number }>) => {
      const account = state.accounts.find(acc => acc.accountNumber === action.payload.accountNumber);
      if (account) {
        account.balance = action.payload.balance;
      }
      if (state.selectedAccount?.accountNumber === action.payload.accountNumber) {
        state.selectedAccount.balance = action.payload.balance;
      }
      // Recalculate total balance
      state.totalBalance = state.accounts.reduce((sum, acc) => sum + acc.balance, 0);
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch all accounts
    builder
      .addCase(fetchAccounts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchAccounts.fulfilled, (state, action) => {
        state.loading = false;
        state.accounts = action.payload;
        state.totalBalance = action.payload.reduce((sum: number, acc: Account) => sum + acc.balance, 0);
      })
      .addCase(fetchAccounts.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Fetch account by ID
      .addCase(fetchAccountById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchAccountById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedAccount = action.payload;
      })
      .addCase(fetchAccountById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Deposit funds
      .addCase(depositFunds.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(depositFunds.fulfilled, (state, action) => {
        state.loading = false;
        // Update the account balance in the list
        const accountIndex = state.accounts.findIndex(
          acc => acc.accountNumber === action.payload.accountNumber
        );
        if (accountIndex !== -1) {
          state.accounts[accountIndex] = action.payload;
        }
        if (state.selectedAccount?.accountNumber === action.payload.accountNumber) {
          state.selectedAccount = action.payload;
        }
        state.totalBalance = state.accounts.reduce((sum, acc) => sum + acc.balance, 0);
      })
      .addCase(depositFunds.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Withdraw funds
      .addCase(withdrawFunds.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(withdrawFunds.fulfilled, (state, action) => {
        state.loading = false;
        // Update the account balance in the list
        const accountIndex = state.accounts.findIndex(
          acc => acc.accountNumber === action.payload.accountNumber
        );
        if (accountIndex !== -1) {
          state.accounts[accountIndex] = action.payload;
        }
        if (state.selectedAccount?.accountNumber === action.payload.accountNumber) {
          state.selectedAccount = action.payload;
        }
        state.totalBalance = state.accounts.reduce((sum, acc) => sum + acc.balance, 0);
      })
      .addCase(withdrawFunds.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setSelectedAccount, clearSelectedAccount, updateAccountBalance, clearError } = accountSlice.actions;
export default accountSlice.reducer;
