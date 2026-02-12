import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { AuditLog } from '../types';

interface AuditLogState {
  logs: AuditLog[];
  selectedLog: AuditLog | null;
  loading: boolean;
  error: string | null;
  totalCount: number;
}

const initialState: AuditLogState = {
  logs: [],
  selectedLog: null,
  loading: false,
  error: null,
  totalCount: 0,
};

const auditLogSlice = createSlice({
  name: 'auditLogs',
  initialState,
  reducers: {
    setLogs: (state, action: PayloadAction<{ logs: AuditLog[]; total: number }>) => {
      state.logs = action.payload.logs;
      state.totalCount = action.payload.total;
      state.loading = false;
      state.error = null;
    },
    setSelectedLog: (state, action: PayloadAction<AuditLog | null>) => {
      state.selectedLog = action.payload;
    },
    addLog: (state, action: PayloadAction<AuditLog>) => {
      state.logs.unshift(action.payload);
      state.totalCount += 1;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
      state.loading = false;
    },
    clearLogs: (state) => {
      state.logs = [];
      state.selectedLog = null;
      state.loading = false;
      state.error = null;
      state.totalCount = 0;
    },
  },
});

export const { setLogs, setSelectedLog, addLog, setLoading, setError, clearLogs } = auditLogSlice.actions;
export default auditLogSlice.reducer;
