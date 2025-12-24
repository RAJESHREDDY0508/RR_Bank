import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Account } from '../types';

interface AccountState {
  accounts: Account[];
  selectedAccount: Account | null;
  loading: boolean;
  error: string | null;
  totalCount: number;
}

const initialState: AccountState = {
  accounts: [],
  selectedAccount: null,
  loading: false,
  error: null,
  totalCount: 0,
};

const accountSlice = createSlice({
  name: 'accounts',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setAccounts: (state, action: PayloadAction<{ accounts: Account[]; totalCount: number }>) => {
      state.accounts = action.payload.accounts;
      state.totalCount = action.payload.totalCount;
      state.loading = false;
      state.error = null;
    },
    setSelectedAccount: (state, action: PayloadAction<Account | null>) => {
      state.selectedAccount = action.payload;
    },
    updateAccountStatus: (state, action: PayloadAction<{ accountNumber: string; status: string }>) => {
      const account = state.accounts.find(a => a.accountNumber === action.payload.accountNumber);
      if (account) {
        account.status = action.payload.status;
      }
      if (state.selectedAccount?.accountNumber === action.payload.accountNumber) {
        state.selectedAccount.status = action.payload.status;
      }
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
      state.loading = false;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
});

export const {
  setLoading,
  setAccounts,
  setSelectedAccount,
  updateAccountStatus,
  setError,
  clearError,
} = accountSlice.actions;

export default accountSlice.reducer;
