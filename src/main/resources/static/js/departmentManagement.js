let departments = [];
let machines = [];
let filteredDepartments = [];
let filteredMachines = [];

// Initialize page when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    loadDepartments();
    loadMachines();
    setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
    const machineSearchInput = document.getElementById('machineSearchInput');
    const machineDepartmentFilter = document.getElementById('machineDepartmentFilter');
    const departmentSearchInput = document.getElementById('departmentSearchInput');


    if (departmentSearchInput) {
        departmentSearchInput.addEventListener('change', filterDepartments);
    }
    if (machineSearchInput) {
        machineSearchInput.addEventListener('input', filterMachines);
    }
    if (machineDepartmentFilter) {
        machineDepartmentFilter.addEventListener('change', filterMachines);
    }
}

// Load departments from API
async function loadDepartments() {
    try {
        console.log('Loading departments...');
        const response = await authFetch('/departments/api/departments');
        
        if (response.ok) {
            departments = await response.json();
            filteredDepartments = [...departments];//copy departments list for searching
            filteredDepartments.sort((a, b) => a.id - b.id);//show the list as ascending order
            console.log('Departments loaded:', departments);
            updateDepartmentTable();
            updateDepartmentStats();
            loadDepartmentOptions();
            loadDepartmentFilterOptions();
        } else {
            console.error('Failed to load departments:', response.status);
            showError('Failed to load departments');
        }
    } catch (error) {
        console.error('Error loading departments:', error);
        showError('Error connecting to server');
        departments = [];
        updateDepartmentTable();
        updateDepartmentStats();
    }
}

// Load machines from API
async function loadMachines() {
    try {
        console.log('Loading machines...');
        const response = await authFetch('/departments/api/machines');
        
        if (response.ok) {
            machines = await response.json();
            filteredMachines = [...machines];
            console.log('Machines loaded:', machines);
            updateMachineTable();
            updateMachineStats();
        } else {
            console.error('Failed to load machines:', response.status);
            showError('Failed to load machines');
        }
    } catch (error) {
        console.error('Error loading machines:', error);
        showError('Error connecting to server');
        machines = [];
        filteredMachines = [];
        updateMachineTable();
        updateMachineStats();
    }
}

// Update department statistics
function updateDepartmentStats() {
    const totalDepartments = departments.length;
    const totalEmployeesInDepts = departments.reduce((sum, dept) => sum + (dept.employeeCount || 0), 0);
    
    const totalDepartmentsEl = document.getElementById('totalDepartments');
    const totalEmployeesInDeptsEl = document.getElementById('totalEmployeesInDepts');
    
    if (totalDepartmentsEl) totalDepartmentsEl.textContent = totalDepartments;
    if (totalEmployeesInDeptsEl) totalEmployeesInDeptsEl.textContent = totalEmployeesInDepts;
}

// Update machine statistics
function updateMachineStats() {
    const totalMachines = machines.length;
    const machinesInDepts = machines.filter(machine => machine.department).length;//only count allocated machines
    
    const totalMachinesEl = document.getElementById('totalMachines');
    const machinesInDeptsEl = document.getElementById('machinesInDepts');
    
    if (totalMachinesEl) totalMachinesEl.textContent = totalMachines;
    if (machinesInDeptsEl) machinesInDeptsEl.textContent = machinesInDepts;
}

// Update department table
function updateDepartmentTable() {
    const tableBody = document.getElementById('departmentTableBody');
    tableBody.innerHTML = '';//clean the table
    
    if (filteredDepartments.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="7" class="text-center py-4">
                <div class="empty-state">
                    <i class="fas fa-building fa-3x text-muted mb-3"></i>
                    <h4>No departments found</h4>
                    <p class="text-muted">Start by adding your first department</p>
                </div>
            </td>
        `;
        tableBody.appendChild(row);
        return;
    }
    
    filteredDepartments.forEach(dept => {
        const row = document.createElement('tr');
        const createdDate = dept.createdAt ? new Date(dept.createdAt).toLocaleDateString() : 'N/A';
        
        row.innerHTML = `
            <td>${dept.id}</td>
            <td><strong>${dept.name}</strong></td>
            <td>${dept.description || 'No description'}</td>
            <td>
                <span class="badge bg-primary">${dept.employeeCount || 0}</span>
            </td>
            <td>
                <span class="badge bg-secondary">${dept.machineCount || 0}</span>
            </td>
            <td>${createdDate}</td>
            <td>
                <button class="btn btn-sm btn-outline-warning me-1" onclick="editDepartment(${dept.id})" title="Edit Department">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteDepartment(${dept.id})" title="Delete Department">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(row);//insert into table
    });
}

