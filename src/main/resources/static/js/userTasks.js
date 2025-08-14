let userTasks = [];
let currentPage = 1;
const itemsPerPage = 10;
let filteredTasks = [];
let currentEmployeeId = null; 

// Initialize page when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializePage();
});

function initializePage() {
    const user = getCurrentUser();
    if (user && user.employeeNumber) {
        currentEmployeeId = user.employeeNumber.toString().padStart(4, '0');
        console.log('Current employee ID:', currentEmployeeId);
        
        loadUserTasks();
        setupEventListeners();
    } else {
        console.error('No user found in localStorage');
        showError('Please log in again');
        window.location.href = '/login';
    }
}

// Load user tasks from API
async function loadUserTasks() {
    try {
        console.log('Loading user tasks...');
        showLoading();
        
        const response = await authFetch(`/tasks/api/user/${currentEmployeeId}/tasks`);
        
        if (response.ok) {
            userTasks = await response.json();
            filteredTasks = [...userTasks];
            
            console.log('Loaded tasks:', userTasks);
            
            updateTaskStats();
            updateTasksTable();
            updatePagination();
            
        } else {
            console.error('Failed to load tasks:', response.status);
            showError('Failed to load tasks from server');
        }
        
    } catch (error) {
        console.error('Error loading tasks:', error);
        showError('Error connecting to server');
    }
}

// Load user task statistics
async function loadUserTaskStats() {
    try {
        const response = await authFetch(`/tasks/api/user/${currentEmployeeId}/task-stats`);
        
        if (response.ok) {
            const stats = await response.json();
            updateStatsDisplay(stats);
        } else {
            console.error('Failed to load task stats');
        }
        
    } catch (error) {
        console.error('Error loading task stats:', error);
    }
}

function setupEventListeners() {
    const statusFilter = document.getElementById('statusFilter');
    const searchInput = document.getElementById('searchInput');
    
    if (statusFilter) {
        statusFilter.addEventListener('change', filterTasks);
    }
    
    if (searchInput) {
        searchInput.addEventListener('input', filterTasks);
    }
}

function updateTaskStats() {
    const totalTasks = userTasks.length;
    const pendingTasks = userTasks.filter(task => task.individualStatus === 'PENDING').length;
    const inProgressTasks = userTasks.filter(task => task.individualStatus === 'IN_PROGRESS').length;
    const completedTasks = userTasks.filter(task => task.individualStatus === 'COMPLETED').length;
    
    document.getElementById('totalTasks').textContent = totalTasks;
    document.getElementById('pendingTasks').textContent = pendingTasks;
    document.getElementById('inProgressTasks').textContent = inProgressTasks;
    document.getElementById('completedTasks').textContent = completedTasks;
}

function updateStatsDisplay(stats) {
    document.getElementById('totalTasks').textContent = stats.totalTasks || 0;
    document.getElementById('pendingTasks').textContent = stats.pendingTasks || 0;
    document.getElementById('inProgressTasks').textContent = stats.inProgressTasks || 0;
    document.getElementById('completedTasks').textContent = stats.completedTasks || 0;
}

function filterTasks() {
    const statusFilter = document.getElementById('statusFilter').value;
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    
    filteredTasks = userTasks.filter(task => {
        const matchesStatus = !statusFilter || task.individualStatus === statusFilter;
        const matchesSearch = !searchTerm || 
            task.title.toLowerCase().includes(searchTerm) ||
            task.description.toLowerCase().includes(searchTerm);
        
        return matchesStatus && matchesSearch;
    });
    
    currentPage = 1;
    updateTasksTable();
    updatePagination();
}

