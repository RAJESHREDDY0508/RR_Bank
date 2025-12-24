import React from 'react';
import { CircularProgress, Box, Typography } from '@mui/material';

interface LoadingProps {
  text?: string;
  size?: number;
}

const Loading: React.FC<LoadingProps> = ({ text = 'Loading...', size = 40 }) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '200px',
        gap: 2,
      }}
    >
      <CircularProgress size={size} />
      {text && (
        <Typography variant="body1" color="text.secondary">
          {text}
        </Typography>
      )}
    </Box>
  );
};

export default Loading;
