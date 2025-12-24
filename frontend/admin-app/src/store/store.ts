import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import customerReducer from './customerSlice';
import accountReducer from './accountSlice';
import fraudAlertReducer from './fraudAlertSlice';
import auditLogReducer from './auditLogSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    customers: customerReducer,
    accounts: accountReducer,
    fraudAlerts: fraudAlertReducer,
    auditLogs: auditLogReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
