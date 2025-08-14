let tasks = [];
let currentPage = 1;
const itemsPerPage = 5;
let filteredTasks = [];

// Initialize page when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    loadTasksFromDatabase();
});

// Load tasks from database
async function loadTasksFromDatabase() {
    try {
        console.log('authFetching tasks from API...');
        const response = await authFetch('/tasks/api/list');
        
        if (response.ok) {
            const rawTasks = await response.json();
            console.log('Raw API response:', rawTasks);
            
            // Transform API data to match frontend expectations
            tasks = rawTasks.map(task => {
                console.log('Processing task:', task);
                return {
                    id: task.id,
                    title: task.title,
                    description: task.description,
                    taskType: task.taskType,
                    status: task.status,
                    dueDateTime: task.dueDateTime,
                    location: task.location,
                    recurring: task.recurring,
                    recurrenceType: task.recurrenceType,
                    recurrenceInterval: task.recurrenceInterval,
                    recurrenceEndDate: task.recurrenceEndDate,
                    emailReminder: task.emailReminder,
                    reminderDaysBefore: task.reminderDaysBefore,
                    createdAt: task.createdAt,
                    updatedAt: task.updatedAt,
                    creator: task.creator,
                    department: task.department,
                    assignedEmployees: task.assignedEmployees || []
                };
            });
            
            filteredTasks = [...tasks];

            const totalPages = Math.ceil(filteredTasks.length / itemsPerPage);
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
            } else if (totalPages === 0) {
                currentPage = 1;
            }

            initializePage();
            console.log('Transformed tasks:', tasks);
            
        } else {
            const errorText = await response.text();
            console.error('Failed to load tasks:', response.status, response.statusText);
            console.error('Error response:', errorText);
            alert(`Failed to load tasks from server. Status: ${response.status}`);
        }
        
    } catch (error) {
        console.error('Error loading tasks:', error);
        alert(`Error connecting to server: ${error.message}`);
        
        // Initialize page with empty data to prevent further errors
        tasks = [];
        filteredTasks = [];
        currentPage = 1;
        initializePage();
    }
}

function initializePage() {
    console.log('Initializing page with', tasks.length, 'tasks');
    updateStats();
    loadDepartmentOptions();
    updateTaskTable();
    setupEventListeners();
    updatePagination();
}

async function loadDepartmentOptions() {
    try {
        const response = await authFetch('/tasks/api/departments');
        const departments = await response.json();

        // Load department filter
        const filterDepartment = document.getElementById('departmentFilter');
        if (filterDepartment) {
            filterDepartment.innerHTML = '<option value="">All Departments</option>';
            departments.forEach(dept => {
                const option = document.createElement('option');
                option.value = dept.id;
                option.textContent = dept.name;
                filterDepartment.appendChild(option);
            });
        }

        // Load add task department
        const addDepartment = document.getElementById('addTaskDepartment');
        if (addDepartment) {
            addDepartment.innerHTML = '<option value="">Select Department (Optional)</option>';
            departments.forEach(dept => {
                const option = document.createElement('option');
                option.value = dept.id;
                option.textContent = dept.name;
                addDepartment.appendChild(option);
            });
        }

        // Load edit task department
        const editDepartment = document.getElementById('editTaskDepartment');
        if (editDepartment) {
            editDepartment.innerHTML = '<option value="">Select Department (Optional)</option>';
            departments.forEach(dept => {
                const option = document.createElement('option');
                option.value = dept.id;
                option.textContent = dept.name;
                editDepartment.appendChild(option);
            });
        }
        
    } catch (error) {
        console.error('Failed to load departments:', error);
    }
}

async function loadDepartmentEmployees(departmentId, targetSelectId) {
    const targetSelect = document.getElementById(targetSelectId);
    if (!targetSelect) return;

    targetSelect.innerHTML = '';

    if (departmentId) {
        try {
            const response = await authFetch(`/tasks/api/departments/${departmentId}/employees`);
            const employees = await response.json();

            employees.forEach(emp => {
                const option = document.createElement('option');
                option.value = emp.id;
                option.textContent = `${emp.name} (${emp.employeeNumber})`;
                targetSelect.appendChild(option);
            });
        } catch (error) {
            console.error('Failed to load department employees:', error);
        }
    } else {
        // Load all employees if no department selected
        try {
            const response = await authFetch('/tasks/api/employees');
            const employees = await response.json();

            employees.forEach(emp => {
                const option = document.createElement('option');
                option.value = emp.id;
                option.textContent = `${emp.name} (${emp.employeeNumber}) - ${emp.department || 'No Dept'}`;
                targetSelect.appendChild(option);
            });
        } catch (error) {
            console.error('Failed to load all employees:', error);
        }
    }
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchInput');
    const typeFilter = document.getElementById('typeFilter');
    const statusFilter = document.getElementById('statusFilter');
    const departmentFilter = document.getElementById('departmentFilter');
    const selectAll = document.getElementById('selectAll');
    
    if (searchInput) {
        searchInput.addEventListener('input', filterTasks);
    }
    if (typeFilter) {
        typeFilter.addEventListener('change', filterTasks);
    }
    if (statusFilter) {
        statusFilter.addEventListener('change', filterTasks);
    }
    if (departmentFilter) {
        departmentFilter.addEventListener('change', filterTasks);
    }

    if (selectAll) {
        selectAll.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('.task-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
        });
    }
    
    console.log('Event listeners setup completed');
}

