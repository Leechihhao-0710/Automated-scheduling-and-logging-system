document.addEventListener('DOMContentLoaded', () => {
    console.log('Sidebar script loaded'); // Debug log
    

    if (typeof getCurrentUser !== 'function') {
        return;
    }

    const user = getCurrentUser();
    const nameElement = document.getElementById("sidebar-user-name");
    
    console.log('User data from localStorage:', user); // Debug log
    console.log('Name element found:', nameElement); // Debug log

    if (nameElement) {
        if (user && user.name && user.name.trim() !== '') {
            nameElement.textContent = user.name;
            console.log('Set sidebar name to:', user.name); // Debug log
        } else {
            nameElement.textContent = "Unknown User";
            console.log('No user name found, setting to Unknown User'); // Debug log
        }
    } else {
        console.error('Sidebar name element not found!');
    }

    console.log('All localStorage data:');
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        const value = localStorage.getItem(key);
        console.log(`${key}: ${value}`);
    }
});