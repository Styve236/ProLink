const socket = new SockJS('/ws-prolink');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connecté au réseau ProLink !');

    stompClient.subscribe('/topic/public', function (response) {
        const notification = JSON.parse(response.body);

        // On déclenche l'apparition de la notification dynamique sur l'écran de l'étudiant
        afficherNotificationSysteme(notification.content);
    });
});

function afficherNotificationSysteme(message) {
    const toast = document.createElement('div');
    toast.className = 'websocket-toast-alert';
    const content = document.createElement('div');
    content.className = 'toast-content';
    const icon = document.createElement('span');
    icon.className = 'toast-icon';
    icon.textContent = '\uD83D\uDE80';
    const p = document.createElement('p');
    const strong = document.createElement('strong');
    strong.textContent = 'Prolink Alerte :';
    p.appendChild(strong);
    p.appendChild(document.createTextNode(' ' + message));
    content.appendChild(icon);
    content.appendChild(p);
    const btn = document.createElement('button');
    btn.innerHTML = '&times;';
    btn.style.cssText = 'background:none; border:none; color:white; cursor:pointer;';
    btn.onclick = function() { toast.remove(); };
    toast.appendChild(content);
    toast.appendChild(btn);
    document.body.appendChild(toast);
}