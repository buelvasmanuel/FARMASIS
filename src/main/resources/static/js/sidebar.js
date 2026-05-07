/**
 * sidebar.js — Lógica compartida del sidebar (toggle, active state, mobile, logout modal)
 * 
 * Flujo anti-flicker:
 *   1. <head> script lee localStorage('sidebarState'). Si === 'closed', inyecta clase
 *      'sidebar-preload-collapsed' en <html> ANTES del primer paint.
 *   2. Los elementos #sidebar y #mainContent arrancan con clase 'no-transition'
 *      para suprimir animaciones CSS durante carga.
 *   3. Este archivo (sidebar.js) al cargar:
 *      a) Sincroniza las clases reales (.collapsed / .expanded) con el estado,
 *      b) Remueve 'no-transition' después del primer frame pintado,
 *      c) Limpia la clase temporal del <html>.
 *
 * Estado por defecto: ABIERTO (si no hay valor en localStorage).
 * Clave localStorage: 'sidebarState' = 'open' | 'closed'
 */

(function () {
    'use strict';

    // ==========================================
    // 1. APPLY SAVED STATE (sync classes)
    // ==========================================
    function applySavedState() {
        var sidebar = document.getElementById('sidebar');
        var mainContent = document.getElementById('mainContent');
        if (!sidebar || !mainContent) return;

        var state = 'open';
        try { state = localStorage.getItem('sidebarState') || 'open'; } catch (e) { }

        if (state === 'closed') {
            sidebar.classList.add('collapsed');
            mainContent.classList.add('expanded');
        } else {
            sidebar.classList.remove('collapsed');
            mainContent.classList.remove('expanded');
        }
    }

    // ==========================================
    // 2. TOGGLE SIDEBAR (collapse / expand)
    // ==========================================
    window.toggleSidebar = function () {
        var sidebar = document.getElementById('sidebar');
        var mainContent = document.getElementById('mainContent');
        if (!sidebar || !mainContent) return;

        // Ensure transitions are enabled for user interaction
        sidebar.classList.remove('no-transition');
        mainContent.classList.remove('no-transition');

        sidebar.classList.toggle('collapsed');
        mainContent.classList.toggle('expanded');

        var newState = sidebar.classList.contains('collapsed') ? 'closed' : 'open';
        try { localStorage.setItem('sidebarState', newState); } catch (e) { }
    };

    // ==========================================
    // 3. ACTIVE STATE — highlight current page
    // ==========================================
    function setActiveNavLink() {
        var currentPath = window.location.pathname;
        var navLinks = document.querySelectorAll('.sidebar .nav-link[href]');

        // Remove all existing active classes first
        navLinks.forEach(function (link) {
            link.classList.remove('active');
        });

        var bestMatch = null;
        var bestMatchLength = 0;

        navLinks.forEach(function (link) {
            var href = link.getAttribute('href');
            if (!href || href === '#') return;

            // Exact match takes priority
            if (currentPath === href) {
                bestMatch = link;
                bestMatchLength = Infinity;
                return;
            }

            // Prefix match — longer prefix wins (e.g. /proveedores/nuevo matches /proveedores)
            if (currentPath.startsWith(href) && href.length > bestMatchLength) {
                bestMatch = link;
                bestMatchLength = href.length;
            }
        });

        if (bestMatch) {
            bestMatch.classList.add('active');
        }
    }

    // ==========================================
    // 4. MOBILE TOGGLE
    // ==========================================
    function setupMobileToggle() {
        var mobileToggle = document.getElementById('mobileToggle');
        if (mobileToggle) {
            mobileToggle.addEventListener('click', function () {
                var sidebar = document.getElementById('sidebar');
                if (sidebar) sidebar.classList.toggle('mobile-open');
            });
        }

        function checkScreenSize() {
            var btn = document.getElementById('mobileToggle');
            if (!btn) return;
            if (window.innerWidth <= 768) {
                btn.style.display = 'block';
            } else {
                btn.style.display = 'none';
                var sidebar = document.getElementById('sidebar');
                if (sidebar) sidebar.classList.remove('mobile-open');
            }
        }

        window.addEventListener('resize', checkScreenSize);
        checkScreenSize();
    }

    // ==========================================
    // 5. LOGOUT MODAL — intercept logout button
    // ==========================================
    function setupLogoutModal() {
        var logoutTrigger = document.getElementById('logoutTrigger');
        if (!logoutTrigger) return;

        logoutTrigger.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            var logoutModal = new bootstrap.Modal(document.getElementById('logoutModal'));
            logoutModal.show();
        });

        var confirmBtn = document.getElementById('logoutConfirmBtn');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', function () {
                var logoutForm = document.getElementById('logoutForm');
                if (logoutForm) logoutForm.submit();
            });
        }
    }

    // ==========================================
    // 6. ENABLE TRANSITIONS (remove no-transition)
    // ==========================================
    function enableTransitions() {
        var sidebar = document.getElementById('sidebar');
        var mainContent = document.getElementById('mainContent');
        // Double rAF ensures the browser has painted the initial state
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                if (sidebar) sidebar.classList.remove('no-transition');
                if (mainContent) mainContent.classList.remove('no-transition');
                // Clean up the <html> preload class
                document.documentElement.classList.remove('sidebar-preload-collapsed');
            });
        });
    }

    // ==========================================
    // INIT
    // ==========================================
    function init() {
        applySavedState();
        setActiveNavLink();
        setupMobileToggle();
        setupLogoutModal();
        enableTransitions();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
