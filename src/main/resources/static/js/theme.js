
/* =========================================
   SMART CLASSROOM - THEME CONTROLLER
   ========================================= */

(function () {

    const savedTheme = localStorage.getItem("smartClassroomTheme");

    const initialTheme = savedTheme || "dark";

    document.documentElement.setAttribute(
        "data-theme",
        initialTheme
    );

    function updateThemeButton() {

        const themeButton = document.getElementById("themeToggle");

        if (!themeButton) {
            return;
        }

        const currentTheme =
            document.documentElement.getAttribute("data-theme");

        if (currentTheme === "dark") {
            themeButton.innerHTML = "☀️ Light Mode";
            themeButton.setAttribute("aria-label", "Switch to light mode");
        } else {
            themeButton.innerHTML = "🌙 Dark Mode";
            themeButton.setAttribute("aria-label", "Switch to dark mode");
        }
    }

    window.toggleTheme = function () {

        const currentTheme =
            document.documentElement.getAttribute("data-theme");

        const newTheme =
            currentTheme === "dark" ? "light" : "dark";

        document.documentElement.setAttribute(
            "data-theme",
            newTheme
        );

        localStorage.setItem(
            "smartClassroomTheme",
            newTheme
        );

        updateThemeButton();
    };

    document.addEventListener("DOMContentLoaded", updateThemeButton);

})();
