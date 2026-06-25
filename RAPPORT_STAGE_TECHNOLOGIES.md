# Rapport de Stage ProLink — Fiche Technique des Technologies et Outils Utilisés

> **Projet :** ProLink — Plateforme de mise en relation professionnelle  
> **Stack :** Java 25 / Spring Boot 3.5.6 / MySQL / Thymeleaf / Bootstrap 5  
> **Type :** Application Web monolithique avec messagerie temps réel

---

## 1. Langages de Programmation

| Langage | Usage | Version |
|---------|-------|---------|
| **Java** | Backend (Spring Boot, JPA, Services, Contrôleurs) | **25** (Amazon Corretto 25 / Oracle OpenJDK 25) |
| **HTML5** | Templates serveur Thymeleaf | — |
| **CSS3** | Styles custom + Bootstrap | — |
| **JavaScript (ES6+)** | Animations, AJAX, WebSocket STOMP client | — |
| **SQL** | Requêtes JPA / JPQL (MySQL) | — |

---

## 2. Framework Principal

### Spring Boot 3.5.6

| Module | Rôle |
|--------|------|
| **spring-boot-starter-web** | Contrôleurs MVC, REST, Tomcat embarqué |
| **spring-boot-starter-data-jpa** | ORM Hibernate / JPA, repositories Spring Data |
| **spring-boot-starter-security** | Authentification, autorisation RBAC, OAuth2 |
| **spring-boot-starter-thymeleaf** | Moteur de templates côté serveur |
| **spring-boot-starter-validation** | Validation des formulaires (Jakarta Bean Validation) |
| **spring-boot-starter-websocket** | WebSocket + STOMP pour messagerie temps réel |
| **spring-boot-starter-mail** | Envoi d'e-mails SMTP (JavaMailSender) |
| **spring-boot-starter-oauth2-client** | Connexion Google (OAuth2 / OpenID Connect) |
| **spring-boot-starter-aop** | Programmation orientée aspect (annotation @RequiertCompteValide) |
| **spring-boot-devtools** | Rechargement automatique en développement |

### Configuration clé (application.properties)

```properties
spring.jpa.hibernate.ddl-auto=update    # Tables créées automatiquement
spring.jpa.open-in-view=true            # Évite LazyInitializationException
spring.thymeleaf.cache=false            # Pas de cache en dev
spring.profiles.active=local            # Profil local avec SMTP Gmail
```

---

## 3. Base de Données

| Technologie | Version | Détail |
|------------|---------|--------|
| **MySQL** | 8+ (via XAMPP) | Base de données relationnelle |
| **MySQL Connector/J** | (fourni par Spring Boot) | Driver JDBC |
| **Hibernate** | 6.x (via Spring Data JPA) | ORM, mapping objet-relationnel |
| **HikariCP** | (intégré Spring Boot) | Pool de connexions JDBC |

### Modèle de Données (11 entités JPA)

| Entité | Table | Type |
|--------|-------|------|
| User | `users` | Classe mère (JOINED) |
| Etudiant | `etudiants` | Héritage User |
| Freelance | `freelances` | Héritage User |
| Recruteur | `recruteurs` | Héritage User |
| JobOffer | `offres` | Indépendante |
| Candidature | `candidatures` | Indépendante |
| Document | `documents` | Indépendante |
| Message | `messages` | Indépendante |
| ChatMessage | `chat_messages` | Indépendante |
| Notification | `notifications` | Indépendante |
| LinkAction | `link_actions` | Indépendante |

Stratégie d'héritage JPA : **`InheritanceType.JOINED`** (une table par classe fille).

---

## 4. Frontend

### CSS Framework & Libraries

| Technologie | Version | Usage |
|------------|---------|-------|
| **Bootstrap 5** | 5.x (SCSS custom) | Framework CSS responsive, grille, composants |
| **Font Awesome** | 5.10.0 (CDN) | Icônes |
| **Bootstrap Icons** | 1.4.1 (CDN) | Icônes complémentaires |
| **Google Fonts** | Heebo + Inter | Typographie |
| **Animate.css** | (locale, lib/animate/) | Animations CSS |
| **OwlCarousel 2** | (locale, lib/owlcarousel/) | Carrousel d'images et témoignages |
| **Waypoints.js** | (locale) | Déclencheurs au scroll |
| **WOW.js** | (locale) | Animations au défilement |
| **jQuery Easing** | (locale) | Fonctions d'easing |

### JavaScript

| Fichier | Rôle |
|---------|------|
| `main.js` | Initialisation spinner, WOW, sticky navbar, back-to-top, carrousels, polling AJAX badge notifications |
| `chat.js` | Client STOMP WebSocket : connexion SockJS, envoi/réception messages, indicateur de frappe |
| `notifications.js` | Polling AJAX toutes les 15s pour le compteur de notifications |
| `websocket-client.js` | Connexion WebSocket publique pour notifications système |

