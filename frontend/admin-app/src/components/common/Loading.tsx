/**
 * Loading Component with various display modes
 */

import React from 'react';
import {
  Box,
  CircularProgress,
  Typography,
  Backdrop,
  Skeleton,
  Paper,
} from '@mui/material';

interface LoadingProps {
  text?: string;
  fullScreen?: boolean;
  size?: 'small' | 'medium' | 'large';
  overlay?: boolean;
}

const Loading: React.FC<LoadingProps> = ({
  text = 'Loading...',
  fullScreen = false,
  size = 'medium',
  overlay = false,
}) => {
  const sizeMap = {
    small: 24,
    medium: 40,
    large: 60,
  };

  const content = (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        p: 3,
      }}
    >
      <CircularProgress size={sizeMap[size]} />
      {text && (
        <Typography variant="body2" color="text.secondary">
          {text}
        </Typography>
      )}
    </Box>
  );

  if (overlay) {
    return (
      <Backdrop
        sx={{
          color: '#fff',
          zIndex: (theme) => theme.zIndex.drawer + 1,
          bgcolor: 'rgba(0, 0, 0, 0.5)',
        }}
        open={true}
      >
        <Paper sx={{ p: 4, borderRadius: 2 }}>{content}</Paper>
      </Backdrop>
    );
  }

  if (fullScreen) {
    return (
      <Box
        sx={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: 'background.default',
          zIndex: 9999,
        }}
      >
        {content}
      </Box>
    );
  }

  return content;
};

/**
 * Page Loading Skeleton
 */
export const PageSkeleton: React.FC = () => (
  <Box>
    {/* Header skeleton */}
    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
      <Skeleton variant="text" width={200} height={40} />
      <Skeleton variant="rectangular" width={120} height={40} />
    </Box>

    {/* Filter skeleton */}
    <Paper sx={{ p: 2, mb: 3 }}>
      <Box sx={{ display: 'flex', gap: 2 }}>
        <Skeleton variant="rectangular" width="100%" height={40} />
        <Skeleton variant="rectangular" width={100} height={40} />
      </Box>
    </Paper>

    {/* Table skeleton */}
    <Paper sx={{ p: 2 }}>
      {Array.from({ length: 5 }).map((_, i) => (
        <Box key={i} sx={{ display: 'flex', gap: 2, mb: 2 }}>
          <Skeleton variant="rectangular" width="20%" height={24} />
          <Skeleton variant="rectangular" width="25%" height={24} />
          <Skeleton variant="rectangular" width="20%" height={24} />
          <Skeleton variant="rectangular" width="15%" height={24} />
          <Skeleton variant="rectangular" width="20%" height={24} />
        </Box>
      ))}
    </Paper>
  </Box>
);

/**
 * Card Loading Skeleton
 */
export const CardSkeleton: React.FC<{ count?: number }> = ({ count = 4 }) => (
  <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' }, gap: 3 }}>
    {Array.from({ length: count }).map((_, i) => (
      <Paper key={i} sx={{ p: 2 }}>
        <Skeleton variant="text" width="60%" height={24} />
        <Skeleton variant="text" width="80%" height={48} />
        <Skeleton variant="text" width="40%" height={20} />
      </Paper>
    ))}
  </Box>
);

/**
 * Inline Loading Indicator
 */
export const InlineLoading: React.FC<{ text?: string }> = ({ text }) => (
  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
    <CircularProgress size={16} />
    {text && (
      <Typography variant="caption" color="text.secondary">
        {text}
      </Typography>
    )}
  </Box>
);

export default Loading;