// Update machine table
function updateMachineTable() {
    const tableBody = document.getElementById('machineTableBody');
    tableBody.innerHTML = '';
    
    if (filteredMachines.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="5" class="text-center py-4">
                <div class="empty-state">
                    <i class="fas fa-cogs fa-3x text-muted mb-3"></i>
                    <h4>No machines found</h4>
                    <p class="text-muted">Start by adding your first machine</p>
                </div>
            </td>
        `;
        tableBody.appendChild(row);
        return;
    }
    
    filteredMachines.sort((a,b)=>{
        const getNumber = (id) => {
            const match=id.match(/\d+/);
            return match?parseInt(match[0]):0;
        };
        return getNumber(a.id) - getNumber(b.id);
    })
    .forEach(machine => {
        const row = document.createElement('tr');
        const createdDate = machine.createdAt ? new Date(machine.createdAt).toLocaleDateString() : 'N/A';
        const departmentName = machine.department ? machine.department.name : 'No Department';
        const departmentClass = machine.department ? 'bg-success' : 'bg-warning';
        
        row.innerHTML = `
            <td><strong>${machine.id}</strong></td>
            <td>${machine.name}</td>
            <td>
                <span class="badge ${departmentClass}">${departmentName}</span>
            </td>
            <td>${createdDate}</td>
            <td>
                <button class="btn btn-sm btn-outline-warning me-1" onclick="editMachine('${machine.id}')" title="Edit Machine">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteMachine('${machine.id}')" title="Delete Machine">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });
}

//load department filter options
function loadDepartmentFilterOptions() {
    const departmentSearchInput = document.getElementById('departmentSearchInput');
    
    if (departmentSearchInput) {
        departmentSearchInput.innerHTML = '<option value="">All Departments</option>';
        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;
            option.textContent = dept.name;
            departmentSearchInput.appendChild(option);
        });
    }
}

// Load department options for selects
function loadDepartmentOptions() {
    const machineDepartmentFilter = document.getElementById('machineDepartmentFilter');
    const addMachineDepartment = document.getElementById('addMachineDepartment');
    const editMachineDepartment = document.getElementById('editMachineDepartment');
    
    // Update filter dropdown
    if (machineDepartmentFilter) {
        machineDepartmentFilter.innerHTML = '<option value="">All Departments</option>';
        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;
            option.textContent = dept.name;
            machineDepartmentFilter.appendChild(option);
        });
    }
    
    // Update add machine department dropdown
    if (addMachineDepartment) {
        addMachineDepartment.innerHTML = '<option value="">No Department</option>';
        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;
            option.textContent = dept.name;
            addMachineDepartment.appendChild(option);
        });
    }
    
    // Update edit machine department dropdown
    if (editMachineDepartment) {
        editMachineDepartment.innerHTML = '<option value="">No Department</option>';
        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;
            option.textContent = dept.name;
            editMachineDepartment.appendChild(option);
        });
    }
}

//Fiter departments
function filterDepartments() {
    const selectedDepartmentId = document.getElementById('departmentSearchInput').value;
    
    if (!selectedDepartmentId) {
        filteredDepartments = [...departments];
    } else {
        filteredDepartments = departments.filter(dept => {
            return dept.id.toString() === selectedDepartmentId;
        });
    }
    filteredDepartments.sort((a, b) => a.id - b.id);
    updateDepartmentTable();
}


