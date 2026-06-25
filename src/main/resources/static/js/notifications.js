(function($) {
    "use strict";

    function mettreAJourBadge() {
        var badges = document.querySelectorAll('.notif-badge, .notification-count');
        if (!badges.length) return;
        fetch('/notifications/count')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var count = data.nonLues || 0;
                badges.forEach(function(badge) {
                    if (count > 0) {
                        badge.textContent = count;
                        badge.style.display = '';
                    } else {
                        badge.style.display = 'none';
                    }
                });
            })
            .catch(function() {});
    }

    setInterval(mettreAJourBadge, 15000);

    if (document.readyState === 'complete') {
        mettreAJourBadge();
    } else {
        document.addEventListener('DOMContentLoaded', mettreAJourBadge);
    }

})(jQuery);
