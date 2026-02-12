/**
 * Responsive Filter Panel Component
 * Shows inline filters on desktop, slide-over panel on mobile
 */

import React, { useState } from 'react';
import {
  Box,
  Paper,
  TextField,
  Button,
  InputAdornment,
  MenuItem,
  useTheme,
  useMediaQuery,
  Drawer,
  IconButton,
  Typography,
  Stack,
  Divider,
  Chip,
  Badge,
  Collapse,
  Grid,
} from '@mui/material';
import {
  Search,
  FilterList,
  Close,
  Clear,
  ExpandMore,
  ExpandLess,
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

// Filter field types
export interface FilterField {
  id: string;
  label: string;
  type: 'text' | 'select' | 'date' | 'dateRange' | 'number';
  placeholder?: string;
  options?: { value: string; label: string }[];
  fullWidth?: boolean;
  gridSize?: number; // Grid columns for desktop (out of 12)
}

// Filter values type
export type FilterValues = Record<string, any>;

interface FilterPanelProps {
  fields: FilterField[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  onSearch: () => void;
  onClear: () => void;
  searchPlaceholder?: string;
  showSearch?: boolean;
}

const FilterPanel: React.FC<FilterPanelProps> = ({
  fields,
  values,
  onChange,
  onSearch,
  onClear,
  searchPlaceholder = 'Search...',
  showSearch = true,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [expanded, setExpanded] = useState(false);

  // Count active filters
  const activeFilterCount = Object.entries(values).filter(([key, value]) => {
    if (key === 'search') return false;
    return value !== '' && value !== null && value !== undefined;
  }).length;

  const handleChange = (fieldId: string, value: any) => {
    onChange({ ...values, [fieldId]: value });
  };

  const handleClear = () => {
    onClear();
    setDrawerOpen(false);
  };

  const handleApply = () => {
    onSearch();
    setDrawerOpen(false);
  };

  const renderField = (field: FilterField, forceFullWidth: boolean = false) => {
    const value = values[field.id] ?? '';
    const fullWidth = forceFullWidth || field.fullWidth !== false;

    switch (field.type) {
      case 'select':
        return (
          <TextField
            select
            label={field.label}
            value={value}
            onChange={(e) => handleChange(field.id, e.target.value)}
            size="small"
            fullWidth={fullWidth}
            sx={{ minWidth: fullWidth ? 'auto' : 150 }}
          >
            <MenuItem value="">All</MenuItem>
            {field.options?.map(opt => (
              <MenuItem key={opt.value} value={opt.value}>
                {opt.label}
              </MenuItem>
            ))}
          </TextField>
        );

      case 'date':
        return (
          <LocalizationProvider dateAdapter={AdapterDateFns}>
            <DatePicker
              label={field.label}
              value={value || null}
              onChange={(newValue) => handleChange(field.id, newValue)}
              slotProps={{
                textField: {
                  size: 'small',
                  fullWidth: fullWidth,
                },
              }}
            />
          </LocalizationProvider>
        );

      case 'number':
        return (
          <TextField
            type="number"
            label={field.label}
            value={value}
            onChange={(e) => handleChange(field.id, e.target.value)}
            size="small"
            fullWidth={fullWidth}
            placeholder={field.placeholder}
          />
        );

      default:
        return (
          <TextField
            label={field.label}
            value={value}
            onChange={(e) => handleChange(field.id, e.target.value)}
            size="small"
            fullWidth={fullWidth}
            placeholder={field.placeholder}
          />
        );
    }
  };

  // Mobile filter drawer
  const filterDrawer = (
    <Drawer
      anchor="right"
      open={drawerOpen}
      onClose={() => setDrawerOpen(false)}
      PaperProps={{
        sx: { width: '100%', maxWidth: 360 },
      }}
    >
      <Box sx={{ p: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" fontWeight="bold">
            Filters
          </Typography>
          <IconButton onClick={() => setDrawerOpen(false)}>
            <Close />
          </IconButton>
        </Box>

        <Divider sx={{ mb: 2 }} />

        <Stack spacing={2}>
          {fields.map(field => (
            <Box key={field.id}>
              {renderField(field, true)}
            </Box>
          ))}
        </Stack>

        <Divider sx={{ my: 2 }} />

        <Stack direction="row" spacing={2}>
          <Button
            variant="outlined"
            fullWidth
            onClick={handleClear}
            startIcon={<Clear />}
          >
            Clear All
          </Button>
          <Button
            variant="contained"
            fullWidth
            onClick={handleApply}
          >
            Apply Filters
          </Button>
        </Stack>
      </Box>
    </Drawer>
  );

  // Mobile view
  if (isMobile) {
    return (
      <>
        <Paper elevation={0} variant="outlined" sx={{ p: 2, mb: 2 }}>
          <Stack direction="row" spacing={1}>
            {showSearch && (
              <TextField
                fullWidth
                placeholder={searchPlaceholder}
                value={values.search || ''}
                onChange={(e) => handleChange('search', e.target.value)}
                size="small"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search />
                    </InputAdornment>
                  ),
                }}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    onSearch();
                  }
                }}
              />
            )}
            <Badge badgeContent={activeFilterCount} color="primary">
              <Button
                variant="outlined"
                onClick={() => setDrawerOpen(true)}
                startIcon={<FilterList />}
                sx={{ minWidth: 'auto', px: 2 }}
              >
                Filters
              </Button>
            </Badge>
          </Stack>
        </Paper>
        {filterDrawer}
      </>
    );
  }

  // Desktop view - inline filters
  return (
    <Paper elevation={0} variant="outlined" sx={{ p: 2, mb: 2 }}>
      <Grid container spacing={2} alignItems="center">
        {/* Search field */}
        {showSearch && (
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              placeholder={searchPlaceholder}
              value={values.search || ''}
              onChange={(e) => handleChange('search', e.target.value)}
              size="small"
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                ),
              }}
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  onSearch();
                }
              }}
            />
          </Grid>
        )}

        {/* Filter fields */}
        {fields.slice(0, expanded ? fields.length : 3).map(field => (
          <Grid item xs={12} sm={6} md={field.gridSize || 2} key={field.id}>
            {renderField(field)}
          </Grid>
        ))}

        {/* Action buttons */}
        <Grid item xs={12} md="auto">
          <Stack direction="row" spacing={1}>
            <Button variant="contained" onClick={onSearch}>
              Search
            </Button>
            {activeFilterCount > 0 && (
              <Button variant="outlined" onClick={onClear} startIcon={<Clear />}>
                Clear
              </Button>
            )}
            {fields.length > 3 && (
              <Button
                variant="text"
                onClick={() => setExpanded(!expanded)}
                endIcon={expanded ? <ExpandLess /> : <ExpandMore />}
              >
                {expanded ? 'Less' : 'More'}
              </Button>
            )}
          </Stack>
        </Grid>
      </Grid>

      {/* Active filter chips */}
      {activeFilterCount > 0 && (
        <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {Object.entries(values)
            .filter(([key, value]) => key !== 'search' && value !== '' && value !== null)
            .map(([key, value]) => {
              const field = fields.find(f => f.id === key);
              const label = field?.options?.find(o => o.value === value)?.label || String(value);
              return (
                <Chip
                  key={key}
                  label={`${field?.label}: ${label}`}
                  size="small"
                  onDelete={() => handleChange(key, '')}
                />
              );
            })}
        </Box>
      )}
    </Paper>
  );
};

export default FilterPanel;
