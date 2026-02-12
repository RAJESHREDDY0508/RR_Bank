/**
 * Responsive Data Display Components
 * Provides table view for desktop and card view for mobile
 */

import React, { useState } from 'react';
import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Paper,
  Card,
  CardContent,
  CardActions,
  Typography,
  IconButton,
  Chip,
  useTheme,
  useMediaQuery,
  Collapse,
  Button,
  Stack,
  Skeleton,
  Alert,
  Divider,
  Grid,
} from '@mui/material';
import {
  KeyboardArrowDown,
  KeyboardArrowUp,
  Visibility,
  Edit,
  Delete,
  MoreVert,
  ChevronLeft,
  ChevronRight,
} from '@mui/icons-material';

// Column definition
export interface Column<T> {
  id: string;
  label: string;
  accessor: keyof T | ((row: T) => React.ReactNode);
  align?: 'left' | 'center' | 'right';
  minWidth?: number;
  format?: (value: any, row: T) => React.ReactNode;
  sortable?: boolean;
  hideOnMobile?: boolean;
  priority?: number; // Lower = shown first on mobile cards
}

// Responsive table props
interface ResponsiveTableProps<T> {
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  error?: string | null;
  keyExtractor: (row: T) => string;
  onRowClick?: (row: T) => void;
  onView?: (row: T) => void;
  onEdit?: (row: T) => void;
  onDelete?: (row: T) => void;
  actions?: (row: T) => React.ReactNode;
  pagination?: {
    page: number;
    rowsPerPage: number;
    totalCount: number;
    onPageChange: (page: number) => void;
    onRowsPerPageChange: (rowsPerPage: number) => void;
  };
  emptyMessage?: string;
  cardTitle?: (row: T) => React.ReactNode;
  cardSubtitle?: (row: T) => React.ReactNode;
}

// Mobile card row component
interface MobileCardProps<T> {
  row: T;
  columns: Column<T>[];
  onView?: (row: T) => void;
  onEdit?: (row: T) => void;
  onDelete?: (row: T) => void;
  actions?: (row: T) => React.ReactNode;
  cardTitle?: (row: T) => React.ReactNode;
  cardSubtitle?: (row: T) => React.ReactNode;
}

function MobileCard<T>({
  row,
  columns,
  onView,
  onEdit,
  onDelete,
  actions,
  cardTitle,
  cardSubtitle,
}: MobileCardProps<T>) {
  const [expanded, setExpanded] = useState(false);

  const getValue = (column: Column<T>) => {
    const value = typeof column.accessor === 'function'
      ? column.accessor(row)
      : row[column.accessor as keyof T];
    
    if (column.format) {
      return column.format(value, row);
    }
    return value as React.ReactNode;
  };

  // Sort columns by priority for mobile display
  const sortedColumns = [...columns].sort((a, b) => (a.priority || 99) - (b.priority || 99));
  const primaryColumns = sortedColumns.slice(0, 3);
  const secondaryColumns = sortedColumns.slice(3);

  return (
    <Card sx={{ mb: 2 }} variant="outlined">
      <CardContent sx={{ pb: 1 }}>
        {/* Card Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
          <Box>
            {cardTitle ? (
              <Typography variant="subtitle1" fontWeight="bold">
                {cardTitle(row)}
              </Typography>
            ) : (
              <Typography variant="subtitle1" fontWeight="bold">
                {getValue(primaryColumns[0])}
              </Typography>
            )}
            {cardSubtitle && (
              <Typography variant="body2" color="text.secondary">
                {cardSubtitle(row)}
              </Typography>
            )}
          </Box>
          {secondaryColumns.length > 0 && (
            <IconButton
              size="small"
              onClick={() => setExpanded(!expanded)}
              sx={{ ml: 1 }}
            >
              {expanded ? <KeyboardArrowUp /> : <KeyboardArrowDown />}
            </IconButton>
          )}
        </Box>

        {/* Primary Fields */}
        <Stack spacing={1}>
          {primaryColumns.slice(cardTitle ? 0 : 1).map(column => (
            <Box key={column.id} sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" color="text.secondary">
                {column.label}:
              </Typography>
              <Typography variant="body2">
                {getValue(column)}
              </Typography>
            </Box>
          ))}
        </Stack>

        {/* Expanded Fields */}
        <Collapse in={expanded}>
          <Divider sx={{ my: 1 }} />
          <Stack spacing={1}>
            {secondaryColumns.map(column => (
              <Box key={column.id} sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">
                  {column.label}:
                </Typography>
                <Typography variant="body2">
                  {getValue(column)}
                </Typography>
              </Box>
            ))}
          </Stack>
        </Collapse>
      </CardContent>

      {/* Actions */}
      {(onView || onEdit || onDelete || actions) && (
        <CardActions sx={{ pt: 0, justifyContent: 'flex-end' }}>
          {actions ? (
            actions(row)
          ) : (
            <>
              {onView && (
                <Button size="small" onClick={() => onView(row)} startIcon={<Visibility />}>
                  View
                </Button>
              )}
              {onEdit && (
                <Button size="small" onClick={() => onEdit(row)} startIcon={<Edit />}>
                  Edit
                </Button>
              )}
              {onDelete && (
                <Button size="small" color="error" onClick={() => onDelete(row)} startIcon={<Delete />}>
                  Delete
                </Button>
              )}
            </>
          )}
        </CardActions>
      )}
    </Card>
  );
}

