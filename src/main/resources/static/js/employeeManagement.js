let employees = [];
let currentPage = 1;
const itemsPerPage = 5;
let filteredEmployees = [];

// Initialize page when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    loadEmployeesFromDatabase();
});

// Load employees from database
async function loadEmployeesFromDatabase() {
    try {
        console.log('authFetching employees from API...');
        const response = await authFetch('/employees/api/all');
        
        if (response.ok) {
            const rawEmployees = await response.json();
            console.log('Raw API response:', rawEmployees);
            
            // Transform API data to match frontend expectations
            employees = rawEmployees.map(emp => {
                console.log('Processing employee:', emp); // Debug log
                return {
                    id: emp.id,
                    name: emp.name,
                    email: emp.email,
                    department: emp.department ? emp.department.name : 'No Department',
                    role: emp.role === 'ADMIN' ? 'Admin' : 'User',
                    createdDate: emp.createdAt ? emp.createdAt.split('T')[0] : new Date().toISOString().split('T')[0],
                    employeeNumber: emp.employeeNumber,
                    originalRole: emp.role, 
                    assignedMachines: emp.assignedMachines || [],
                    dateOfBirth: emp.dateOfBirth
                };
            });
            
            filteredEmployees = [...employees];

            const totalPages=Math.ceil(filteredEmployees.length/itemsPerPage);
            if(currentPage>totalPages && totalPages>0){
                currentPage=totalPages;
            }else if(totalPages===0){
                currentPage=1;
            }

            initializePage();
            console.log('Transformed employees:', employees);
            
        } else {
            const errorText = await response.text();
            console.error('Failed to load employees:', response.status, response.statusText);
            console.error('Error response:', errorText);
            alert(`Failed to load employees from server. Status: ${response.status}`);
        }
        
    } catch (error) {
        console.error('Error loading employees:', error);
        console.error('Error details:', error.message);
        console.error('Error stack:', error.stack);
        alert(`Error connecting to server: ${error.message}`);
        
        // Initialize page with empty data to prevent further errors
        employees = [];
        filteredEmployees = [];
        currentPage = 1;
        initializePage();
    }
}

function initializePage() {
    console.log('Initializing page with', employees.length, 'employees');
    updateStats();
    loadDepartmentOptions();
    loadMachineOptions();
    updateEmployeeTable();
    setupEventListeners();
    updatePagination();
    setupEmployeeForm();
}

function loadDepartmentOptions() {
    const filterDepartment = document.getElementById('departmentFilter');
    if (!filterDepartment) {
        console.log('filterDepartment element not found');
        return;
    }

    filterDepartment.innerHTML = '<option value="">All Departments</option>';

    const uniqueDepartments = new Set();
    employees.forEach(emp => {
        console.log('Processing employee for departments:', emp.name, 'department:', emp.department); // Debug
        if (emp.department && emp.department !== 'No Department' && emp.department.trim() !== '') {
            uniqueDepartments.add(emp.department.trim());
        }
    });
    
    console.log('Found departments:', Array.from(uniqueDepartments));

    Array.from(uniqueDepartments).sort().forEach(deptName => {
        const option = document.createElement('option');
        option.value = deptName;
        option.textContent = deptName;
        filterDepartment.appendChild(option);
        console.log('Added department option:', deptName);
    });
    
    console.log('Department options loaded successfully');
}

function setupEmployeeForm() {
    // Hide and disable employee ID field since it will be auto-generated
    const employeeIdField = document.getElementById('employeeId');
    if (employeeIdField) {
        const employeeIdContainer = employeeIdField.closest('.mb-3');
        if (employeeIdContainer) {
            employeeIdContainer.style.display = 'none';
        }
    }
    console.log('Employee form setup completed');
}

async function loadMachineOptions() {
    try {
        const response = await authFetch('/employees/api/machines');
        const machines = await response.json();

        const machineFilter = document.getElementById('machineFilter');
        if (machineFilter) {
            machineFilter.innerHTML = '<option value="">All Machines</option>';
            machines.forEach(machine => {
                const option = document.createElement('option');
                option.value = machine.id;
                option.textContent = machine.name;
                machineFilter.appendChild(option);
            });
        }

        const addMachineSelect = document.getElementById('addEmployeeMachines');
        if (addMachineSelect) {
            addMachineSelect.innerHTML = '';
            machines.forEach(machine => {
                const option = document.createElement('option');
                option.value = machine.id;
                option.textContent = machine.name;
                addMachineSelect.appendChild(option);
            });
        }
        
        const editMachineSelect = document.getElementById('editEmployeeMachines');
        if (editMachineSelect) {
            editMachineSelect.innerHTML = '';
            machines.forEach(machine => {
                const option = document.createElement('option');
                option.value = machine.id;
                option.textContent = machine.name;
                editMachineSelect.appendChild(option);
            });
        }
        
    } catch (error) {
        console.error('Failed to load machines:', error);
    }
}

