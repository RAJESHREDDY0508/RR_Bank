import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type ThemeMode = 'light' | 'dark' | 'system';

interface ThemeState {
  mode: ThemeMode;
  isDark: boolean;
}

const getInitialTheme = (): ThemeMode => {
  const savedTheme = localStorage.getItem('theme') as ThemeMode;
  return savedTheme || 'light';
};

const getIsDark = (mode: ThemeMode): boolean => {
  if (mode === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
  return mode === 'dark';
};

const initialState: ThemeState = {
  mode: getInitialTheme(),
  isDark: getIsDark(getInitialTheme()),
};

const themeSlice = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    setTheme: (state, action: PayloadAction<ThemeMode>) => {
      state.mode = action.payload;
      state.isDark = getIsDark(action.payload);
      localStorage.setItem('theme', action.payload);
      
      // Update document class for Tailwind dark mode
      if (state.isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    },
    toggleTheme: (state) => {
      const newMode = state.mode === 'dark' ? 'light' : 'dark';
      state.mode = newMode;
      state.isDark = newMode === 'dark';
      localStorage.setItem('theme', newMode);
      
      // Update document class for Tailwind dark mode
      if (state.isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    },
  },
});

export const { setTheme, toggleTheme } = themeSlice.actions;
export default themeSlice.reducer;