// Loading skeleton for cards
const CardSkeleton: React.FC = () => (
  <Card sx={{ mb: 2 }} variant="outlined">
    <CardContent>
      <Skeleton variant="text" width="60%" height={24} />
      <Skeleton variant="text" width="40%" height={20} />
      <Box sx={{ mt: 1 }}>
        <Skeleton variant="text" width="100%" />
        <Skeleton variant="text" width="100%" />
      </Box>
    </CardContent>
  </Card>
);

// Loading skeleton for table rows
const TableRowSkeleton: React.FC<{ columns: number }> = ({ columns }) => (
  <TableRow>
    {Array.from({ length: columns }).map((_, i) => (
      <TableCell key={i}>
        <Skeleton variant="text" />
      </TableCell>
    ))}
  </TableRow>
);

// Main ResponsiveTable component
function ResponsiveTable<T>({
  columns,
  data,
  loading = false,
  error = null,
  keyExtractor,
  onRowClick,
  onView,
  onEdit,
  onDelete,
  actions,
  pagination,
  emptyMessage = 'No data available',
  cardTitle,
  cardSubtitle,
}: ResponsiveTableProps<T>) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const getValue = (row: T, column: Column<T>) => {
    const value = typeof column.accessor === 'function'
      ? column.accessor(row)
      : row[column.accessor as keyof T];
    
    if (column.format) {
      return column.format(value, row);
    }
    return value as React.ReactNode;
  };

  // Filter columns for desktop (hide hideOnMobile columns only affect mobile)
  const visibleColumns = isMobile
    ? columns.filter(col => !col.hideOnMobile)
    : columns;

  // Error state
  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error}
      </Alert>
    );
  }

  // Mobile view - Cards
  if (isMobile) {
    return (
      <Box>
        {loading ? (
          Array.from({ length: 5 }).map((_, i) => <CardSkeleton key={i} />)
        ) : data.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: 'center' }}>
            <Typography color="text.secondary">{emptyMessage}</Typography>
          </Paper>
        ) : (
          <>
            {data.map(row => (
              <MobileCard
                key={keyExtractor(row)}
                row={row}
                columns={visibleColumns}
                onView={onView}
                onEdit={onEdit}
                onDelete={onDelete}
                actions={actions}
                cardTitle={cardTitle}
                cardSubtitle={cardSubtitle}
              />
            ))}
          </>
        )}

        {/* Mobile Pagination */}
        {pagination && data.length > 0 && (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              gap: 2,
              py: 2,
            }}
          >
            <IconButton
              onClick={() => pagination.onPageChange(pagination.page - 1)}
              disabled={pagination.page === 0}
            >
              <ChevronLeft />
            </IconButton>
            <Typography variant="body2">
              Page {pagination.page + 1} of {Math.ceil(pagination.totalCount / pagination.rowsPerPage)}
            </Typography>
            <IconButton
              onClick={() => pagination.onPageChange(pagination.page + 1)}
              disabled={(pagination.page + 1) * pagination.rowsPerPage >= pagination.totalCount}
            >
              <ChevronRight />
            </IconButton>
          </Box>
        )}
      </Box>
    );
  }

  // Desktop view - Table
  return (
    <Paper elevation={0} variant="outlined">
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              {visibleColumns.map(column => (
                <TableCell
                  key={column.id}
                  align={column.align || 'left'}
                  style={{ minWidth: column.minWidth }}
                  sx={{ fontWeight: 'bold', bgcolor: 'grey.50' }}
                >
                  {column.label}
                </TableCell>
              ))}
              {(onView || onEdit || onDelete || actions) && (
                <TableCell align="right" sx={{ fontWeight: 'bold', bgcolor: 'grey.50' }}>
                  Actions
                </TableCell>
              )}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRowSkeleton key={i} columns={visibleColumns.length + 1} />
              ))
            ) : data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={visibleColumns.length + 1} align="center" sx={{ py: 5 }}>
                  <Typography color="text.secondary">{emptyMessage}</Typography>
                </TableCell>
              </TableRow>
            ) : (
              data.map(row => (
                <TableRow
                  key={keyExtractor(row)}
                  hover
                  onClick={() => onRowClick?.(row)}
                  sx={{ cursor: onRowClick ? 'pointer' : 'default' }}
                >
                  {visibleColumns.map(column => (
                    <TableCell key={column.id} align={column.align || 'left'}>
                      {getValue(row, column)}
                    </TableCell>
                  ))}
                  {(onView || onEdit || onDelete || actions) && (
                    <TableCell align="right">
                      {actions ? (
                        actions(row)
                      ) : (
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          {onView && (
                            <IconButton
                              size="small"
                              onClick={(e) => { e.stopPropagation(); onView(row); }}
                            >
                              <Visibility fontSize="small" />
                            </IconButton>
                          )}
                          {onEdit && (
                            <IconButton
                              size="small"
                              onClick={(e) => { e.stopPropagation(); onEdit(row); }}
                            >
                              <Edit fontSize="small" />
                            </IconButton>
                          )}
                          {onDelete && (
                            <IconButton
                              size="small"
                              color="error"
                              onClick={(e) => { e.stopPropagation(); onDelete(row); }}
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          )}
                        </Stack>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Desktop Pagination */}
      {pagination && (
        <TablePagination
          component="div"
          count={pagination.totalCount}
          page={pagination.page}
          onPageChange={(_, page) => pagination.onPageChange(page)}
          rowsPerPage={pagination.rowsPerPage}
          onRowsPerPageChange={(e) => pagination.onRowsPerPageChange(parseInt(e.target.value, 10))}
          rowsPerPageOptions={[5, 10, 25, 50]}
        />
      )}
    </Paper>
  );
}

export default ResponsiveTable;
export type { ResponsiveTableProps };
