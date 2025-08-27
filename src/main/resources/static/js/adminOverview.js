let currentTasks = [];
let departments = [];
let allEmployees = [];
let taskDetailModal;

document.addEventListener('DOMContentLoaded', function() {
    taskDetailModal = new bootstrap.Modal(document.getElementById('taskDetailModal'));
    initializePage();
});

function initializePage() {
    loadDepartments();
    loadAllEmployees();
    setupEventListeners();
    onDepartmentChange();
}

function setupEventListeners() {
    const departmentFilter = document.getElementById('departmentFilter');
    if (departmentFilter) {
        departmentFilter.addEventListener('change', onDepartmentChange);
    }
}

async function loadDepartments() {
    try {
        const response = await authFetch('/tasks/api/departments');
        if (response.ok) {
            departments = await response.json();
            populateDepartmentFilter();
        }
    } catch (error) {
        console.error('Error loading departments:', error);
    }
}

async function loadAllEmployees() {
    try {
        const response = await authFetch('/tasks/api/employees');
        if (response.ok) {
            allEmployees = await response.json();
        }
    } catch (error) {
        console.error('Error loading employees:', error);
    }
}

function populateDepartmentFilter() {
    const departmentFilter = document.getElementById('departmentFilter');
    departmentFilter.innerHTML = '<option value="">All Departments</option>';//default al departments
    
    departments.forEach(dept => {
        const option = document.createElement('option');
        option.value = dept.id;
        option.textContent = dept.name;
        departmentFilter.appendChild(option);
    });
}

async function onDepartmentChange() {
    const departmentId = document.getElementById('departmentFilter').value;
    const employeeFilter = document.getElementById('employeeFilter');
    
    employeeFilter.innerHTML = '<option value="">All Employees</option>';
    
    if (departmentId) {
        try {
            const response = await authFetch(`/tasks/api/departments/${departmentId}/employees`);
            if (response.ok) {
                const employees = await response.json();
                employees.forEach(emp => {
                    const option = document.createElement('option');
                    option.value = emp.id;
                    option.textContent = `${emp.name} (${emp.employeeNumber})`;
                    employeeFilter.appendChild(option);
                });//list the employees in the specific department
            }
        } catch (error) {
            console.error('Error loading department employees:', error);
        }
    } else {
        // Show all employees
        allEmployees.forEach(emp => {
            const option = document.createElement('option');
            option.value = emp.id;
            option.textContent = `${emp.name} (${emp.employeeNumber}) - ${emp.department || 'No Dept'}`;
            employeeFilter.appendChild(option);
        });
    }
}

async function applyFilters() {
    const filters = {
        departmentId: document.getElementById('departmentFilter').value,
        employeeId: document.getElementById('employeeFilter').value,
        status: document.getElementById('statusFilter').value,
        taskType: document.getElementById('typeFilter').value,
        startDate: document.getElementById('startDateFilter').value,
        endDate: document.getElementById('endDateFilter').value,
        search: document.getElementById('searchInput').value.trim(),
        creatorType: document.getElementById('creatorFilter').value,
        overdueFilter: document.getElementById('overdueFilter').value
    };

    try {
        showLoading();
        const queryParams = new URLSearchParams();
        
        Object.keys(filters).forEach(key => {
            if (filters[key]) {
                queryParams.append(key, filters[key]);
            }
        });

        const response = await authFetch(`/tasks/api/admin/overview?${queryParams.toString()}`);
        
        if (response.ok) {
            const data = await response.json();
            currentTasks = data.tasks;
            let filteredTasks = filterAndSortTasks(data.tasks, filters.overdueFilter);
            updateStatistics(data.statistics);
            updateTasksTable(filteredTasks);
            showResults();
        } else {
            showError('Failed to load task data');
        }
    } catch (error) {
        console.error('Error applying filters:', error);
        showError('Error loading data');
    }
}

function updateStatistics(stats) {
    document.getElementById('totalTasks').textContent = stats.totalTasks || 0;
    document.getElementById('pendingTasks').textContent = stats.pendingTasks || 0;
    document.getElementById('inProgressTasks').textContent = stats.inProgressTasks || 0;
    document.getElementById('completedTasks').textContent = stats.completedTasks || 0;
    document.getElementById('overdueTasks').textContent = stats.overdueTasks || 0;

}

