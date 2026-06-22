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
    toast.innerHTML = `
        <div class="toast-content">
            <span class="toast-icon">🚀</span>
            <p><strong>Prolink Alerte :</strong> ${message}</p>
        </div>
        <button onclick="this.parentElement.remove()" style="background:none; border:none; color:white; cursor:pointer;">&times;</button>
    `;
    document.body.appendChild(toast);
}