function setupEventListeners() {
    const searchEmployee = document.getElementById('searchInput');
    const filterDepartment = document.getElementById('departmentFilter');
    const filterRole = document.getElementById('roleFilter');
    const filterMachine = document.getElementById('machineFilter');
    const selectAll = document.getElementById('selectAll');
    
    if (searchEmployee) {
        searchEmployee.addEventListener('input', filterEmployees);
    }
    if (filterDepartment) {
        filterDepartment.addEventListener('change', filterEmployees);
    }
    if (filterRole) {
        filterRole.addEventListener('change', filterEmployees);
    }
    if (filterMachine) { 
        filterMachine.addEventListener('change', filterEmployees);
    }

    if (selectAll) {
        selectAll.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('.employee-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
        });
    }
    
    console.log('Event listeners setup completed');
}

// Generate next employee ID 
function generateNextEmployeeId() {
    if (employees.length === 0) {
        return '0001';
    }
    
    // Find the highest existing employee number
    const maxNumber = employees.reduce((max, emp) => {
        // Use employeeNumber from API instead of parsing ID
        const number = emp.employeeNumber || parseInt(emp.id);
        if (!isNaN(number)) {
            return number > max ? number : max;
        }
        return max;
    }, 0);
    
    // Generate next ID with leading zeros (4 digits)
    const nextNumber = maxNumber + 1;
    return nextNumber.toString().padStart(4, '0');
}

function updateStats() {
    const adminUsers = employees.filter(emp => emp.originalRole === 'ADMIN').length;

    const uniqueDepartments = new Set();
    employees.forEach(emp => {
        console.log('Employee department:', emp.department); // Debug log
        if (emp.department && emp.department !== 'No Department' && emp.department.trim() !== '') {
            uniqueDepartments.add(emp.department.trim());
        }
    });
    
    const departmentCount = uniqueDepartments.size;

    const totalEmployeesEl = document.getElementById('totalEmployees');
    const activeEmployeesEl = document.getElementById('activeEmployees');
    const departmentsEl = document.getElementById('totalDepartments');
    const adminUsersEl = document.getElementById('totalAdmins');
    
    if (totalEmployeesEl) totalEmployeesEl.textContent = employees.length;
    if (activeEmployeesEl) activeEmployeesEl.textContent = employees.length;
    if (departmentsEl) departmentsEl.textContent = departmentCount;
    if (adminUsersEl) adminUsersEl.textContent = adminUsers;
    
    console.log('Stats updated:', {
        totalEmployees: employees.length,
        departments: departmentCount,
        adminUsers: adminUsers,
        uniqueDepartments: Array.from(uniqueDepartments)
    });
}

function filterEmployees() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const departmentFilter = document.getElementById('departmentFilter').value;
    const roleFilter = document.getElementById('roleFilter').value;
    const machineFilter = document.getElementById('machineFilter').value;
    
    filteredEmployees = employees.filter(emp => {
        const matchesSearch = emp.name.toLowerCase().includes(searchTerm) || 
                            emp.id.toLowerCase().includes(searchTerm) ||
                            emp.email.toLowerCase().includes(searchTerm);
        const matchesDepartment = !departmentFilter || emp.department === departmentFilter;
        const matchesRole = !roleFilter || emp.originalRole === roleFilter;
        const matchesMachine = !machineFilter || 
            (emp.assignedMachines && emp.assignedMachines.some(machine => machine.id === machineFilter));
        return matchesSearch && matchesDepartment && matchesRole && matchesMachine;
    });
    
    currentPage = 1; // Reset to first page when filtering
    updateEmployeeTable();
    updatePagination();
}

