// auth.js

/**
 * JWT authentication
 *  API request add Authorization header automatically
 */
function authFetch(url, options = {}) {
    const token = localStorage.getItem('token'); 
    const defaultHeaders = {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
    }; 
    options.headers = {
        ...defaultHeaders,
        ...options.headers
    }; 
    return fetch(url, options);
}


function isLoggedIn() {
    const token = localStorage.getItem('token');
    return token && token.length > 0;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('employeeNumber');
    localStorage.removeItem('employeeName');
    window.location.href = '/login';
}


function getCurrentUser() {
    return {
        employeeNumber: localStorage.getItem('employeeNumber'),
        name: localStorage.getItem('employeeName'),
        role: localStorage.getItem('role'),
        token: localStorage.getItem('token')
    };
}
