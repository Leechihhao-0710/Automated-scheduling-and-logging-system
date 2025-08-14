let currentTasks = [];
let currentEmployeeId = null;
let taskDetailModal = null;

document.addEventListener('DOMContentLoaded', function() {
    initializePage();
});

function initializePage() {
    const user = getCurrentUser();
    if (user && user.employeeNumber) {
        currentEmployeeId = user.employeeNumber.toString().padStart(4, '0');
        console.log('Current employee ID:', currentEmployeeId);
        
        setupEventListeners();
        initializeModal();
        
        showEmptyState();
    } else {
        console.error('No user found in localStorage');
        showError('Please log in again');
        window.location.href = '/login';
    }
}

function setupEventListeners() {
    const applyBtn = document.querySelector('[onclick="applyFilters()"]');
    const clearBtn = document.querySelector('[onclick="clearFilters()"]');

    document.getElementById('searchInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            applyFilters();
        }
    });

    document.getElementById('startDateFilter').addEventListener('change', function() {
        if (this.value && document.getElementById('endDateFilter').value) {
        }
    });
    
    document.getElementById('endDateFilter').addEventListener('change', function() {
        if (this.value && document.getElementById('startDateFilter').value) {
        }
    });
}

function initializeModal() {
    taskDetailModal = new bootstrap.Modal(document.getElementById('taskDetailModal'));
}

async function applyFilters() {
    const filters = {
        search: document.getElementById('searchInput').value.trim(),
        taskType: document.getElementById('taskTypeFilter').value,
        status: document.getElementById('statusFilter').value,
        creatorRole: document.getElementById('creatorFilter').value,
        startDate: document.getElementById('startDateFilter').value,
        endDate: document.getElementById('endDateFilter').value
    };

    try {
        showLoading();
        
        const queryParams = new URLSearchParams();
        Object.keys(filters).forEach(key => {
            if (filters[key]) {
                queryParams.append(key, filters[key]);
            }
        });

        const response = await authFetch(`/tasks/api/user/${currentEmployeeId}/overview?${queryParams.toString()}`);
        
        if (response.ok) {
            const tasks = await response.json();
            currentTasks = tasks;
            
            const stats = calculateStats(tasks);
            
            updateStatistics(stats);
            updateTasksTable(tasks);
            showResults();
            
            console.log('Tasks loaded:', tasks.length);
        } else {
            console.error('Failed to load tasks:', response.status);
            showError('Failed to load task data');
        }
    } catch (error) {
        console.error('Error applying filters:', error);
        showError('Error loading data. Please try again.');
    }
}

function calculateStats(tasks) {
    const now = new Date();
    
    const stats = {
        totalTasks: tasks.length,
        pendingTasks: 0,
        inProgressTasks: 0,
        completedTasks: 0,
        overdueTasks: 0
    };
    
    tasks.forEach(task => {
        switch (task.individualStatus) {
            case 'PENDING':
                stats.pendingTasks++;
                break;
            case 'IN_PROGRESS':
                stats.inProgressTasks++;
                break;
            case 'COMPLETED':
                stats.completedTasks++;
                break;
        }
        
        if (task.isOverdue || (new Date(task.dueDateTime) < now && task.individualStatus !== 'COMPLETED')) {
            stats.overdueTasks++;
        }
    });
    
    return stats;
}

function updateStatistics(stats) {
    document.getElementById('totalTasks').textContent = stats.totalTasks;
    document.getElementById('pendingTasks').textContent = stats.pendingTasks;
    document.getElementById('inProgressTasks').textContent = stats.inProgressTasks;
    document.getElementById('completedTasks').textContent = stats.completedTasks;
    document.getElementById('overdueTasks').textContent = stats.overdueTasks;
}