function updateTasksTable(tasks) {
    const tableBody = document.getElementById('tasksTableBody');
    tableBody.innerHTML = '';
    
    if (tasks.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="8" class="text-center py-4">No tasks found</td>';
        tableBody.appendChild(row);
        return;
    }
    
    tasks.forEach(task => {
        const row = document.createElement('tr');
        const statusClass = getStatusClass(task.status);
        const typeClass = getTypeClass(task.taskType);
        
        const now = new Date();
        const dueDate = new Date(task.dueDateTime);
        const isOverdue = dueDate < now && task.status !== 'COMPLETED';
        
        if (isOverdue) {
            row.classList.add('table-danger'); 
        }

        row.innerHTML = `
            <td><strong>${escapeHtml(task.title)}</strong></td>
            <td><span class="task-type ${typeClass}">${formatTaskType(task.taskType)}</span></td>
            <td>${task.creator ? task.creator.name : 'Unknown'}</td>
            <td>${getAssignedToDisplay(task)}</td>
            <td><span class="task-status ${statusClass}">${formatStatus(task.status)}</span></td>
            <td class="${isOverdue ? 'text-danger fw-bold' : ''}">
                ${formatDateTime(task.dueDateTime)}
                ${isOverdue ? '<br><small>(' + getOverdueDays(task.dueDateTime) + ')</small>' : ''}
            </td>
            <td>${formatDateTime(task.createdAt)}</td>
            <td>${getActionButtons(task)}</td>
        `;
        tableBody.appendChild(row);
    });
}

function getOverdueDays(dueDateTime) {
    const now = new Date();
    const dueDate = new Date(dueDateTime);
    const diffTime = now - dueDate;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return `${diffDays} day${diffDays > 1 ? 's' : ''} overdue`;
}

function getAssignedToDisplay(task) {
    if (task.department && task.assignedEmployees.length > 1) {
        return `${task.department.name} (${task.assignedEmployees.length} employees)`;
    } else if (task.assignedEmployees && task.assignedEmployees.length > 0) {
        return task.assignedEmployees.map(emp => emp.name).join(', ');
    }
    return 'Not assigned';
}

function getActionButtons(task) {
    if (task.department && task.assignedEmployees.length > 1) {
        return `<button class="btn btn-sm btn-info" onclick="viewTaskDetails(${task.id})">View Details</button>`;
    } else {
        return `<button class="btn btn-sm btn-primary" onclick="viewTaskReport(${task.id})">View Report</button>`;
    }
}

async function viewTaskDetails(taskId) {
    try {
        const response = await authFetch(`/tasks/api/admin/task-details/${taskId}`);
        if (response.ok) {
            const data = await response.json();
            displayTaskDetails(data);
        } else {
            showError('Failed to load task details');
        }
    } catch (error) {
        console.error('Error loading task details:', error);
        showError('Error loading task details');
    }
}

async function viewTaskReport(taskId) {
    try {
        const response = await authFetch(`/tasks/api/admin/task-details/${taskId}`);
        if (response.ok) {
            const data = await response.json();
            displayTaskReport(data);
        } else {
            showError('Failed to load task report');
        }
    } catch (error) {
        console.error('Error loading task report:', error);
        showError('Error loading task report');
    }
}

function displayTaskReport(data) {
    if (!data || !data.task) {
        console.error('Invalid data provided to displayTaskReports');
        showError('Task data is missing');
        return;
    }
    const task = data.task;
    const assignments = data.assignments;
    
    if (assignments.length === 1) {
        const assignment = assignments[0];
        let content = `
            <div class="task-info">
                <h6><strong>Task:</strong> ${task.title}</h6>
                <p><strong>Description:</strong> ${task.description || 'No description'}</p>
                <p><strong>Employee:</strong> ${assignment.employee} (${assignment.employeeNumber})</p>
                <p><strong>Status:</strong> <span class="badge ${getStatusClass(assignment.individualStatus)}">${formatStatus(assignment.individualStatus)}</span></p>
                <p><strong>Due Date:</strong> ${formatDateTime(task.dueDateTime)}</p>
            </div>
            <hr>
            <h6>Report:</h6>
            <div class="report-content">
                ${assignment.report ? `<p>${assignment.report}</p>` : '<p class="text-muted">No report submitted yet</p>'}
            </div>
        `;
        
        document.getElementById('taskDetailContent').innerHTML = content;
        if (taskDetailModal) taskDetailModal.show();
    } else {

        displayTaskDetails(data);
    }
}