---

## 5. Templates (Thymeleaf)

**33 templates** répartis en 9 dossiers :

| Dossier | Pages |
|---------|-------|
| `admin/` | Dashboard, gestion utilisateurs, documents, offres |
| `auth/` | Connexion, inscription |
| `messages/` | Boîte de réception, chat temps réel |
| `offres/` | Liste, détail, formulaire, candidatures |
| `pages/` | À propos, contact |
| `profil/` | Dashboard par rôle, documents, profil public, recherche |
| `connexions/` | Demandes envoyées/reçues |
| `error/` | Pages 401, 404, 500 |
| `recruteur/` | Liste candidats par offre |

Fonctionnalités Thymeleaf utilisées : `th:each`, `th:if`, `th:field`, `th:object`, `th:replace`, `th:fragment`, `sec:authorize`, expressions `@{...}`, `|...|` (substitution littérale).

---

## 6. Communication Temps Réel

### WebSocket — STOMP

| Élément | Détail |
|---------|--------|
| **Protocole** | STOMP over WebSocket |
| **Fallback** | SockJS (webjar: sockjs-client 1.5.1) |
| **Point de connexion** | `/ws` (SockJS) |
| **Broker** | Simple broker Spring : `/topic` (broadcast), `/queue` (point-à-point) |
| **Destination app** | `/app` |
| **Destination user** | `/user` |
| **Chat room** | `/topic/chat.{roomId}` |
| **Typing indicator** | `/app/chat.frappe` → `/topic/chat.{roomId}.frappe` |
| **Auth** | Propagation du `Principal` via `HandshakeInterceptor` + `ChannelInterceptor` |
| **Reconnexion auto** | Intervalle 5 secondes avec heartbeats |

### AJAX Polling

| Endpoint | Fréquence | Usage |
|----------|-----------|-------|
| `GET /notifications/count` | Toutes les 15 secondes | Mise à jour badge compteur |

---

## 7. Sécurité

| Technique | Détail |
|-----------|--------|
| **BCrypt** | Hash des mots de passe (strength 12) |
| **RBAC** | Contrôle d'accès par rôle (path-based + `@PreAuthorize`) |
| **OAuth2** | Connexion Google (scopes: profile, email) |
| **Custom AuthenticationSuccessHandler** | Redirection après connexion vers la page d'origine (paramètre `redirect`) |
| **CSRF** | Activé globalement, désactivé uniquement pour WebSocket (`/ws/**`) |
| **AOP** | `@RequiertCompteValide` pour vérifier statut ACTIF avant exécution |
| **Session management** | Persistance désactivée, cookie JSESSIONID standard |
| **Custom access denied page** | `/erreur/acces-refuse` (401) |

### Rôles et Permissions

| Rôle | Accès principal |
|------|----------------|
| **ADMIN** | `/admin/**` — dashboard, validation comptes/documents/offres |
| **RECRUTEUR** | Publier/modifier offres, gérer candidatures |
| **ETUDIANT** | Postuler aux offres, uploader documents |
| **FREELANCE** | Postuler aux offres, uploader documents |
| **Non connecté** | `/offres`, `/a-propos`, `/contact`, `/auth/**` |

---

## 8. Email (SMTP Gmail)

| Propriété | Valeur |
|-----------|--------|
| **Hôte** | `smtp.gmail.com` |
| **Port** | `587` (TLS) |
| **Auth** | Mot de passe d'application Gmail |
| **STARTTLS** | Activé |
| **Profil** | `local` uniquement, credentials dans `application-local.properties` (ignoré git) |

### Types d'e-mails envoyés

- Validation du compte utilisateur
- Validation / rejet d'un document
- Approbation d'une offre
- Changement de statut d'une candidature
- Nouveau message reçu
- Test SMTP depuis le dashboard admin

---

## 9. Design Patterns et Architecture

| Pattern | Application |
|---------|-------------|
| **MVC** | Model (Entities) / View (Thymeleaf) / Controller |
| **DTO** | `InscriptionDto`, `OffreDto`, `CandidatureDto` — séparation formulaire / entité |
| **Service Layer** | Logique métier dans les services, contrôleurs légers |
| **Repository** | Accès données via Spring Data JPA |
| **DAO** | Les repositories font office de DAO |
| **Dependency Injection** | Constructeur avec `@RequiredArgsConstructor` ou constructeur explicite |
| **AOP** | `CompteValideAspect` pour `@RequiertCompteValide` |
| **Inheritance (JOINED)** | JPA InheritanceType.JOINED pour User → Etudiant / Freelance / Recruteur |
| **CommandLineRunner** | `AdminInitializer` pour créer le compte admin au démarrage |
| **Observer** | Système de notification : services → NotificationService (email + notification en base) |
| **Strategy** | `AuthService.inscrire()` dispatch par rôle via `switch` |
| **REST endpoint** | `NotificationApiController` pour les appels AJAX JSON |

