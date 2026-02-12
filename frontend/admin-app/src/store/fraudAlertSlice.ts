import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { FraudAlert } from '../types';

interface FraudAlertState {
  alerts: FraudAlert[];
  selectedAlert: FraudAlert | null;
  loading: boolean;
  error: string | null;
  totalCount: number;
  unreadCount: number;
}

const initialState: FraudAlertState = {
  alerts: [],
  selectedAlert: null,
  loading: false,
  error: null,
  totalCount: 0,
  unreadCount: 0,
};

const fraudAlertSlice = createSlice({
  name: 'fraudAlerts',
  initialState,
  reducers: {
    setAlerts: (state, action: PayloadAction<{ alerts: FraudAlert[]; total: number }>) => {
      state.alerts = action.payload.alerts;
      state.totalCount = action.payload.total;
      state.unreadCount = action.payload.alerts.filter((a: FraudAlert) => a.status === 'PENDING').length;
      state.loading = false;
      state.error = null;
    },
    setSelectedAlert: (state, action: PayloadAction<FraudAlert | null>) => {
      state.selectedAlert = action.payload;
    },
    updateAlert: (state, action: PayloadAction<{ id: string; updates: Partial<FraudAlert> }>) => {
      const alert = state.alerts.find((a: FraudAlert) => a.id === action.payload.id);
      if (alert) {
        Object.assign(alert, action.payload.updates);
      }
      if (state.selectedAlert?.id === action.payload.id) {
        Object.assign(state.selectedAlert, action.payload.updates);
      }
      // Update unread count
      state.unreadCount = state.alerts.filter((a: FraudAlert) => a.status === 'PENDING').length;
    },
    addAlert: (state, action: PayloadAction<FraudAlert>) => {
      state.alerts.unshift(action.payload);
      state.totalCount += 1;
      if (action.payload.status === 'PENDING') {
        state.unreadCount += 1;
      }
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
      state.loading = false;
    },
    clearAlerts: (state) => {
      state.alerts = [];
      state.selectedAlert = null;
      state.loading = false;
      state.error = null;
      state.totalCount = 0;
      state.unreadCount = 0;
    },
  },
});

export const { setAlerts, setSelectedAlert, updateAlert, addAlert, setLoading, setError, clearAlerts } = fraudAlertSlice.actions;
export default fraudAlertSlice.reducer;
