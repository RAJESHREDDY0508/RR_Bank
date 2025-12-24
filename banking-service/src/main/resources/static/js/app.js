// Banking Application Frontend JavaScript

// Configuration
const API_BASE_URL = 'http://localhost:8080';

// State Management
let currentUser = null;
let authToken = null;

// Initialize app on page load
document.addEventListener('DOMContentLoaded', () => {
    checkApiStatus();
    loadUserFromStorage();
    
    // Check API status every 30 seconds
    setInterval(checkApiStatus, 30000);
});

// ==================== AUTH FUNCTIONS ====================

// Check if user is logged in from localStorage
function loadUserFromStorage() {
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('currentUser');
    
    if (token && user) {
        authToken = token;
        currentUser = JSON.parse(user);
        updateUIForLoggedInUser();
    }
}

// Handle Login
async function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    
    clearMessages('login');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        if (response.ok) {
            const data = await response.json();
            authToken = data.token;
            currentUser = {
                username: data.username,
                email: data.email,
                role: data.role
            };
            
            // Store in localStorage
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            
            showSuccess('login', 'Login successful!');
            
            setTimeout(() => {
                updateUIForLoggedInUser();
                if (currentUser.role === 'ADMIN') {
                    showPage('admin');
                    loadAllUsers();
                } else {
                    showPage('dashboard');
                    loadDashboardData();
                }
            }, 1000);
            
        } else {
            const error = await response.text();
            showError('login', 'Invalid username or password');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('login', 'Unable to connect to server. Please check if the backend is running.');
    }
}

// Handle Register
async function handleRegister(event) {
    event.preventDefault();
    
    const userData = {
        username: document.getElementById('registerUsername').value,
        password: document.getElementById('registerPassword').value,
        email: document.getElementById('registerEmail').value,
        firstName: document.getElementById('registerFirstName').value,
        lastName: document.getElementById('registerLastName').value
    };
    
    clearMessages('register');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        
        if (response.ok) {
            const data = await response.json();
            authToken = data.token;
            currentUser = {
                username: data.username,
                email: data.email,
                role: data.role
            };
            
            // Store in localStorage
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            
            showSuccess('register', 'Account created successfully!');
            
            setTimeout(() => {
                updateUIForLoggedInUser();
                showPage('dashboard');
                loadDashboardData();
            }, 1000);
            
        } else {
            const error = await response.text();
            showError('register', error || 'Registration failed. Username or email may already exist.');
        }
    } catch (error) {
        console.error('Register error:', error);
        showError('register', 'Unable to connect to server. Please check if the backend is running.');
    }
}

// Logout
function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    
    updateUIForLoggedOutUser();
    showPage('home');
}

// ==================== DASHBOARD FUNCTIONS ====================

// Load Dashboard Data
async function loadDashboardData() {
    loadProfileInfo();
    loadTokenInfo();
    loadAccounts();
}

// Load Profile Info
function loadProfileInfo() {
    const profileDiv = document.getElementById('profileInfo');
    
    if (currentUser) {
        profileDiv.innerHTML = `
            <div class="token-info-item"><strong>Username:</strong> ${currentUser.username}</div>
            <div class="token-info-item"><strong>Email:</strong> ${currentUser.email}</div>
            <div class="token-info-item"><strong>Role:</strong> <span class="badge badge-${currentUser.role.toLowerCase()}">${currentUser.role}</span></div>
        `;
    }
}

// Load Token Info
function loadTokenInfo() {
    const tokenDiv = document.getElementById('tokenInfo');
    
    if (authToken) {
        // Decode JWT payload
        const payload = JSON.parse(atob(authToken.split('.')[1]));
        
        const issuedAt = new Date(payload.iat * 1000).toLocaleString();
        const expiresAt = new Date(payload.exp * 1000).toLocaleString();
        
        tokenDiv.innerHTML = `
            <div class="token-info-item"><strong>Subject:</strong> ${payload.sub}</div>
            <div class="token-info-item"><strong>User ID:</strong> ${payload.userId}</div>
            <div class="token-info-item"><strong>Roles:</strong> ${JSON.stringify(payload.roles)}</div>
            <div class="token-info-item"><strong>Issued:</strong> ${issuedAt}</div>
            <div class="token-info-item"><strong>Expires:</strong> ${expiresAt}</div>
            <div style="margin-top: 1rem;">
                <strong>Token (First 100 chars):</strong>
                <div class="token-display">${authToken.substring(0, 100)}...</div>
            </div>
        `;
    }
}

