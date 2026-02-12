import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Customer } from '../types';

interface CustomerState {
  customers: Customer[];
  selectedCustomer: Customer | null;
  loading: boolean;
  error: string | null;
  totalCount: number;
}

const initialState: CustomerState = {
  customers: [],
  selectedCustomer: null,
  loading: false,
  error: null,
  totalCount: 0,
};

const customerSlice = createSlice({
  name: 'customers',
  initialState,
  reducers: {
    setCustomers: (state, action: PayloadAction<{ customers: Customer[]; total: number }>) => {
      state.customers = action.payload.customers;
      state.totalCount = action.payload.total;
      state.loading = false;
      state.error = null;
    },
    setSelectedCustomer: (state, action: PayloadAction<Customer | null>) => {
      state.selectedCustomer = action.payload;
    },
    updateCustomer: (state, action: PayloadAction<{ id: string; updates: Partial<Customer> }>) => {
      const customer = state.customers.find((c: Customer) => c.id === action.payload.id);
      if (customer) {
        Object.assign(customer, action.payload.updates);
      }
      if (state.selectedCustomer?.id === action.payload.id) {
        Object.assign(state.selectedCustomer, action.payload.updates);
      }
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
      state.loading = false;
    },
    clearCustomers: (state) => {
      state.customers = [];
      state.selectedCustomer = null;
      state.loading = false;
      state.error = null;
      state.totalCount = 0;
    },
  },
});

export const { setCustomers, setSelectedCustomer, updateCustomer, setLoading, setError, clearCustomers } = customerSlice.actions;
export default customerSlice.reducer;