// Filter machines
function filterMachines() {
    const searchTerm = document.getElementById('machineSearchInput').value.toLowerCase();
    const departmentFilter = document.getElementById('machineDepartmentFilter').value;
    
    filteredMachines = machines.filter(machine => {
        const matchesSearch = machine.id.toLowerCase().includes(searchTerm) || 
                            machine.name.toLowerCase().includes(searchTerm);
        const matchesDepartment = !departmentFilter || 
                                (machine.department && machine.department.id.toString() === departmentFilter);
        
        return matchesSearch && matchesDepartment;
    });
    filteredMachines.sort((a, b) => {
        const getNumber = (id) => {
            const match = id.match(/\d+/);
            return match ? parseInt(match[0]) : 0;
        };
        return getNumber(a.id) - getNumber(b.id);
    });
    updateMachineTable();
}

// Show add department modal
function showAddDepartmentModal() {
    document.getElementById('addDepartmentName').value = '';
    document.getElementById('addDepartmentDescription').value = '';
    
    const modal = new bootstrap.Modal(document.getElementById('addDepartmentModal'));//create bootstrap modal object
    modal.show();
}

// Add department
async function addDepartment() {
    const name = document.getElementById('addDepartmentName').value.trim();
    const description = document.getElementById('addDepartmentDescription').value.trim();
    
    if (!name) {
        alert('Please enter department name');
        return;
    }
    
    // Check if department name already exists
    if (departments.find(dept => dept.name.toLowerCase() === name.toLowerCase())) {
        alert('Department name already exists');
        return;
    }
    
    const newDepartment = {
        name: name,
        description: description
    };
    
    try {
        const response = await authFetch('/departments/api/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newDepartment)//object -> JSON
        });

        if (response.ok) {
            alert('Department added successfully!');
            await loadDepartments();
            await loadMachines(); // Reload machines to update department info
            
            const modal = bootstrap.Modal.getInstance(document.getElementById('addDepartmentModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to add department: ' + errorText);
        }
    } catch (error) {
        console.error('Error adding department:', error);
        alert('Error adding department. Please try again.');
    }
}

// Edit department
function editDepartment(departmentId) {
    const department = departments.find(dept => dept.id === departmentId);
    if (!department) return;
    
    document.getElementById('editDepartmentId').value = department.id;
    document.getElementById('editDepartmentName').value = department.name;
    document.getElementById('editDepartmentDescription').value = department.description || '';
    
    const modal = new bootstrap.Modal(document.getElementById('editDepartmentModal'));
    modal.show();
}

// Update department
async function updateDepartment() {
    const departmentId = document.getElementById('editDepartmentId').value;
    const name = document.getElementById('editDepartmentName').value.trim();
    const description = document.getElementById('editDepartmentDescription').value.trim();
    
    if (!name) {
        alert('Please enter department name');
        return;
    }
    
    // Check if department name already exists (excluding current department)
    if (departments.find(dept => dept.name.toLowerCase() === name.toLowerCase() && dept.id.toString() !== departmentId)) {
        alert('Department name already exists');
        return;
    }
    
    const updatedDepartment = {
        name: name,
        description: description
    };
    
    try {
        const response = await authFetch(`/departments/api/update/${departmentId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedDepartment)
        });

        if (response.ok) {
            alert('Department updated successfully!');
            await loadDepartments();
            await loadMachines(); // Reload machines to update department info
            
            const modal = bootstrap.Modal.getInstance(document.getElementById('editDepartmentModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to update department: ' + errorText);
        }
    } catch (error) {
        console.error('Error updating department:', error);
        alert('Error updating department. Please try again.');
    }
}

// Delete department
async function deleteDepartment(departmentId) {
    const department = departments.find(dept => dept.id === departmentId);
    if (!department) return;
    
    const employeeCount = department.employeeCount || 0;
    const machineCount = department.machineCount || 0;
    
    let confirmMessage = `Are you sure you want to delete department "${department.name}"?`;
    if (employeeCount > 0 || machineCount > 0) {
        confirmMessage += `\n\nThis will affect:`;
        if (employeeCount > 0) {
            confirmMessage += `\n- ${employeeCount} employee(s) will be moved to "No Department"`;
        }
        if (machineCount > 0) {
            confirmMessage += `\n- ${machineCount} machine(s) will be moved to "No Department"`;
        }
    }
    
    if (confirm(confirmMessage)) {
        try {
            const response = await authFetch(`/departments/api/delete/${departmentId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                alert('Department deleted successfully!');
                await loadDepartments();
                await loadMachines(); // Reload machines to update department info
            } else {
                const errorText = await response.text();
                alert('Failed to delete department: ' + errorText);
            }
        } catch (error) {
            console.error('Error deleting department:', error);
            alert('Error deleting department. Please try again.');
        }
    }
}