// Load Accounts
async function loadAccounts() {
    const accountsDiv = document.getElementById('accountsList');
    accountsDiv.innerHTML = '<p class="loading">Loading accounts...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/accounts`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const accounts = await response.json();
            
            if (accounts.length === 0) {
                accountsDiv.innerHTML = `
                    <div class="no-data">
                        <div class="no-data-icon">💳</div>
                        <p>No accounts yet. Create your first account!</p>
                    </div>
                `;
            } else {
                accountsDiv.innerHTML = accounts.map(account => `
                    <div class="account-card">
                        <h4>${account.accountType} Account</h4>
                        <p><strong>Account Number:</strong> ${account.accountNumber || 'N/A'}</p>
                        <div class="balance">$${account.balance?.toFixed(2) || '0.00'}</div>
                        <p><strong>Status:</strong> <span class="badge badge-${account.status?.toLowerCase() || 'active'}">${account.status || 'ACTIVE'}</span></p>
                        <p><strong>Created:</strong> ${new Date(account.createdAt).toLocaleDateString()}</p>
                    </div>
                `).join('');
            }
        } else if (response.status === 500) {
            // AccountService not fully implemented
            accountsDiv.innerHTML = `
                <div class="no-data">
                    <div class="no-data-icon">⚠️</div>
                    <p>Account service is not fully implemented yet.</p>
                    <p style="font-size: 0.9rem; color: #64748b;">This is expected if you haven't implemented the AccountService backend.</p>
                </div>
            `;
        } else {
            showError('accounts', 'Failed to load accounts');
        }
    } catch (error) {
        console.error('Load accounts error:', error);
        accountsDiv.innerHTML = `
            <div class="no-data">
                <div class="no-data-icon">❌</div>
                <p>Unable to load accounts. Server may be down.</p>
            </div>
        `;
    }
}

// Handle Create Account
async function handleCreateAccount(event) {
    event.preventDefault();
    
    const accountData = {
        accountType: document.getElementById('accountType').value,
        initialBalance: parseFloat(document.getElementById('initialBalance').value)
    };
    
    clearMessages('createAccount');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/accounts`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify(accountData)
        });
        
        if (response.ok) {
            closeCreateAccountModal();
            loadAccounts();
            alert('Account created successfully!');
        } else {
            showError('createAccount', 'Failed to create account. Service may not be implemented yet.');
        }
    } catch (error) {
        console.error('Create account error:', error);
        showError('createAccount', 'Unable to connect to server');
    }
}

// ==================== ADMIN FUNCTIONS ====================

// Load All Users (Admin)
async function loadAllUsers() {
    const usersDiv = document.getElementById('usersList');
    usersDiv.innerHTML = '<p class="loading">Loading users...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/users`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const users = await response.json();
            
            if (users.length === 0) {
                usersDiv.innerHTML = '<div class="no-data"><p>No users found</p></div>';
            } else {
                usersDiv.innerHTML = `
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Username</th>
                                <th>Email</th>
                                <th>Name</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Created</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${users.map(user => `
                                <tr>
                                    <td>${user.username}</td>
                                    <td>${user.email}</td>
                                    <td>${user.firstName} ${user.lastName}</td>
                                    <td><span class="badge badge-${user.role.toLowerCase()}">${user.role}</span></td>
                                    <td><span class="badge badge-${user.enabled ? 'active' : 'inactive'}">${user.enabled ? 'Active' : 'Inactive'}</span></td>
                                    <td>${new Date(user.createdAt).toLocaleDateString()}</td>
                                    <td>
                                        <button class="btn btn-sm btn-secondary" onclick="changeUserRole('${user.id}', '${user.role}')">Change Role</button>
                                        <button class="btn btn-sm btn-${user.enabled ? 'danger' : 'primary'}" onclick="toggleUserStatus('${user.id}', ${user.enabled})">${user.enabled ? 'Disable' : 'Enable'}</button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            }
        } else if (response.status === 403) {
            usersDiv.innerHTML = '<div class="no-data"><p>Access denied. Admin privileges required.</p></div>';
        } else {
            usersDiv.innerHTML = '<div class="no-data"><p>Failed to load users</p></div>';
        }
    } catch (error) {
        console.error('Load users error:', error);
        usersDiv.innerHTML = '<div class="no-data"><p>Unable to connect to server</p></div>';
    }
}