function updateTasksTable(tasks) {
    const tableBody = document.getElementById('tasksTableBody');
    tableBody.innerHTML = '';
    
    if (tasks.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="6" class="text-center py-4">
                <div class="empty-state">
                    <i class="fas fa-search fa-2x mb-3"></i>
                    <h5>No tasks found</h5>
                    <p>Try adjusting your filter criteria</p>
                </div>
            </td>
        `;
        tableBody.appendChild(row);
        return;
    }
    
    const sortedTasks = tasks.sort((a, b) => {
        const now = new Date();
        const aDueDate = new Date(a.dueDateTime);
        const bDueDate = new Date(b.dueDateTime);
        
        const aOverdue = aDueDate < now && a.individualStatus !== 'COMPLETED';
        const bOverdue = bDueDate < now && b.individualStatus !== 'COMPLETED';
        
        if (aOverdue && !bOverdue) return -1;
        if (!aOverdue && bOverdue) return 1;
        
        return aDueDate - bDueDate;
    });
    
    sortedTasks.forEach(task => {
        const row = document.createElement('tr');
        const isOverdue = isTaskOverdue(task);
        const dueDateClass = getDueDateClass(task.dueDateTime, task.individualStatus);
        
        if (isOverdue) {
            row.classList.add('task-row-overdue');
        }
        
        row.innerHTML = `
            <td>
                <div>
                    <strong>${escapeHtml(task.title)}</strong>
                    ${isOverdue ? '<span class="overdue-indicator">OVERDUE</span>' : ''}
                    ${task.description ? `<br><small class="text-muted">${escapeHtml(task.description.substring(0, 50))}${task.description.length > 50 ? '...' : ''}</small>` : ''}
                </div>
            </td>
            <td>
                <span class="task-type ${getTypeClass(task.taskType)}">${formatTaskType(task.taskType)}</span>
            </td>
            <td>
                <span class="task-status ${getStatusClass(task.individualStatus)}">${formatStatus(task.individualStatus)}</span>
            </td>
            <td>
                <span class="creator-badge ${getCreatorClass(task.creator)}">${getCreatorDisplay(task.creator)}</span>
            </td>
            <td>
                <span class="${dueDateClass}">${formatDateTime(task.dueDateTime)}</span>
                ${getTimeUntilDue(task.dueDateTime, task.individualStatus)}
            </td>
            <td>
                <div class="task-actions">
                    <button class="btn btn-action btn-view" onclick="viewTaskDetails(${task.id})" title="View Details">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-action btn-edit" onclick="editTask(${task.id})" title="Edit Task">
                        <i class="fas fa-edit"></i>
                    </button>
                </div>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

function clearFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('taskTypeFilter').value = '';
    document.getElementById('statusFilter').value = '';
    document.getElementById('creatorFilter').value = '';
    document.getElementById('startDateFilter').value = '';
    document.getElementById('endDateFilter').value = '';
    
    showEmptyState();
    console.log('Filters cleared');
}

function refreshData() {
    applyFilters();
}

function showResults() {
    document.getElementById('statsSection').style.display = 'grid';
    document.getElementById('tasksSection').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('loadingSection').style.display = 'none';
}

function showEmptyState() {
    document.getElementById('statsSection').style.display = 'none';
    document.getElementById('tasksSection').style.display = 'none';
    document.getElementById('emptyState').style.display = 'block';
    document.getElementById('loadingSection').style.display = 'none';
}

function showLoading() {
    document.getElementById('statsSection').style.display = 'none';
    document.getElementById('tasksSection').style.display = 'none';
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('loadingSection').style.display = 'block';
}

async function viewTaskDetails(taskId) {
    const task = currentTasks.find(t => t.id === taskId);
    if (!task) {
        showError('Task not found');
        return;
    }
    
    const content = `
        <div class="task-detail-info">
            <h6><i class="fas fa-info-circle me-2"></i>Task Information</h6>
            <p><strong>Title:</strong> ${escapeHtml(task.title)}</p>
            <p><strong>Description:</strong> ${task.description ? escapeHtml(task.description) : 'No description'}</p>
            <p><strong>Type:</strong> <span class="task-type ${getTypeClass(task.taskType)}">${formatTaskType(task.taskType)}</span></p>
            <p><strong>Status:</strong> <span class="task-status ${getStatusClass(task.individualStatus)}">${formatStatus(task.individualStatus)}</span></p>
            <p><strong>Due Date:</strong> <span class="${getDueDateClass(task.dueDateTime, task.individualStatus)}">${formatDateTime(task.dueDateTime)}</span></p>
            <p><strong>Created by:</strong> <span class="creator-badge ${getCreatorClass(task.creator)}">${getCreatorDisplay(task.creator)}</span></p>
            <p><strong>Location:</strong> ${task.location || 'Not specified'}</p>
            <p><strong>Assigned At:</strong> ${formatDateTime(task.assignedAt)}</p>
        </div>
        
        <div class="report-section">
            <h6><i class="fas fa-file-alt me-2"></i>My Report</h6>
            <div class="report-content">
                ${task.report ? `<p>${escapeHtml(task.report)}</p>` : '<div class="no-report">No report submitted yet</div>'}
            </div>
        </div>
    `;
    
    document.getElementById('taskDetailContent').innerHTML = content;
    document.querySelector('#taskDetailModal .modal-title').innerHTML = '<i class="fas fa-tasks me-2"></i>Task Details';
    
    if (taskDetailModal) {
        taskDetailModal.show();
    }
}

function editTask(taskId) {
    window.location.href = '/user/user-task-management';
}


// Utility Functions
function isTaskOverdue(task) {
    const now = new Date();
    const dueDate = new Date(task.dueDateTime);
    return dueDate < now && task.individualStatus !== 'COMPLETED';
}

function getDueDateClass(dueDateTime, status) {
    if (status === 'COMPLETED') return 'due-normal';
    
    const now = new Date();
    const dueDate = new Date(dueDateTime);
    const diffHours = (dueDate - now) / (1000 * 60 * 60);
    
    if (diffHours < 0) return 'due-overdue';
    if (diffHours < 24) return 'due-today';
    if (diffHours < 72) return 'due-upcoming';
    return 'due-normal';
}

function getTimeUntilDue(dueDateTime, status) {
    if (status === 'COMPLETED') return '';
    
    const now = new Date();
    const dueDate = new Date(dueDateTime);
    const diffMs = dueDate - now;
    
    if (diffMs < 0) {
        const overdueDays = Math.ceil(Math.abs(diffMs) / (1000 * 60 * 60 * 24));
        return `<br><small class="due-overdue">${overdueDays} days overdue</small>`;
    } else if (diffMs < 24 * 60 * 60 * 1000) {
        const hours = Math.ceil(diffMs / (1000 * 60 * 60));
        return `<br><small class="due-today">${hours} hours left</small>`;
    } else if (diffMs < 72 * 60 * 60 * 1000) {
        const days = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
        return `<br><small class="due-upcoming">${days} days left</small>`;
    }
    return '';
}

function getStatusClass(status) {
    switch (status) {
        case 'PENDING': return 'status-pending';
        case 'IN_PROGRESS': return 'status-in-progress';
        case 'COMPLETED': return 'status-completed';
        default: return 'status-pending';
    }
}

function getTypeClass(type) {
    switch (type) {
        case 'PERSONAL': return 'type-personal';
        case 'MEETING': return 'type-meeting';
        case 'MAINTENANCE': return 'type-maintenance';
        default: return 'type-personal';
    }
}

function getCreatorClass(creator) {
    if (!creator) return 'creator-user';
    return creator.role === 'ADMIN' ? 'creator-admin' : 'creator-user';
}

function getCreatorDisplay(creator) {
    if (!creator) return 'Unknown';
    return creator.role === 'ADMIN' ? 'Admin' : 'Me';
}

function formatTaskType(type) {
    switch (type) {
        case 'PERSONAL': return 'Personal';
        case 'MEETING': return 'Meeting';
        case 'MAINTENANCE': return 'Maintenance';
        default: return type;
    }
}

function formatStatus(status) {
    switch (status) {
        case 'PENDING': return 'Pending';
        case 'IN_PROGRESS': return 'In Progress';
        case 'COMPLETED': return 'Completed';
        default: return status;
    }
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showError(message) {
    console.error(message);
    alert('Error: ' + message); 
}

function showSuccess(message) {
    console.log(message);
    alert('Success: ' + message); 
}
function getCurrentUser() {
    return {
        employeeNumber: localStorage.getItem('employeeNumber'),
        name: localStorage.getItem('employeeName'),
        role: localStorage.getItem('role'),
        token: localStorage.getItem('token')
    };
}