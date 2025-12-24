import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Customer } from '../types';

interface CustomerState {
  customers: Customer[];
  selectedCustomer: Customer | null;
  loading: boolean;
  error: string | null;
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

const initialState: CustomerState = {
  customers: [],
  selectedCustomer: null,
  loading: false,
  error: null,
  totalCount: 0,
  currentPage: 1,
  pageSize: 10,
};

const customerSlice = createSlice({
  name: 'customers',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setCustomers: (state, action: PayloadAction<{ customers: Customer[]; totalCount: number }>) => {
      state.customers = action.payload.customers;
      state.totalCount = action.payload.totalCount;
      state.loading = false;
      state.error = null;
    },
    setSelectedCustomer: (state, action: PayloadAction<Customer | null>) => {
      state.selectedCustomer = action.payload;
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
      state.loading = false;
    },
    setPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
});

export const {
  setLoading,
  setCustomers,
  setSelectedCustomer,
  setError,
  setPage,
  clearError,
} = customerSlice.actions;

export default customerSlice.reducer;
