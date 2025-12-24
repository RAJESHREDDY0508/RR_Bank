import apiClient from './client';
import { Account, AccountBalance, DepositRequest, WithdrawRequest } from '../types';

export const accountsApi = {
  // Get all accounts for current user
  getAllAccounts: async (): Promise<Account[]> => {
    const response = await apiClient.get('/accounts');
    return response.data;
  },

  // Get account by account number
  getAccountByNumber: async (accountNumber: string): Promise<Account> => {
    const response = await apiClient.get(`/accounts/${accountNumber}`);
    return response.data;
  },

  // Get account balance
  getAccountBalance: async (accountNumber: string): Promise<AccountBalance> => {
    const response = await apiClient.get(`/accounts/${accountNumber}/balance`);
    return response.data;
  },

  // Create new account
  createAccount: async (accountData: {
    accountType: string;
    initialDeposit?: number;
  }): Promise<Account> => {
    const response = await apiClient.post('/accounts', accountData);
    return response.data;
  },

  // Deposit funds
  deposit: async (depositData: DepositRequest): Promise<Account> => {
    const response = await apiClient.post(
      `/accounts/${depositData.accountNumber}/deposit`,
      {
        amount: depositData.amount,
        description: depositData.description,
      }
    );
    return response.data;
  },

  // Withdraw funds
  withdraw: async (withdrawData: WithdrawRequest): Promise<Account> => {
    const response = await apiClient.post(
      `/accounts/${withdrawData.accountNumber}/withdraw`,
      {
        amount: withdrawData.amount,
        description: withdrawData.description,
      }
    );
    return response.data;
  },

  // Update account status
  updateAccountStatus: async (
    accountNumber: string,
    status: 'ACTIVE' | 'INACTIVE' | 'FROZEN'
  ): Promise<Account> => {
    const response = await apiClient.patch(`/accounts/${accountNumber}/status`, {
      status,
    });
    return response.data;
  },

  // Close account
  closeAccount: async (accountNumber: string): Promise<void> => {
    await apiClient.delete(`/accounts/${accountNumber}`);
  },

  // Get account summary
  getAccountSummary: async (accountNumber: string): Promise<{
    account: Account;
    balance: AccountBalance;
    transactionCount: number;
    lastTransaction?: any;
  }> => {
    const response = await apiClient.get(`/accounts/${accountNumber}/summary`);
    return response.data;
  },
};

export default accountsApi;
