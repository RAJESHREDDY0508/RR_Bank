import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { FraudAlert } from '../types';

interface FraudAlertState {
  alerts: FraudAlert[];
  selectedAlert: FraudAlert | null;
  loading: boolean;
  error: string | null;
  unreadCount: number;
}

const initialState: FraudAlertState = {
  alerts: [],
  selectedAlert: null,
  loading: false,
  error: null,
  unreadCount: 0,
};

const fraudAlertSlice = createSlice({
  name: 'fraudAlerts',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setAlerts: (state, action: PayloadAction<FraudAlert[]>) => {
      state.alerts = action.payload;
      state.unreadCount = action.payload.filter(a => a.status === 'PENDING').length;
      state.loading = false;
      state.error = null;
    },
    setSelectedAlert: (state, action: PayloadAction<FraudAlert | null>) => {
      state.selectedAlert = action.payload;
    },
    updateAlertStatus: (state, action: PayloadAction<{ id: string; status: string }>) => {
      const alert = state.alerts.find(a => a.id === action.payload.id);
      if (alert) {
        alert.status = action.payload.status;
      }
      if (state.selectedAlert?.id === action.payload.id) {
        state.selectedAlert.status = action.payload.status;
      }
      state.unreadCount = state.alerts.filter(a => a.status === 'PENDING').length;
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
  setAlerts,
  setSelectedAlert,
  updateAlertStatus,
  setError,
  clearError,
} = fraudAlertSlice.actions;

export default fraudAlertSlice.reducer;
