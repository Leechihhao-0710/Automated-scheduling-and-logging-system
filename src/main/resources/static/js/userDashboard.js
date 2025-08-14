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
        
        loadDashboardData();
    } else {
        console.error('No user found in localStorage');
        showError('Please log in again');
        window.location.href = '/login';
    }
}

// Load all dashboard data
async function loadDashboardData() {
    try {
        showLoading();
        
        // Load data concurrently
        await Promise.all([
            loadTaskStats(),
            loadTodaysTasks(),
            loadRecentNotifications()
        ]);

    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showError('Error loading dashboard data');
    }
}

// Load task statistics
async function loadTaskStats() {
    try {
        const response = await authFetch(`/tasks/api/user/${currentEmployeeId}/dashboard-stats`);
        
        if (response.ok) {
            const stats = await response.json();
            updateTaskStats(stats);
        } else {
            console.error('Failed to load task stats');
        }
    } catch (error) {
        console.error('Error loading task stats:', error);
    }
}

// Update task statistics display
function updateTaskStats(stats) {
    // Update the stat numbers
    const statElements = document.querySelectorAll('.stat-number');
    if (statElements.length >= 4) {
        statElements[0].textContent = stats.activeTasks || 0;
        statElements[1].textContent = stats.dueToday || 0;
        statElements[2].textContent = stats.overdue || 0;
        statElements[3].textContent = stats.completedThisMonth || 0;
    }
    
    // Alternative approach if you have specific IDs
    const activeTasksEl = document.querySelector('.stat-card:nth-child(1) .stat-number');
    const dueTodayEl = document.querySelector('.stat-card:nth-child(2) .stat-number');
    const overdueEl = document.querySelector('.stat-card:nth-child(3) .stat-number');
    const completedEl = document.querySelector('.stat-card:nth-child(4) .stat-number');
    
    if (activeTasksEl) activeTasksEl.textContent = stats.activeTasks || 0;
    if (dueTodayEl) dueTodayEl.textContent = stats.dueToday || 0;
    if (overdueEl) overdueEl.textContent = stats.overdue || 0;
    if (completedEl) completedEl.textContent = stats.completedThisMonth || 0;
}

// Load today's tasks (due in next 3 days)
async function loadTodaysTasks() {
    try {
        const response = await authFetch(`/tasks/api/user/${currentEmployeeId}/upcoming-tasks`);
        
        if (response.ok) {
            const tasks = await response.json();
            updateTodaysTasks(tasks);
        } else {
            console.error('Failed to load today\'s tasks');
        }
    } catch (error) {
        console.error('Error loading today\'s tasks:', error);
    }
}

// Update today's tasks display
function updateTodaysTasks(tasks) {
    const taskList = document.querySelector('.today-tasks .task-list');
    if (!taskList) return;
    
    taskList.innerHTML = '';
    
    if (tasks.length === 0) {
        taskList.innerHTML = `
            <div class="task-item">
                <div class="task-status">✅</div>
                <div class="task-content">
                    <h4>No upcoming tasks</h4>
                    <p class="task-time">You're all caught up!</p>
                </div>
            </div>
        `;
        return;
    }
    
    tasks.forEach(task => {
        const taskItem = document.createElement('div');
        taskItem.className = 'task-item pending';
        
        const dueDate = new Date(task.dueDateTime);
        const dueDateStr = formatDueTime(dueDate);
        const statusIcon = getTaskStatusIcon(task.individualStatus);
        
        taskItem.innerHTML = `
            <div class="task-status">${statusIcon}</div>
            <div class="task-content">
                <h4>${escapeHtml(task.title)}</h4>
                <p class="task-time">Due: ${dueDateStr}</p>
            </div>
        `;
        
        // Add click event to view task details
        taskItem.addEventListener('click', () => {
            window.location.href = `/user/current-tasks`;
        });
        
        taskList.appendChild(taskItem);
    });
}

// Load recent notifications (tasks assigned in last 3 days)
async function loadRecentNotifications() {
    try {
        const response = await authFetch(`/tasks/api/user/${currentEmployeeId}/recent-assignments`);
        
        if (response.ok) {
            const notifications = await response.json();
            updateRecentNotifications(notifications);
        } else {
            console.error('Failed to load recent notifications');
        }
    } catch (error) {
        console.error('Error loading recent notifications:', error);
    }
}

// Update recent notifications display
function updateRecentNotifications(notifications) {
    const notificationList = document.querySelector('.recent-notifications .notification-list');
    if (!notificationList) return;
    
    notificationList.innerHTML = '';
    
    if (notifications.length === 0) {
        notificationList.innerHTML = `
            <div class="notification-item">
                <div class="notification-icon">📬</div>
                <div class="notification-content">
                    <h4>No recent notifications</h4>
                    <p>No new task assignments in the past 3 days</p>
                    <span class="notification-time">All caught up!</span>
                </div>
            </div>
        `;
        return;
    }
    
    notifications.forEach(notification => {
        const notificationItem = document.createElement('div');
        notificationItem.className = 'notification-item';
        
        const assignedTime = formatRelativeTime(new Date(notification.assignedAt));
        const creatorName = notification.task.creator ? notification.task.creator.name : 'Admin';
        
        notificationItem.innerHTML = `
            <div class="notification-icon">📋</div>
            <div class="notification-content">
                <h4>New task assigned</h4>
                <p>"${escapeHtml(notification.task.title)}" has been assigned to you by ${escapeHtml(creatorName)}</p>
                <span class="notification-time">${assignedTime}</span>
            </div>
        `;
        
        // Add click event to view task
        notificationItem.addEventListener('click', () => {
            window.location.href = `/user/current-tasks`;
        });
        
        notificationList.appendChild(notificationItem);
    });
}

// Utility functions
function getTaskStatusIcon(status) {
    switch (status) {
        case 'PENDING': return '-';
        case 'IN_PROGRESS': return '~';
        case 'COMPLETED': return 'v';
        default: return '○';
    }
}

function formatDueTime(date) {
    const now = new Date();
    const diffDays = Math.ceil((date - now) / (1000 * 60 * 60 * 24));
    
    if (diffDays < 0) {
        return `Overdue by ${Math.abs(diffDays)} day(s)`;
    } else if (diffDays === 0) {
        return date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    } else if (diffDays === 1) {
        return `Tomorrow at ${date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
    } else {
        return `${date.toLocaleDateString()} at ${date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
    }
}

function formatRelativeTime(date) {
    const now = new Date();
    const diffMs = now - date;
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffHours < 1) {
        return 'Just now';
    } else if (diffHours < 24) {
        return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else {
        return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showLoading() {
    // Update stat numbers to show loading
    const statNumbers = document.querySelectorAll('.stat-number');
    statNumbers.forEach(el => {
        el.textContent = 'loading';
    });
}


function showError(message) {
    console.error(message);
    alert('Error: ' + message);
}

function refreshDashboard() {
    loadDashboardData();
}
setInterval(refreshDashboard, 5 * 60 * 1000);