function updateEmployeeTable() {
    const tableBody = document.getElementById('employeeTableBody');
    tableBody.innerHTML = '';
    
    // Calculate start and end indices for pagination
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedEmployees = filteredEmployees.slice(startIndex, endIndex);
    
    if (paginatedEmployees.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="9" class="text-center py-4">
                <i class="fas fa-users fa-3x text-muted mb-3"></i>
                <p class="text-muted">No employees found</p>
            </td>
        `;
        tableBody.appendChild(row);
        return;
    }
    
    paginatedEmployees.forEach(emp => {
        const row = document.createElement('tr');
        const roleClass = emp.role === 'Admin' ? 'bg-primary' : 'bg-secondary';
        
        row.innerHTML = `
            <td>
                <input type="checkbox" class="form-check-input employee-checkbox" value="${emp.id}">
            </td>
            <td>${emp.id}</td>
            <td>${emp.name}</td>
            <td>${emp.email}</td>
            <td>${emp.department}</td>
            <td>${emp.assignedMachines && emp.assignedMachines.length > 0 ? emp.assignedMachines.map(m => m.name).join(', ') : 'None'}</td>
            <td>${emp.createdDate}</td>
            <td><span class="badge ${roleClass}">${emp.role}</span></td>
            <td>
                
                <button class="btn btn-sm btn-outline-warning me-1" onclick="editEmployee('${emp.id}')" title="Edit Employee">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteEmployee('${emp.id}')" title="Delete Employee">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });
}

function updatePagination() {
    const totalPages = Math.ceil(filteredEmployees.length / itemsPerPage);
    const paginationControls = document.getElementById('paginationControls');
    const paginationInfo = document.getElementById('paginationInfo');
    
    // Update pagination info
    const startIndex = (currentPage - 1) * itemsPerPage + 1;
    const endIndex = Math.min(currentPage * itemsPerPage, filteredEmployees.length);
    paginationInfo.textContent = `Showing ${startIndex} to ${endIndex} of ${filteredEmployees.length} entries`;
    
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
    const totalPages = Math.ceil(filteredEmployees.length / itemsPerPage);
    if (page < 1 || page > totalPages) return;
    
    currentPage = page;
    updateEmployeeTable();
    updatePagination();
}

async function addEmployee() {
    const employeeName = document.getElementById('addEmployeeName').value.trim();
    const employeeEmail = document.getElementById('addEmployeeEmail').value.trim();
    const employeeDOB = document.getElementById('addEmployeeDOB').value;
    const employeeDepartment = parseInt(document.getElementById('addEmployeeDepartment').value); 
    const employeeRole = document.getElementById('addEmployeeRole').value;
    const machineSelect = document.getElementById('addEmployeeMachines');
    const selectedMachines = Array.from(machineSelect.selectedOptions).map(option => option.value);
    // Validation
    if (!employeeName || !employeeEmail || !employeeDOB || !employeeDepartment || !employeeRole) {
        alert('Please fill in all required fields');
        return;
    }

    // Check if email already exists (front-end check only)
    if (employees.find(emp => emp.email.toLowerCase() === employeeEmail.toLowerCase())) {
        alert('Email address already exists');
        return;
    }

    const newEmployee = {
        name: employeeName,
        email: employeeEmail,
        dateOfBirth: employeeDOB,
        departmentId: parseInt(employeeDepartment),
        role: employeeRole,
        machineIds: selectedMachines
    };
    console.log('Sending new employee data:', newEmployee);
    console.log('Selected machines:', selectedMachines);
    try {
        // const formData = new FormData();
        // formData.append('name', employeeName);
        // formData.append('email', employeeEmail);
        // formData.append('dateOfBirth', employeeDOB);
        // formData.append('departmentId', employeeDepartment);
        // formData.append('role', employeeRole);
        // selectedMachines.forEach(machineId => {
        //     formData.append('machineIds', machineId);
        // });
        // const response = await authFetch('/employees', {  
        //     method: 'POST',
        //     body: formData

        const response = await authFetch('/employees/api/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newEmployee)
        });

        if (response.ok) {
            alert('Employee added successfully!');
            await loadEmployeesFromDatabase();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addEmployeeModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to add employee: ' + errorText);
        }

    } catch (error) {
        console.error('Error adding employee:', error);
        alert('Error adding employee. Please try again.');
    }
}

function viewEmployee(employeeId) {
    const employee = employees.find(emp => emp.id === employeeId);
    if (!employee) return;
    
    document.getElementById('viewEmployeeId').textContent = employee.id;
    document.getElementById('viewEmployeeName').textContent = employee.name;
    document.getElementById('viewEmployeeEmail').textContent = employee.email;
    document.getElementById('viewEmployeeDepartment').textContent = employee.department;
    document.getElementById('viewEmployeeRole').textContent = employee.role;
    document.getElementById('viewEmployeeCreated').textContent = employee.createdDate;
    
    const modal = new bootstrap.Modal(document.getElementById('viewEmployeeModal'));
    modal.show();
}