function displayTaskDetails(data) {
    if (!data || !data.task) {
        console.error('Invalid data provided to displayTaskDetails');
        showError('Task data is missing');
        return;
    }
    const task = data.task;
    const assignments = data.assignments;
    
    let content = `
        <div class="task-info">
            <h6><strong>Task:</strong> ${task.title}</h6>
            <p><strong>Description:</strong> ${task.description || 'No description'}</p>
            <p><strong>Due Date:</strong> ${formatDateTime(task.dueDateTime)}</p>
            <p><strong>Creator:</strong> ${task.creator ? task.creator.name : 'Unknown'}</p>
        </div>
        <hr>
        <h6>Employee Progress:</h6>
        <div class="table-responsive">
            <table class="table table-sm">
                <thead>
                    <tr>
                        <th>Employee</th>
                        <th>Status</th>
                        <th>Started</th>
                        <th>Completed</th>
                        <th>Report</th>
                    </tr>
                </thead>
                <tbody>
    `;

    assignments.forEach(assignment => {
        content += `
            <tr>
                <td>${assignment.employee} (${assignment.employeeNumber})</td>
                <td><span class="badge ${getStatusClass(assignment.individualStatus)}">${formatStatus(assignment.individualStatus)}</span></td>
                <td>${assignment.startedAt ? formatDateTime(assignment.startedAt) : '-'}</td>
                <td>${assignment.completedAt ? formatDateTime(assignment.completedAt) : '-'}</td>
                <td>
                    ${assignment.report ? 
                        `<button class="btn btn-sm btn-primary" onclick="showEmployeeReport(${task.id}, '${assignment.employee}', '${assignment.employeeNumber}', '${assignment.individualStatus}', '${assignment.report ? assignment.report.replace(/'/g, "\\'").replace(/"/g, '\\"') : ''}')">
                            <i class="fas fa-file-alt"></i> View Report
                        </button>` 
                        : '<span class="text-muted">No report</span>'
                    }
            </tr>
        `;
    });
    
    content += `
                </tbody>
            </table>
        </div>
    `;
    
    document.getElementById('taskDetailContent').innerHTML = content;
    if (taskDetailModal) taskDetailModal.show();
}

function clearFilters() {
    document.getElementById('departmentFilter').value = '';
    document.getElementById('employeeFilter').value = '';
    document.getElementById('statusFilter').value = '';
    document.getElementById('typeFilter').value = '';
    document.getElementById('startDateFilter').value = '';
    document.getElementById('endDateFilter').value = '';
    document.getElementById('searchInput').value = '';
    document.getElementById('creatorFilter').value = '';
    document.getElementById('overdueFilter').value = '';
    
    // Reset employee filter to show all employees
    onDepartmentChange();
    
    hideResults();
}

function showResults() {
    document.getElementById('statsSection').style.display = 'grid';
    document.getElementById('tableSection').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';
}

function hideResults() {
    document.getElementById('statsSection').style.display = 'none';
    document.getElementById('tableSection').style.display = 'none';
    document.getElementById('emptyState').style.display = 'block';
}

function showLoading() {
    const tableBody = document.getElementById('tasksTableBody');
    tableBody.innerHTML = `
        <tr>
            <td colspan="8" class="text-center py-4">
                <div class="loading">
                    <i class="fas fa-spinner fa-spin"></i>
                    <p class="mt-2">Loading...</p>
                </div>
            </td>
        </tr>
    `;
    showResults();
}

