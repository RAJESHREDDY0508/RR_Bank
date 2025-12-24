import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { AuditLog } from '../types';

interface AuditLogState {
  logs: AuditLog[];
  loading: boolean;
  error: string | null;
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

const initialState: AuditLogState = {
  logs: [],
  loading: false,
  error: null,
  totalCount: 0,
  currentPage: 1,
  pageSize: 20,
};

const auditLogSlice = createSlice({
  name: 'auditLogs',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setLogs: (state, action: PayloadAction<{ logs: AuditLog[]; totalCount: number }>) => {
      state.logs = action.payload.logs;
      state.totalCount = action.payload.totalCount;
      state.loading = false;
      state.error = null;
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
  setLogs,
  setError,
  setPage,
  clearError,
} = auditLogSlice.actions;

export default auditLogSlice.reducer;
