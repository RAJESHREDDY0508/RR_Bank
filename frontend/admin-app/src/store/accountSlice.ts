import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Account } from '../types';

interface AccountState {
  accounts: Account[];
  selectedAccount: Account | null;
  loading: boolean;
  error: string | null;
}

const initialState: AccountState = {
  accounts: [],
  selectedAccount: null,
  loading: false,
  error: null,
};

const accountSlice = createSlice({
  name: 'accounts',
  initialState,
  reducers: {
    setAccounts: (state, action: PayloadAction<Account[]>) => {
      state.accounts = action.payload;
      state.loading = false;
      state.error = null;
    },
    setSelectedAccount: (state, action: PayloadAction<Account | null>) => {
      state.selectedAccount = action.payload;
    },
    updateAccount: (state, action: PayloadAction<{ accountNumber: string; updates: Partial<Account> }>) => {
      const account = state.accounts.find((a: Account) => a.accountNumber === action.payload.accountNumber);
      if (account) {
        Object.assign(account, action.payload.updates);
      }
      if (state.selectedAccount?.accountNumber === action.payload.accountNumber) {
        Object.assign(state.selectedAccount, action.payload.updates);
      }
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
      state.loading = false;
    },
    clearAccounts: (state) => {
      state.accounts = [];
      state.selectedAccount = null;
      state.loading = false;
      state.error = null;
    },
  },
});

export const { setAccounts, setSelectedAccount, updateAccount, setLoading, setError, clearAccounts } = accountSlice.actions;
export default accountSlice.reducer;
