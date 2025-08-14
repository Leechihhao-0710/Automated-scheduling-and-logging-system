
document.addEventListener('submit', (e) => {
  e.preventDefault();
});

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('loginForm');
    const errorMessage = document.getElementById('error-message');


    if (!form) {
        console.error("loginForm not found!");
        return;
    } else {
        console.log("loginForm got");
    }




    form.addEventListener('submit', async function (e) {

        e.preventDefault();
       

        const employeeNumber = document.getElementById('employeeNumber').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch('/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    employeeNumber,
                    password
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                errorMessage.innerText = errorText || 'login failed';
                return;
            }

            const data = await response.json();

        
            localStorage.setItem('token', data.token);
            localStorage.setItem('role', data.role);
            localStorage.setItem('employeeNumber', data.employeeNumber);
            localStorage.setItem('employeeName', data.name);

            if (data.role === 'ADMIN') {
                window.location.href = '/admin/dashboard';
            } else if (data.role === 'USER') {
                window.location.href = '/user/dashboard';
            } else {
                errorMessage.innerText = 'failed';
            }

        } catch (err) {
            console.error('Login error:', err);
            errorMessage.innerText = 'system error。';
        }
    });
});
