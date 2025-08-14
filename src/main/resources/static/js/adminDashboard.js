
document.addEventListener('DOMContentLoaded', function() {
    loadDashboardData();
});

async function loadDashboardData() {
    try {
        showLoadingStats();

        const [employeeStats, taskStats] = await Promise.all([
            loadEmployeeStats(),
            loadTaskStats()
        ]);

        updateStatsCards(employeeStats, taskStats);
        
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showErrorStats();
    }
}

async function loadEmployeeStats() {
    try {
        const response = await authFetch('/employees/api/stats');
        if (response.ok) {
            return await response.json();
        } else {
            throw new Error('Failed to load employee stats');
        }
    } catch (error) {
        console.error('Error loading employee stats:', error);
        return { totalEmployees: 0 };
    }
}

async function loadTaskStats() {
    try {
        const response = await authFetch('/tasks/api/stats');
        if (response.ok) {
            return await response.json();
        } else {
            throw new Error('Failed to load task stats');
        }
    } catch (error) {
        console.error('Error loading task stats:', error);
        return { 
            totalTasks: 0, 
            pendingTasks: 0, 
            inProgressTasks: 0, 
            completedTasks: 0, 
            overdueTasks: 0 
        };
    }
}

function updateStatsCards(employeeStats, taskStats) {
    const totalEmployeesEl = document.getElementById('totalEmployees');
    if (totalEmployeesEl) {
        totalEmployeesEl.textContent = employeeStats.totalEmployees || 0;
        totalEmployeesEl.addEventListener('click',() => {
            window.location.href='/employees';
        })
    }
    
    const activeTasksEl = document.getElementById('activeTasks');
    if (activeTasksEl) {
        const activeTasks = (taskStats.pendingTasks || 0) + (taskStats.inProgressTasks || 0);
        activeTasksEl.textContent = activeTasks;
        activeTasksEl.addEventListener('click',() => {
            window.location.href='/tasks';
        })
    }

    const overdueTasksEl = document.getElementById('overdueTasks');
    if (overdueTasksEl) {
        overdueTasksEl.textContent = taskStats.overdueTasks || 0;
        overdueTasksEl.addEventListener('click',() => {
            window.location.href='/tasks';
        })
    }

    const completedTasksEl = document.getElementById('completedTasks');
    if (completedTasksEl) {
        completedTasksEl.textContent = taskStats.completedTasks || 0;
        completedTasksEl.addEventListener('click',() => {
            window.location.href='/tasks';
        })
    }

    
}

function showLoadingStats() {
    const statsElements = ['totalEmployees', 'activeTasks', 'overdueTasks', 'completedTasks'];
    
    statsElements.forEach(elementId => {
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        }
    });
}

function showErrorStats() {
    const statsElements = ['totalEmployees', 'activeTasks', 'overdueTasks', 'completedTasks'];
    
    statsElements.forEach(elementId => {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = 'Error';
            element.parentElement.parentElement.style.opacity = '0.6';
        }
    });
}