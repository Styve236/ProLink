/* ============================================================
   ProLink — Mode clair / sombre
   - Persiste le choix dans localStorage (clé : prolink-theme)
   - Respecte la préférence système au premier chargement
   - Injecte un bouton de bascule FLOTTANT (fixe, en bas à droite)
     pour ne jamais modifier la mise en page des navbars
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

        // Bouton flottant, fixé en bas à droite (au-dessus du bouton
        // "back-to-top" du template). Injecté en FIXE pour ne JAMAIS
        // modifier la mise en page des navbars (flex) ni décaler les
        // boutons existants comme "Publier une offre".
        document.body.appendChild(btn);
    }

    if (document.readyState !== "loading") injecter();
    else document.addEventListener("DOMContentLoaded", injecter);
})();