---

## 10. Outils de Développement

| Outil | Version | Usage |
|-------|---------|-------|
| **IntelliJ IDEA** | — | IDE principal |
| **Maven** (wrapper) | 3.9.16 | Build et dépendances |
| **Spring Boot Maven Plugin** | 3.5.6 | Packaging, exécution |
| **Lombok** | 1.18.42 | Réduction boilerplate (annotations `@Data`, `@Getter`, `@Setter`, etc.) |
| **XAMPP** | — | Serveur MySQL + phpMyAdmin (environnement local) |
| **Git** | — | Versionnement |
| **Google SMTP (App Password)** | — | Envoi d'e-mails depuis le serveur local |

---

## 11. Fonctionnalités Implémentées

### Côté Utilisateur

- [x] Inscription avec validation email `@gmail.com` et photo de profil optionnelle
- [x] Connexion classique + Google OAuth2
- [x] Consultation des offres d'emploi publiques (sans connexion)
- [x] Postulation aux offres (nécessite connexion)
- [x] Upload de documents justificatifs avec validation admin
- [x] Messagerie privée asynchrone (1er message libre, puis réponse requise)
- [x] Chat temps réel WebSocket
- [x] Profil public avec documents validés
- [x] Recherche d'offres et de profils
- [x] Notifications en temps réel (badge + page)
- [x] Demandes de connexion entre utilisateurs
- [x] Tableau de bord personnalisé par rôle

### Côté Administration

- [x] Dashboard avec statistiques (utilisateurs, offres, documents)
- [x] Validation des comptes utilisateurs (trust score)
- [x] Validation des documents justificatifs
- [x] Approbation / rejet des offres d'emploi
- [x] Suspension, archivage, suppression d'utilisateurs
- [x] Téléchargement des documents utilisateurs
- [x] Test d'envoi d'e-mail
- [x] Ajustement du trust score

---

## 12. Fichiers et Configuration Remarquables

| Fichier | Rôle |
|---------|------|
| `pom.xml` | Dépendances, build, plugins |
| `application.properties` | Configuration Spring Boot (DB, mail, thymeleaf, etc.) |
| `application-local.properties` | Credentials SMTP (ignoré par git) |
| `SecurityConfig.java` | Chaîne de filtres, règles d'accès, OAuth2, formulaire login |
| `WebSocketConfig.java` | Configuration STOMP, intercepteurs, broker |
| `WebMvcConfig.java` | Mapping `/uploads/**` → `file:uploads/` |
| `AdminInitializer.java` | Compte admin `admin@prolink.cm` créé au démarrage |
| `EmailService.java` | Méthodes d'envoi d'e-mail avec templates |
| `chat.js` | Client STOMP JavaScript complet |
| `main.js` | Animations, carrousels, polling notifications |
| `style.css` | 393 lignes de styles customs |

---

## 13. Dépendances Externes (CDN / WebJars)

| Ressource | Source |
|-----------|--------|
| Bootstrap Icons | `cdn.jsdelivr.net/npm/bootstrap-icons@1.4.1` |
| Font Awesome | `cdnjs.cloudflare.com/ajax/libs/font-awesome/5.10.0` |
| Google Fonts | `fonts.googleapis.com` |
| jQuery | `code.jquery.com/jquery-3.4.1.min.js` |
| Bootstrap JS | `cdn.jsdelivr.net/npm/bootstrap@5.0.0` |
| SockJS | WebJar `org.webjars:sockjs-client:1.5.1` |

---

## 14. Flux Utilisateur Clés

### Parcours d'une offre

```
Visiteur → /offres → Consultation → Clique postuler
  ↓ (non connecté)
Redirection /auth/connexion?redirect=/offres/{id}
  ↓ (connexion / inscription)
Redirection automatique vers l'offre + formulaire de postulation
  ↓ (envoi)
Candidature enregistrée → notification email au recruteur
```

### Messagerie temps réel

```
Utilisateur A ↔ Chat room (/topic/chat.{roomId})
  ↓ Envoi message
WebSocket STOMP → /app/chat.envoyer → ChatController
  ↓ Broadcast
/topic/chat.{roomId} → tous les participants
```

### Validation admin

```
Utilisateur upload document → admin voit dans dashboard
  ↓ Admin valide
Email + notification à l'utilisateur
  ↓
Document visible sur profil public
```

---

*Document généré le 25/06/2026 — ProLink v2*
