document.addEventListener('DOMContentLoaded', () => {
    console.log('Sidebar script loaded'); // Debug log
    
    if (typeof getCurrentUser === 'function') {
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
    }

    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    function toggleSidebar() {
        if (sidebar && overlay) {
            sidebar.classList.toggle('open');
            overlay.classList.toggle('show');

            if (mobileMenuBtn) {
                const icon = mobileMenuBtn.querySelector('i');
                if (icon) {
                    if (sidebar.classList.contains('open')) {
                        icon.className = 'fas fa-times';
                    } else {
                        icon.className = 'fas fa-bars';
                    }
                }
            }
        }
    }
    
    function closeSidebar() {
        if (sidebar && overlay) {
            sidebar.classList.remove('open');
            overlay.classList.remove('show');
            if (mobileMenuBtn) {
                const icon = mobileMenuBtn.querySelector('i');
                if (icon) {
                    icon.className = 'fas fa-bars';
                }
            }
        }
    }

    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', toggleSidebar);
        console.log('Mobile menu button event listener added');
    }

    if (overlay) {
        overlay.addEventListener('click', closeSidebar);
        console.log('Overlay event listener added');
    }

    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function() {
            if (window.innerWidth <= 768) {
                closeSidebar();
            }
        });
    });

    window.addEventListener('resize', function() {
        if (window.innerWidth > 768) {
            closeSidebar();
        }
    });
    
    console.log('Mobile sidebar functionality initialized');
});