function updateTasksTable() {
    const tableBody = document.getElementById('tasksTableBody');
    tableBody.innerHTML = '';
    
    // Calculate pagination
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedTasks = filteredTasks.slice(startIndex, endIndex);
    
    if (paginatedTasks.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="5" class="text-center py-4">
                <div class="empty-state">
                    <i class="fas fa-tasks fa-3x mb-3"></i>
                    <h3>No tasks found</h3>
                    <p>You don't have any tasks matching the current filters.</p>
                </div>
            </td>
        `;
        tableBody.appendChild(row);
        return;
    }
    
    paginatedTasks.forEach(task => {
        const row = document.createElement('tr');
        
        const statusClass = getStatusClass(task.individualStatus);
        const typeClass = getTypeClass(task.taskType);
        const dueDateClass = getDueDateClass(task.dueDateTime);
        
        row.innerHTML = `
            <td>
                <div>
                    <h6 class="mb-1">${escapeHtml(task.title)}</h6>
                    <small class="text-muted">${escapeHtml(task.description || '').substring(0, 50)}${task.description && task.description.length > 50 ? '...' : ''}</small>
                </div>
            </td>
            <td>
                <span class="task-type ${typeClass}">${formatTaskType(task.taskType)}</span>
            </td>
            <td>
                <span class="${dueDateClass}">${formatDateTime(task.dueDateTime)}</span>
            </td>
            <td>
                <span class="task-status ${statusClass}">${formatStatus(task.individualStatus)}</span>
            </td>
            <td>
                <button class="btn btn-action btn-view" onclick="viewTaskDetails(${task.id})" title="View Details">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-action btn-edit" onclick="showStatusModal(${task.id})" title="Update Status">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-action btn-report" onclick="showReportModal(${task.id})" title="Submit Report">
                    <i class="fas fa-file-alt"></i>
                </button>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

function updatePagination() {
    const totalPages = Math.ceil(filteredTasks.length / itemsPerPage);
    const paginationControls = document.getElementById('paginationControls');
    const paginationInfo = document.getElementById('paginationInfo');
    
    // Update pagination info
    const startIndex = (currentPage - 1) * itemsPerPage + 1;
    const endIndex = Math.min(currentPage * itemsPerPage, filteredTasks.length);
    paginationInfo.textContent = `Showing ${startIndex} to ${endIndex} of ${filteredTasks.length} entries`;
    
    // Clear existing pagination
    paginationControls.innerHTML = '';
    
    if (totalPages <= 1) return;
    
    // Previous button
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPage === 1 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" onclick="changePage(${currentPage - 1})">Previous</a>`;
    paginationControls.appendChild(prevLi);
    
    // Page numbers
    for (let i = 1; i <= totalPages; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPage ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="changePage(${i})">${i}</a>`;
        paginationControls.appendChild(li);
    }
    
    // Next button
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPage === totalPages ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" onclick="changePage(${currentPage + 1})">Next</a>`;
    paginationControls.appendChild(nextLi);
}

function changePage(page) {
    const totalPages = Math.ceil(filteredTasks.length / itemsPerPage);
    if (page < 1 || page > totalPages) return;
    
    currentPage = page;
    updateTasksTable();
    updatePagination();
}

function viewTaskDetails(taskId) {
    const task = userTasks.find(t => t.id === taskId);
    if (!task) return;
    
    // Populate modal with task details
    document.getElementById('detailTitle').textContent = task.title;
    document.getElementById('detailType').textContent = formatTaskType(task.taskType);
    document.getElementById('detailDueDate').textContent = formatDateTime(task.dueDateTime);
    document.getElementById('detailStatus').textContent = formatStatus(task.individualStatus);
    document.getElementById('detailDescription').textContent = task.description || 'No description';
    document.getElementById('detailLocation').textContent = task.location || 'No location specified';
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('taskDetailModal'));
    modal.show();
}

function showStatusModal(taskId) {
    const task = userTasks.find(t => t.id === taskId);
    if (!task) return;
    
    document.getElementById('updateTaskId').value = taskId;
    document.getElementById('newStatus').value = task.individualStatus;
    
    const modal = new bootstrap.Modal(document.getElementById('statusUpdateModal'));
    modal.show();
}

function showReportModal(taskId) {
    const task = userTasks.find(t => t.id === taskId);
    if (!task) return;
    
    document.getElementById('reportTaskId').value = taskId;
    document.getElementById('reportStatus').value = task.individualStatus;
    document.getElementById('reportText').value = task.report || '';
    
    const modal = new bootstrap.Modal(document.getElementById('reportSubmitModal'));
    modal.show();
}

async function updateTaskStatus() {
    const taskId = document.getElementById('updateTaskId').value;
    const newStatus = document.getElementById('newStatus').value;
    
    try {
        const response = await authFetch(`/tasks/api/user/tasks/${taskId}/status?employeeId=${currentEmployeeId}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                status: newStatus
            })
        });
        
        if (response.ok) {
            showSuccess('Task status updated successfully!');
            
            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('statusUpdateModal'));
            modal.hide();
            
            // Refresh tasks
            await loadUserTasks();
            
        } else {
            const error = await response.text();
            showError('Failed to update task status: ' + error);
        }
        
    } catch (error) {
        console.error('Error updating task status:', error);
        showError('Error updating task status');
    }
}

async function submitTaskReport() {
    const taskId = document.getElementById('reportTaskId').value;
    const status = document.getElementById('reportStatus').value;
    const report = document.getElementById('reportText').value.trim();
    
    if (!report) {
        showError('Please enter a report before submitting');
        return;
    }
    
    try {
        const response = await authFetch(`/tasks/api/user/tasks/${taskId}/report?employeeId=${currentEmployeeId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                report: report,
                status: status
            })
        });
        
        if (response.ok) {
            showSuccess('Task report submitted successfully!');
            
            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('reportSubmitModal'));
            modal.hide();
            
            // Clear form
            document.getElementById('reportText').value = '';
            
            // Refresh tasks
            await loadUserTasks();
            
        } else {
            const error = await response.text();
            showError('Failed to submit report: ' + error);
        }
        
    } catch (error) {
        console.error('Error submitting report:', error);
        showError('Error submitting report');
    }
}

function refreshTasks() {
    loadUserTasks();
}

// Utility functions
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
        case 'MAINTENANCE': return 'type-maintenance';
        case 'INSPECTION': return 'type-inspection';
        case 'CLEANING': return 'type-cleaning';
        case 'REPAIR': return 'type-repair';
        default: return 'type-other';
    }
}

function getDueDateClass(dueDateTime) {
    const now = new Date();
    const dueDate = new Date(dueDateTime);
    const diffDays = Math.ceil((dueDate - now) / (1000 * 60 * 60 * 24));
    
    if (diffDays < 0) return 'due-overdue';
    if (diffDays === 0) return 'due-today';
    if (diffDays <= 3) return 'due-upcoming';
    return 'due-normal';
}

function formatTaskType(type) {
    switch (type) {
        case 'MAINTENANCE': return 'Maintenance';
        case 'INSPECTION': return 'Inspection';
        case 'CLEANING': return 'Cleaning';
        case 'REPAIR': return 'Repair';
        case 'OTHER': return 'Other';
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
    const date = new Date(dateTimeStr);
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showLoading() {
    const tableBody = document.getElementById('tasksTableBody');
    tableBody.innerHTML = `
        <tr>
            <td colspan="5" class="text-center py-4">
                <div class="loading">
                    <i class="fas fa-spinner fa-spin"></i>
                    <p class="mt-2">Loading tasks...</p>
                </div>
            </td>
        </tr>
    `;
}

function showSuccess(message) {
    alert(message); 
}

function showError(message) {
    console.error(message);
    alert('Error: ' + message); 
}