function viewFullReport(reportText, employeeName) {
    const content = `
        <div class="report-details">
            <h6><strong>Employee:</strong> ${employeeName}</h6>
            <hr>
            <h6>Full Report:</h6>
            <div class="report-content" style="max-height: 400px; overflow-y: auto; padding: 15px; background-color: #f8f9fa; border-radius: 5px;">
                <p style="white-space: pre-wrap;">${reportText}</p>
            </div>
        </div>
    `;
    
    document.getElementById('taskDetailContent').innerHTML = content;
    document.getElementById('taskDetailModalLabel').textContent = 'Task Report Details';
    
    if (taskDetailModal) taskDetailModal.show();
}


function displaySingleReportInsideModal(task, assignment) {
    if (!assignment || !task) {
        console.error('Missing data for task or assignment');
        showError('Data missing');
        return;
    }
    
    const content = `
        <div class="task-info">
            <h6><strong>Task:</strong> ${task.title}</h6>
            <p><strong>Description:</strong> ${task.description || 'No description'}</p>
            <p><strong>Employee:</strong> ${assignment.employee} (${assignment.employeeNumber})</p>
            <p><strong>Status:</strong> <span class="badge ${getStatusClass(assignment.individualStatus)}">${formatStatus(assignment.individualStatus)}</span></p>
            <p><strong>Due Date:</strong> ${formatDateTime(task.dueDateTime)}</p>
        </div>
        <hr>
        <h6>Report:</h6>
        <div class="report-content">
            ${assignment.report ? `<p style="white-space: pre-wrap;">${assignment.report}</p>` : '<p class="text-muted">No report submitted yet</p>'}
        </div>
        <hr>
        <button class="btn btn-secondary" onclick="backToTaskDetails(${task.id})">
            <i class="fas fa-arrow-left"></i> Back to Details
        </button>
    `;
    
    document.getElementById('taskDetailContent').innerHTML = content;
    document.getElementById('taskDetailModalLabel').textContent = 'Employee Report';
}


function showEmployeeReport(taskId, employeeName, employeeNumber, status, report) {

    const task = currentTasks.find(t => t.id == taskId) || { id: taskId, title: 'Task', description: '', dueDateTime: '' };
    
    const assignment = {
        employee: employeeName,
        employeeNumber: employeeNumber,
        individualStatus: status,
        report: report
    };
    
    displaySingleReportInsideModal(task, assignment);
}

function backToTaskDetails(taskId) {
    viewTaskDetails(taskId);
}


function exportResults() {
    alert('Export functionality not implemented yet');
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
        case 'PERSONAL': return 'type-personal';
        case 'MEETING': return 'type-meeting';
        case 'MAINTENANCE': return 'type-maintenance';
        default: return 'type-personal';
    }
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

function filterAndSortTasks(tasks, overdueFilter) {
    const now = new Date();
    let filteredTasks = [...tasks];
    
    if (overdueFilter === 'show') {
        filteredTasks = filteredTasks.filter(task => {
            const dueDate = new Date(task.dueDateTime);
            return dueDate < now && task.status !== 'COMPLETED';
        });
    } else if (overdueFilter === 'hide') {
        filteredTasks = filteredTasks.filter(task => {
            const dueDate = new Date(task.dueDateTime);
            return dueDate >= now || task.status === 'COMPLETED';
        });
    }
    

    filteredTasks.sort((a, b) => {
        const now = new Date();
        const aDueDate = new Date(a.dueDateTime);
        const bDueDate = new Date(b.dueDateTime);
        
        const aOverdue = aDueDate < now && a.status !== 'COMPLETED';
        const bOverdue = bDueDate < now && b.status !== 'COMPLETED';
        
        if (aOverdue && !bOverdue) return -1;
        if (!aOverdue && bOverdue) return 1;
        
        return aDueDate - bDueDate;
    });
    
    return filteredTasks;
}

function calculateStatistics(tasks) {
    const now = new Date();
    const stats = {
        totalTasks: tasks.length,
        pendingTasks: 0,
        inProgressTasks: 0,
        completedTasks: 0,
        overdueTasks: 0
    };
    
    tasks.forEach(task => {
        switch (task.status) {
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
        
        const dueDate = new Date(task.dueDateTime);
        if (dueDate < now && task.status !== 'COMPLETED') {
            stats.overdueTasks++;
        }
    });
    
    return stats;
}