// Change User Role
async function changeUserRole(userId, currentRole) {
    const roles = ['CUSTOMER', 'ADMIN', 'MANAGER', 'SUPPORT'];
    const newRole = prompt(`Change role from ${currentRole} to:\n${roles.map((r, i) => `${i+1}. ${r}`).join('\n')}\nEnter number (1-4):`);
    
    if (newRole && newRole >= 1 && newRole <= 4) {
        const selectedRole = roles[newRole - 1];
        
        try {
            const response = await fetch(`${API_BASE_URL}/api/admin/users/${userId}/role?role=${selectedRole}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            
            if (response.ok) {
                alert('Role updated successfully!');
                loadAllUsers();
            } else {
                alert('Failed to update role');
            }
        } catch (error) {
            console.error('Change role error:', error);
            alert('Unable to connect to server');
        }
    }
}

// Toggle User Status
async function toggleUserStatus(userId, currentStatus) {
    const newStatus = !currentStatus;
    const action = newStatus ? 'enable' : 'disable';
    
    if (confirm(`Are you sure you want to ${action} this user?`)) {
        try {
            const response = await fetch(`${API_BASE_URL}/api/admin/users/${userId}/status?enabled=${newStatus}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            
            if (response.ok) {
                alert('User status updated successfully!');
                loadAllUsers();
            } else {
                alert('Failed to update user status');
            }
        } catch (error) {
            console.error('Toggle status error:', error);
            alert('Unable to connect to server');
        }
    }
}

// Load All Accounts (Admin)
async function loadAllAccounts() {
    const accountsDiv = document.getElementById('adminAccountsList');
    accountsDiv.innerHTML = '<p class="loading">Loading all accounts...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/accounts/admin/all`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const accounts = await response.json();
            
            if (accounts.length === 0) {
                accountsDiv.innerHTML = '<div class="no-data"><p>No accounts found</p></div>';
            } else {
                accountsDiv.innerHTML = `
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Account Number</th>
                                <th>Type</th>
                                <th>Balance</th>
                                <th>Status</th>
                                <th>User ID</th>
                                <th>Created</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${accounts.map(account => `
                                <tr>
                                    <td>${account.accountNumber || 'N/A'}</td>
                                    <td>${account.accountType}</td>
                                    <td>$${account.balance?.toFixed(2) || '0.00'}</td>
                                    <td><span class="badge badge-${account.status?.toLowerCase() || 'active'}">${account.status || 'ACTIVE'}</span></td>
                                    <td>${account.userId}</td>
                                    <td>${new Date(account.createdAt).toLocaleDateString()}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            }
        } else if (response.status === 500) {
            accountsDiv.innerHTML = '<div class="no-data"><p>Account service not fully implemented yet</p></div>';
        } else {
            accountsDiv.innerHTML = '<div class="no-data"><p>Failed to load accounts</p></div>';
        }
    } catch (error) {
        console.error('Load admin accounts error:', error);
        accountsDiv.innerHTML = '<div class="no-data"><p>Unable to connect to server</p></div>';
    }
}

// ==================== UI HELPER FUNCTIONS ====================

// Show Page
function showPage(pageName) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    
    // Show selected page
    const page = document.getElementById(pageName + 'Page');
    if (page) {
        page.classList.add('active');
        
        // Load data if needed
        if (pageName === 'dashboard' && currentUser) {
            loadDashboardData();
        } else if (pageName === 'admin' && currentUser && currentUser.role === 'ADMIN') {
            loadAllUsers();
        }
    }
}

// Update UI for logged in user
function updateUIForLoggedInUser() {
    document.getElementById('loginNavBtn').style.display = 'none';
    document.getElementById('registerNavBtn').style.display = 'none';
    document.getElementById('dashboardNavBtn').style.display = 'inline-block';
    document.getElementById('logoutNavBtn').style.display = 'inline-block';
    
    if (currentUser && currentUser.role === 'ADMIN') {
        document.getElementById('adminNavBtn').style.display = 'inline-block';
    }
    
    const userInfo = document.getElementById('userInfo');
    userInfo.style.display = 'block';
    userInfo.textContent = `👤 ${currentUser.username} (${currentUser.role})`;
}

// Update UI for logged out user
function updateUIForLoggedOutUser() {
    document.getElementById('loginNavBtn').style.display = 'inline-block';
    document.getElementById('registerNavBtn').style.display = 'inline-block';
    document.getElementById('dashboardNavBtn').style.display = 'none';
    document.getElementById('adminNavBtn').style.display = 'none';
    document.getElementById('logoutNavBtn').style.display = 'none';
    document.getElementById('userInfo').style.display = 'none';
}

// Show/Hide Error Messages
function showError(formName, message) {
    const errorDiv = document.getElementById(formName + 'Error');
    errorDiv.textContent = message;
    errorDiv.classList.add('show');
}

function showSuccess(formName, message) {
    const successDiv = document.getElementById(formName + 'Success');
    successDiv.textContent = message;
    successDiv.classList.add('show');
}

function clearMessages(formName) {
    const errorDiv = document.getElementById(formName + 'Error');
    const successDiv = document.getElementById(formName + 'Success');
    
    if (errorDiv) {
        errorDiv.classList.remove('show');
        errorDiv.textContent = '';
    }
    if (successDiv) {
        successDiv.classList.remove('show');
        successDiv.textContent = '';
    }
}

// Modal Functions
function showCreateAccountModal() {
    document.getElementById('createAccountModal').classList.add('show');
}

function closeCreateAccountModal() {
    document.getElementById('createAccountModal').classList.remove('show');
    document.getElementById('createAccountForm').reset();
    clearMessages('createAccount');
}

// Check API Status
async function checkApiStatus() {
    const statusSpan = document.getElementById('apiStatus');
    
    try {
        const response = await fetch(`${API_BASE_URL}/actuator/health`);
        if (response.ok) {
            statusSpan.textContent = 'Online';
            statusSpan.className = 'online';
        } else {
            statusSpan.textContent = 'Error';
            statusSpan.className = 'offline';
        }
    } catch (error) {
        statusSpan.textContent = 'Offline';
        statusSpan.className = 'offline';
    }
}