async function editEmployee(employeeId) {
    const employee = employees.find(emp => emp.id === employeeId);
    if (!employee) return;

    document.getElementById('editEmployeeId').value = employee.id;
    document.getElementById('editEmployeeNumber').value = employee.employeeNumber;
    document.getElementById('editEmployeeName').value = employee.name;
    document.getElementById('editEmployeeEmail').value = employee.email;
    document.getElementById('editEmployeeRole').value = employee.originalRole;
    document.getElementById('editEmployeeDOB').value = employee.dateOfBirth;
    document.getElementById('editEmployeeNumber').readOnly = true;

    await populateEditEmployeeDepartment();

    const departmentSelect = document.getElementById('editEmployeeDepartment');
    for (let option of departmentSelect.options) {
        if (option.textContent === employee.department) {
            option.selected = true;
            break;
        }
    }

    await loadMachineOptions();

    const machineSelect = document.getElementById('editEmployeeMachines');
    if (employee.assignedMachines && employee.assignedMachines.length > 0) {
        for (let option of machineSelect.options) {
            const isAssigned = employee.assignedMachines.some(machine => machine.id === option.value);
            option.selected = isAssigned;
        }
    }
    
    const modal = new bootstrap.Modal(document.getElementById('editEmployeeModal'));
    modal.show();
}

async function populateEditEmployeeDepartment() {
    const departmentSelect = document.getElementById('editEmployeeDepartment');
    if (!departmentSelect) return;

    departmentSelect.innerHTML = '<option value="">Select Department</option>';

    try {
        const response = await authFetch('/employees/api/departments');
        const departments = await response.json();

        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;              
            option.textContent = dept.name;        
            departmentSelect.appendChild(option);
        });

    } catch (error) {
        console.error('Failed to load departments for edit:', error);
    }
}

function editEmployeeFromView() {
    const employeeId = document.getElementById('viewEmployeeId').textContent;
    const viewModal = bootstrap.Modal.getInstance(document.getElementById('viewEmployeeModal'));
    viewModal.hide();
    
    setTimeout(() => {
        editEmployee(employeeId);
    }, 300);
}

