document.addEventListener('DOMContentLoaded', () => {

    const zoneMessages     = document.getElementById('zoneMessages');
    const champMessage     = document.getElementById('champMessage');
    const boutonEnvoyer    = document.getElementById('boutonEnvoyer');
    const statutConnexion  = document.getElementById('statutConnexion');
    const indicateurFrappe = document.getElementById('indicateurFrappe');
    const formMessage      = document.getElementById('formMessage');

    // Défilement initial vers le bas (historique déjà chargé par Thymeleaf)
    zoneMessages.scrollTop = zoneMessages.scrollHeight;

    const socket = new SockJS('/ws');
    const stompClient = new StompJs.Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,          // tentative de reconnexion auto toutes les 5s
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = () => {
        statutConnexion.textContent = 'En ligne';
        statutConnexion.className = 'badge bg-success';
        boutonEnvoyer.disabled = false;

        // Abonnement au topic de cette room précise
        stompClient.subscribe('/topic/chat.' + ROOM_ID, (frame) => {
            const data = JSON.parse(frame.body);

            if (data.type === 'FRAPPE') {
                afficherFrappe(data.expediteurNom);
                return;
            }

            afficherMessage(data);
        });
    };

    stompClient.onWebSocketClose = () => {
        statutConnexion.textContent = 'Déconnecté — reconnexion...';
        statutConnexion.className = 'badge bg-warning text-dark';
        boutonEnvoyer.disabled = true;
    };

    stompClient.onStompError = (frame) => {
        console.error('Erreur STOMP :', frame.headers['message'], frame.body);
        statutConnexion.textContent = 'Erreur de connexion';
        statutConnexion.className = 'badge bg-danger';
    };

    stompClient.activate();

    // ENVOI D'UN MESSAGE
    function envoyerMessage() {
        const contenu = champMessage.value.trim();
        if (contenu === '' || !stompClient.connected) return;

        stompClient.publish({
            destination: '/app/chat.envoyer',
            body: JSON.stringify({
                contenu: contenu,
                roomId: ROOM_ID
            })
        });

        champMessage.value = '';
    }

    formMessage.addEventListener('submit', envoyerMessage);

    // NOTIFICATION DE FRAPPE — anti-spam avec un délai
    let timeoutFrappe = null;
    champMessage.addEventListener('input', () => {
        if (!stompClient.connected) return;

        stompClient.publish({
            destination: '/app/chat.frappe',
            body: JSON.stringify({ roomId: ROOM_ID })
        });
    });

    function afficherFrappe(nom) {
        indicateurFrappe.textContent = nom + ' est en train d\'écrire...';
        clearTimeout(timeoutFrappe);
        timeoutFrappe = setTimeout(() => {
            indicateurFrappe.textContent = '';
        }, 3000);
    }

    // AFFICHAGE D'UN NOUVEAU MESSAGE REÇU
    function afficherMessage(data) {
        const estMoi = Number(data.expediteurId) === Number(MON_ID);

        const wrapper = document.createElement('div');
        wrapper.className = estMoi ? 'text-end mb-2' : 'text-start mb-2';

        const bulle = document.createElement('div');
        bulle.className = estMoi
            ? 'd-inline-block bg-primary text-white rounded p-2'
            : 'd-inline-block bg-light rounded p-2';

        const texte = document.createElement('div');
        texte.textContent = data.contenu; // textContent = pas d'injection HTML possible

        const heure = document.createElement('div');
        heure.className = 'small opacity-75';
        heure.textContent = new Date(data.horodatage).toLocaleTimeString('fr-FR', {
            hour: '2-digit', minute: '2-digit'
        });

        bulle.appendChild(texte);
        bulle.appendChild(heure);
        wrapper.appendChild(bulle);
        zoneMessages.appendChild(wrapper);

        zoneMessages.scrollTop = zoneMessages.scrollHeight;
    }

});