// Show add machine modal
async function showAddMachineModal() {
    try {
        const response = await authFetch('/departments/api/machines/next-id');
        const nextId = await response.text();
        document.getElementById('addMachineId').value = nextId;
    } catch (error) {
        console.error('Failed to authFetch next machine ID:', error);
        document.getElementById('addMachineId').value = 'M001';
    }
    document.getElementById('addMachineName').value = '';
    document.getElementById('addMachineDepartment').value = '';
    
    const modal = new bootstrap.Modal(document.getElementById('addMachineModal'));
    modal.show();
}

// Add machine
async function addMachine() {
    const machineId = document.getElementById('addMachineId').value.trim();
    const machineName = document.getElementById('addMachineName').value.trim();
    const departmentId = document.getElementById('addMachineDepartment').value;
    
    if (!machineId || !machineName) {
        alert('Please fill in all required fields');
        return;
    }
    
    // Check if machine ID already exists
    if (machines.find(machine => machine.id === machineId)) {
        alert('Machine ID already exists');
        return;
    }
    
    const newMachine = {
        id: machineId,
        name: machineName,
        departmentId: departmentId ? parseInt(departmentId) : null
    };
    
    try {
        const response = await authFetch('/departments/api/machines/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newMachine)
        });

        if (response.ok) {
            alert('Machine added successfully!');
            await loadMachines();
            await loadDepartments(); // Reload departments to update machine count
            
            const modal = bootstrap.Modal.getInstance(document.getElementById('addMachineModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to add machine: ' + errorText);
        }
    } catch (error) {
        console.error('Error adding machine:', error);
        alert('Error adding machine. Please try again.');
    }
}

// Edit machine
function editMachine(machineId) {
    const machine = machines.find(m => m.id === machineId);
    if (!machine) return;
    
    document.getElementById('editMachineId').value = machine.id;
    document.getElementById('editMachineIdDisplay').value = machine.id;
    document.getElementById('editMachineName').value = machine.name;
    document.getElementById('editMachineDepartment').value = machine.department ? machine.department.id : '';
    
    const modal = new bootstrap.Modal(document.getElementById('editMachineModal'));
    modal.show();
}

// Update machine
async function updateMachine() {
    const machineId = document.getElementById('editMachineId').value;
    const machineName = document.getElementById('editMachineName').value.trim();
    const departmentId = document.getElementById('editMachineDepartment').value;
    
    if (!machineName) {
        alert('Please enter machine name');
        return;
    }
    
    const updatedMachine = {
        name: machineName,
        departmentId: departmentId ? parseInt(departmentId) : null
    };
    
    try {
        const response = await authFetch(`/departments/api/machines/update/${machineId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedMachine)//JavaScript objecy -> JSON object
        });

        if (response.ok) {
            alert('Machine updated successfully!');
            await loadMachines();
            await loadDepartments(); // Reload departments to update machine count
            
            const modal = bootstrap.Modal.getInstance(document.getElementById('editMachineModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to update machine: ' + errorText);
        }
    } catch (error) {
        console.error('Error updating machine:', error);
        alert('Error updating machine. Please try again.');
    }
}

// Delete machine
async function deleteMachine(machineId) {
    const machine = machines.find(m => m.id === machineId);
    if (!machine) return;
    
    if (confirm(`Are you sure you want to delete machine "${machine.name}" (${machineId})?`)) {
        try {
            const response = await authFetch(`/departments/api/machines/delete/${machineId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                alert('Machine deleted successfully!');
                await loadMachines();
                await loadDepartments(); // Reload departments to update machine count
            } else {
                const errorText = await response.text();
                alert('Failed to delete machine: ' + errorText);
            }
        } catch (error) {
            console.error('Error deleting machine:', error);
            alert('Error deleting machine. Please try again.');
        }
    }
}

// Utility function to show error messages
function showError(message) {
    console.error(message);
}