async function updateEmployee() {
    const employeeId = document.getElementById('editEmployeeId').value;
    const employeeName = document.getElementById('editEmployeeName').value.trim();
    const employeeEmail = document.getElementById('editEmployeeEmail').value.trim();
    const employeeDepartment = document.getElementById('editEmployeeDepartment').value;
    const employeeRole = document.getElementById('editEmployeeRole').value;
    const employeeDOB = document.getElementById('editEmployeeDOB').value;
    const machineSelect = document.getElementById('editEmployeeMachines');
    const selectedMachines = Array.from(machineSelect.selectedOptions).map(option => option.value);
    // Validation
    if (!employeeName || !employeeEmail || !employeeDepartment || !employeeRole) {
        alert('Please fill in all required fields');
        return;
    }
    
    // Check if email already exists (excluding current employee)
    if (employees.find(emp => emp.email === employeeEmail && emp.id !== employeeId)) {
        alert('Email address already exists');
        return;
    }
    
    const employeeIndex = employees.findIndex(emp => emp.id === employeeId);
    if (employeeIndex === -1) return;
    
    const updatedEmployee = {
        name: employeeName,
        email: employeeEmail,
        dateOfBirth: employeeDOB || null ,
        departmentId: employeeDepartment ? parseInt(employeeDepartment) : null,
        role: employeeRole,
        machineIds: selectedMachines
    };
    console.log('Sending update data:', updatedEmployee);
    try {
        // const formData = new FormData();
        // formData.append('name', employeeName);
        // formData.append('email', employeeEmail);
        // formData.append('dateOfBirth', employeeDOB);
        // formData.append('departmentId', employeeDepartment);
        // formData.append('role', employeeRole);
        // selectedMachines.forEach(machineId => {
        //     formData.append('machineIds', machineId);
        // });
        // const response = await authFetch(`/employees/${employeeId}`, {  
        //     method: 'POST', 
        //     body: formData
        const response = await authFetch(`/employees/api/update/${employeeId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedEmployee)
        });

        if (response.ok) {
            alert('Employee updated successfully!');
            await loadEmployeesFromDatabase();

            const modal = bootstrap.Modal.getInstance(document.getElementById('editEmployeeModal'));
            modal.hide();
        } else {
            const errorText = await response.text();
            alert('Failed to update employee: ' + errorText);
        }
        
    } catch (error) {
        console.error('Error updating employee:', error);
        alert('Error updating employee. Please try again.');
    }
}

async function deleteEmployee(employeeId) {
    const employee = employees.find(emp => emp.id === employeeId);
    if (!employee) return;
    
    if (confirm(`Are you sure you want to delete employee ${employee.name} (${employeeId})?`)) {
        try {
            const response =await authFetch(`/employees/api/delete/${employeeId}`,{method : 'DELETE'});
            if(response.ok){
                // const employeeIndex=employees.findIndex(emp => emp.id === employeeId);
                // employees.splice=(employeeIndex , 1);
                // filterEmployees=[...employees];

                // const totalPages=Math.ceil(filteredEmployees.length/itemsPerPage);
                // if(currentPage>totalPages && totalPages > 0){
                //     currentPage=totalPages;
                // }

                // updateEmployeeTable();
                // updateStats();
                // updatePagination();
                await loadEmployeesFromDatabase();
                alert('Employee deleted successfully!');
            }else{
                const errorText=await response.text();
                alert('Failed to delete employee '+errorText);
            }
        }catch(error){
            console.error('Error deleting employee ', error);
            alert('Error deleting employee , Please try again');
        }
            // Replace with actual API call to your backend
    //         const employeeIndex = employees.findIndex(emp => emp.id === employeeId);
    //         employees.splice(employeeIndex, 1);
            
    //         filteredEmployees = [...employees];
            
    //         // Adjust current page if necessary
    //         const totalPages = Math.ceil(filteredEmployees.length / itemsPerPage);
    //         if (currentPage > totalPages && totalPages > 0) {
    //             currentPage = totalPages;
    //         }
            
    //         updateEmployeeTable();
    //         updateStats();
    //         updatePagination();
            
    //         alert('Employee deleted successfully!');
            
    //     } catch (error) {
    //         console.error('Error deleting employee:', error);
    //         alert('Error deleting employee. Please try again.');
    //     }
     }
}

function bulkActions() {
    const selectedEmployees = Array.from(document.querySelectorAll('.employee-checkbox:checked'))
        .map(checkbox => checkbox.value);
    
    if (selectedEmployees.length === 0) {
        alert('Please select at least one employee');
        return;
    }
    
    const action = prompt(`Selected ${selectedEmployees.length} employee(s). Choose action:\n1. Delete\n2. Change Department\n\nEnter number:`);
    
    switch(action) {
        case '1':
            if (confirm(`Are you sure you want to delete ${selectedEmployees.length} employee(s)?`)) {
                employees = employees.filter(emp => !selectedEmployees.includes(emp.id));
                filteredEmployees = [...employees];
                updateEmployeeTable();
                updateStats();
                updatePagination();
                alert('Selected employees deleted successfully!');
            }
            break;
        case '2':
            const newDepartment = prompt('Enter new department:');
            if (newDepartment && newDepartment.trim()) {
                employees.forEach(emp => {
                    if (selectedEmployees.includes(emp.id)) {
                        emp.department = newDepartment.trim();
                    }
                });
                filteredEmployees = [...employees];
                updateEmployeeTable();
                updateStats();
                alert(`Department updated for ${selectedEmployees.length} employee(s)!`);
            }
            break;
        default:
            alert('Invalid action');
    }
    
    // Uncheck all checkboxes
    document.getElementById('selectAll').checked = false;
    document.querySelectorAll('.employee-checkbox').forEach(checkbox => {
        checkbox.checked = false;
    });

    

}
async function showAddModal() {
    try {
        const response = await authFetch('/employees/api/next-number');
        const nextNumber = await response.json();
        document.getElementById('addEmployeeNumber').value = nextNumber.toString().padStart(4, '0');
    } catch (error) {
        console.error('Failed to authFetch next employee number:', error);
        document.getElementById('addEmployeeNumber').value = '0001';
    }

    await populateAddEmployeeDepartment();

    const modal = new bootstrap.Modal(document.getElementById('addEmployeeModal'));
    modal.show();
}
async function populateAddEmployeeDepartment() {
    const departmentSelect = document.getElementById('addEmployeeDepartment');
    if (!departmentSelect) return;

    departmentSelect.innerHTML = '<option value="">Select Department</option>';

    try {
        const response = await authFetch('/employees/api/departments');
        const departments = await response.json();

        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.id;              
            option.textContent = dept.name;        
            departmentSelect.appendChild(option);
        });

    } catch (error) {
        console.error('Failed to load departments:', error);
    }
}
