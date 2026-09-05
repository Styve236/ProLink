/* ============================================================
   ProLink — Mode clair / sombre
   - Persiste le choix dans localStorage (clé : prolink-theme)
   - Respecte la préférence système au premier chargement
   - Injecte le bouton de bascule dans la navbar (ou en haut à
     droite si la page n'a pas de navbar)
   - Applique l'attribut data-theme="dark" sur <html>
   ============================================================ */
(function () {
    "use strict";

    var THEME_KEY = "prolink-theme";

    function themeActuel() {
        var sauvegarde = localStorage.getItem(THEME_KEY);
        if (sauvegarde === "dark" || sauvegarde === "light") return sauvegarde;
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
            ? "dark" : "light";
    }

    function appliquer(theme) {
        var html = document.documentElement;
        if (theme === "dark") {
            html.setAttribute("data-theme", "dark");
        } else {
            html.removeAttribute("data-theme");
        }
        var icone = document.querySelector(".theme-toggle i");
        if (icone) {
            icone.className = theme === "dark" ? "fa fa-sun" : "fa fa-moon";
        }
    }

    var theme = themeActuel();
    appliquer(theme);

    function creerBouton() {
        if (document.querySelector(".theme-toggle")) return null;
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "theme-toggle";
        btn.setAttribute("aria-label", "Changer le thème (clair / sombre)");
        btn.title = "Thème clair / sombre";
        btn.innerHTML = '<i class="fa ' + (theme === "dark" ? "fa-sun" : "fa-moon") + '"></i>';
        btn.addEventListener("click", function () {
            theme = document.documentElement.hasAttribute("data-theme") ? "light" : "dark";
            localStorage.setItem(THEME_KEY, theme);
            appliquer(theme);
        });
        return btn;
    }

    function injecter() {
        var btn = creerBouton();
        if (!btn) return;

        // 1. Dans la navbar, à côté des liens (préféré)
        var collapse = document.querySelector(".navbar-collapse");
        if (collapse) {
            collapse.appendChild(btn);
            return;
        }

        // 2. Dans la navbar sans menu repliable
        var nav = document.querySelector("nav.navbar");
        if (nav) {
            nav.appendChild(btn);
            return;
        }

        // 3. Page sans navbar (ex : compte en attente) — bouton flottant
        btn.classList.add("theme-toggle-flottant");
        document.body.appendChild(btn);
    }

    if (document.readyState !== "loading") injecter();
    else document.addEventListener("DOMContentLoaded", injecter);
})();