async function updateStats() {
    try {
        const response = await authFetch('/tasks/api/stats');
        const stats = await response.json();

        const totalTasksEl = document.getElementById('totalTasks');
        const pendingTasksEl = document.getElementById('pendingTasks');
        const inProgressTasksEl = document.getElementById('inProgressTasks');
        const completedTasksEl = document.getElementById('completedTasks');
        
        if (totalTasksEl) totalTasksEl.textContent = stats.totalTasks || 0;
        if (pendingTasksEl) pendingTasksEl.textContent = stats.pendingTasks || 0;
        if (inProgressTasksEl) inProgressTasksEl.textContent = stats.inProgressTasks || 0;
        if (completedTasksEl) completedTasksEl.textContent = stats.completedTasks || 0;
        
        console.log('Stats updated:', stats);
    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

function filterTasks() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const typeFilter = document.getElementById('typeFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    const departmentFilter = document.getElementById('departmentFilter').value;
    
    filteredTasks = tasks.filter(task => {
        const matchesSearch = task.title.toLowerCase().includes(searchTerm) || 
                            (task.description && task.description.toLowerCase().includes(searchTerm));
        const matchesType = !typeFilter || task.taskType === typeFilter;
        const matchesStatus = !statusFilter || task.status === statusFilter;
        const matchesDepartment = !departmentFilter || 
            (task.department && task.department.id == departmentFilter);
        
        return matchesSearch && matchesType && matchesStatus && matchesDepartment;
    });
    
    currentPage = 1; // Reset to first page when filtering
    updateTaskTable();
    updatePagination();
}

function clearFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('typeFilter').value = '';
    document.getElementById('statusFilter').value = '';
    document.getElementById('departmentFilter').value = '';
    filterTasks();
}

function updateTaskTable() {
    const tableBody = document.getElementById('taskTableBody');
    tableBody.innerHTML = '';
    
    // Calculate start and end indices for pagination
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedTasks = filteredTasks.slice(startIndex, endIndex);
    
    if (paginatedTasks.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="8" class="text-center py-4">
                <i class="fas fa-tasks fa-3x text-muted mb-3"></i>
                <p class="text-muted">No tasks found</p>
            </td>
        `;
        tableBody.appendChild(row);
        return;
    }
    
    paginatedTasks.forEach(task => {
        const row = document.createElement('tr');
        const statusClass = getStatusClass(task.status);
        const typeIcon = getTypeIcon(task.taskType);
        
        row.innerHTML = `
            <td>
                <input type="checkbox" class="form-check-input task-checkbox" value="${task.id}">
            </td>
            <td>
                <strong>${task.title}</strong>
                ${task.description ? `<br><small class="text-muted">${task.description.substring(0, 50)}${task.description.length > 50 ? '...' : ''}</small>` : ''}
            </td>
            <td>
                <span class="badge bg-info">${typeIcon} ${task.taskType}</span>
            </td>
            <td><span class="badge ${statusClass}">${task.status.replace('_', ' ')}</span></td>
            <td>
                ${task.assignedEmployees && task.assignedEmployees.length > 0 
                    ? task.assignedEmployees.map(emp => emp.name).join(', ')
                    : '<span class="text-muted">Not assigned</span>'}
            </td>
            <td>${formatDateTime(task.dueDateTime)}</td>
            <td>${formatDateTime(task.createdAt)}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewTask(${task.id})" title="View Task">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-warning me-1" onclick="editTask(${task.id})" title="Edit Task">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteTask(${task.id})" title="Delete Task">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });
}

function getStatusClass(status) {
    switch (status) {
        case 'PENDING': return 'bg-warning';
        case 'IN_PROGRESS': return 'bg-primary';
        case 'COMPLETED': return 'bg-success';
        default: return 'bg-secondary';
    }
}

function getTypeIcon(type) {
    switch (type) {
        case 'PERSONAL': return '👤';
        case 'MEETING': return '🏢';
        case 'MAINTENANCE': return '🔧';
        default: return '📋';
    }
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
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
    updateTaskTable();
    updatePagination();
}

// Modal Functions
function showAddModal() {
    // Clear form
    document.getElementById('addTaskForm').reset();
    document.getElementById('addTaskRecurring').checked = false;
    toggleRecurrenceOptions();
    
    // Load employees for initial state
    loadDepartmentEmployees('', 'addTaskEmployees');
    
    const modal = new bootstrap.Modal(document.getElementById('addTaskModal'));
    modal.show();
}

function toggleRecurrenceOptions() {
    const isRecurring = document.getElementById('addTaskRecurring').checked;
    const recurrenceType = document.getElementById('addTaskRecurrenceType');
    const recurrenceInterval = document.getElementById('addTaskRecurrenceInterval');
    const recurrenceEndDate = document.getElementById('addTaskRecurrenceEndDate');
    
    recurrenceType.disabled = !isRecurring;
    recurrenceInterval.disabled = !isRecurring;
    recurrenceEndDate.disabled = !isRecurring;
    
    if (!isRecurring) {
        recurrenceType.value = '';
        recurrenceInterval.value = '1';
        recurrenceEndDate.value = '';
    }
    updateRecurrenceOptions();
}

function updateRecurrenceOptions() {
    const recurrenceType = document.getElementById('addTaskRecurrenceType').value;
    const specificSettings = document.getElementById('specificRecurrenceSettings');
    const specificLabel = document.getElementById('specificLabel');
    const specificDay = document.getElementById('addTaskSpecificDay');
    const intervalHelp = document.getElementById('intervalHelp');
    const skipWeekends = document.getElementById('addTaskSkipWeekends');
    
    if (recurrenceType === 'WEEKLY') {
        specificSettings.style.display = 'block';
        specificLabel.textContent = 'On day';
        intervalHelp.textContent = 'weeks';
        specificDay.innerHTML = `
            <option value="">Select day</option>
            <option value="1">Monday</option>
            <option value="2">Tuesday</option>
            <option value="3">Wednesday</option>
            <option value="4">Thursday</option>
            <option value="5">Friday</option>
            <option value="6">Saturday</option>
            <option value="7">Sunday</option>
        `;
        specificDay.disabled = false;
        skipWeekends.disabled = true;
        skipWeekends.checked = false;
    } else if (recurrenceType === 'MONTHLY') {
        specificSettings.style.display = 'block';
        specificLabel.textContent = 'On day';
        intervalHelp.textContent = 'months';
        specificDay.innerHTML = `<option value="">Select day</option>`;
        for (let i = 1; i <= 31; i++) {
            specificDay.innerHTML += `<option value="${i}">${i}</option>`;
        }
        specificDay.disabled = false;
        skipWeekends.disabled = false;
        skipWeekends.checked = true;
    } else {
        specificSettings.style.display = 'none';
        specificDay.disabled = true;
        skipWeekends.disabled = true;
        intervalHelp.textContent = 'weeks';
    }
}

async function addTask() {
    const title = document.getElementById('addTaskTitle').value.trim();
    const description = document.getElementById('addTaskDescription').value.trim();
    const taskType = document.getElementById('addTaskType').value;
    const dueDateTime = document.getElementById('addTaskDueDateTime').value;
    const location = document.getElementById('addTaskLocation').value.trim();
    const departmentId = document.getElementById('addTaskDepartment').value;
    const employeeSelect = document.getElementById('addTaskEmployees');
    const selectedEmployees = Array.from(employeeSelect.selectedOptions).map(option => option.value);
    
    // Validation
    if (!title || !taskType || !dueDateTime) {
        alert('Please fill in all required fields');
        return;
    }

    const taskData = {
        title: title,
        description: description,
        taskType: taskType,
        dueDateTime: dueDateTime,
        location: location,
        departmentId: departmentId ? parseInt(departmentId) : null,
        employeeIds: selectedEmployees.length > 0 ? selectedEmployees : null,
        recurring: document.getElementById('addTaskRecurring').checked,
        //emailReminder: document.getElementById('addTaskEmailReminder').checked,
        //reminderDaysBefore: parseInt(document.getElementById('addTaskReminderDays').value)
    };

    // Add recurrence data if applicable
    if (taskData.recurring) {
        taskData.recurrenceType = document.getElementById('addTaskRecurrenceType').value;
        taskData.recurrenceInterval = parseInt(document.getElementById('addTaskRecurrenceInterval').value);
        const endDate = document.getElementById('addTaskRecurrenceEndDate').value;
        if (endDate) {
            taskData.recurrenceEndDate = endDate;
        }
        const specificDay = document.getElementById('addTaskSpecificDay').value;

        if (taskData.recurrenceType === 'WEEKLY' && specificDay) {
            taskData.recurringDayOfWeek = parseInt(specificDay);
        }
        if (taskData.recurrenceType === 'MONTHLY' && specificDay) {
            taskData.recurringDayOfMonth = parseInt(specificDay);
        }
        taskData.skipWeekends = document.getElementById('addTaskSkipWeekends').checked;
    }

    console.log('Sending new task data:', taskData);

    try {
        const response = await authFetch('/tasks/api/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(taskData)
        });

        if (response.ok) {
            alert('Task created successfully!');
            await loadTasksFromDatabase();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addTaskModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to create task: ' + errorText);
        }

    } catch (error) {
        console.error('Error creating task:', error);
        alert('Error creating task. Please try again.');
    }
}

function viewTask(taskId) {
    const task = tasks.find(t => t.id === taskId);
    if (!task) return;
    
    // You can implement a view modal here
    alert(`Task: ${task.title}\nStatus: ${task.status}\nDue: ${formatDateTime(task.dueDateTime)}\nDescription: ${task.description || 'No description'}`);
}

async function editTask(taskId) {
    const task = tasks.find(t => t.id === taskId);
    if (!task) return;

    // Populate edit form
    document.getElementById('editTaskId').value = task.id;
    document.getElementById('editTaskTitle').value = task.title;
    document.getElementById('editTaskDescription').value = task.description || '';
    document.getElementById('editTaskType').value = task.taskType;
    document.getElementById('editTaskDueDateTime').value = task.dueDateTime.substring(0, 16); // Format for datetime-local
    document.getElementById('editTaskLocation').value = task.location || '';

    // Set department
    if (task.department) {
        document.getElementById('editTaskDepartment').value = task.department.id;
        await loadDepartmentEmployees(task.department.id, 'editTaskEmployees');
    } else {
        document.getElementById('editTaskDepartment').value = '';
        await loadDepartmentEmployees('', 'editTaskEmployees');
    }

    // Select assigned employees
    const employeeSelect = document.getElementById('editTaskEmployees');
    for (let option of employeeSelect.options) {
        const isAssigned = task.assignedEmployees.some(emp => emp.id === option.value);
        option.selected = isAssigned;
    }
    
    const modal = new bootstrap.Modal(document.getElementById('editTaskModal'));
    modal.show();
}

async function updateTask() {
    const taskId = document.getElementById('editTaskId').value;
    const title = document.getElementById('editTaskTitle').value.trim();
    const description = document.getElementById('editTaskDescription').value.trim();
    const taskType = document.getElementById('editTaskType').value;
    const dueDateTime = document.getElementById('editTaskDueDateTime').value;
    const location = document.getElementById('editTaskLocation').value.trim();
    const departmentId = document.getElementById('editTaskDepartment').value;
    const employeeSelect = document.getElementById('editTaskEmployees');
    const selectedEmployees = Array.from(employeeSelect.selectedOptions).map(option => option.value);
    
    // Validation
    if (!title || !taskType || !dueDateTime) {
        alert('Please fill in all required fields');
        return;
    }
    
    const updatedTask = {
        title: title,
        description: description,
        taskType: taskType,
        dueDateTime: dueDateTime,
        location: location,
        departmentId: departmentId ? parseInt(departmentId) : null,
        employeeIds: selectedEmployees.length > 0 ? selectedEmployees : null
    };
    
    console.log('Sending update data:', updatedTask);
    
    try {
        const response = await authFetch(`/tasks/api/update/${taskId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedTask)
        });

        if (response.ok) {
            alert('Task updated successfully!');
            await loadTasksFromDatabase();

            const modal = bootstrap.Modal.getInstance(document.getElementById('editTaskModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to update task: ' + errorText);
        }
        
    } catch (error) {
        console.error('Error updating task:', error);
        alert('Error updating task. Please try again.');
    }
}

async function deleteTask(taskId) {
    const task = tasks.find(t => t.id === taskId);
    if (!task) return;
    
    if (confirm(`Are you sure you want to delete task "${task.title}"?`)) {
        try {
            const response = await authFetch(`/tasks/api/delete/${taskId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                await loadTasksFromDatabase();
                alert('Task deleted successfully!');
            } else {
                const errorText = await response.text();
                alert('Failed to delete task: ' + errorText);
            }
        } catch (error) {
            console.error('Error deleting task:', error);
            alert('Error deleting task. Please try again.');
        }
    }
}