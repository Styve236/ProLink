-- ============================================================
-- PROLINK — Jeu de données de démonstration COMPLET
-- 10 recruteurs (4 personnes physiques + 6 sociétés)
--   + 15 offres d'emploi (statut APPROUVEE, visibles publiquement)
--   + 10 freelances
--   + 10 étudiants
-- Chaque compte a une photo de profil distincte (URLs externes).
--
-- Mots de passe (hash BCrypt) :
--   → Recruteurs :  Recruteur@2026!
--   → Freelances :  Demo@2026!
--   → Étudiants :   Demo@2026!
--
-- Le script est IDEMPOTENT : il supprime d'abord les données de démo
-- existantes (domaine @prolink.cm) puis réinsère. Exécutable plusieurs fois.
--
-- À exécuter en UN SEUL bloc dans le SQL Editor de Neon.
-- ============================================================

-- ============================================================
-- 0) SUPPRESSION des données de démo existantes
-- ============================================================
-- Blog (commentaires, réactions puis posts) — tout le blog de démo
DELETE FROM commentaires WHERE post_id IN (
    SELECT id FROM blog_posts WHERE auteur_id IN (
        SELECT u.id FROM users u WHERE u.email LIKE 'demo-%@prolink.cm'
    )
);
DELETE FROM reactions WHERE post_id IN (
    SELECT id FROM blog_posts WHERE auteur_id IN (
        SELECT u.id FROM users u WHERE u.email LIKE 'demo-%@prolink.cm'
    )
);
DELETE FROM blog_posts WHERE auteur_id IN (
    SELECT u.id FROM users u WHERE u.email LIKE 'demo-%@prolink.cm'
);
-- Offres puis profils puis users
DELETE FROM offres WHERE recruteur_id IN (
    SELECT id FROM recruteurs WHERE id IN (SELECT u.id FROM users u WHERE u.email LIKE 'demo-%@prolink.cm')
);
DELETE FROM freelances WHERE id IN (SELECT u.id FROM users u WHERE u.email LIKE 'demo-free-%@prolink.cm');
DELETE FROM etudiants  WHERE id IN (SELECT u.id FROM users u WHERE u.email LIKE 'demo-etu-%@prolink.cm');
DELETE FROM recruteurs WHERE id IN (SELECT u.id FROM users u WHERE u.email LIKE 'demo-%@prolink.cm');
DELETE FROM users WHERE email LIKE 'demo-%@prolink.cm';

-- ============================================================
-- 1) LES 10 RECRUTEURS
--    (4 personnes physiques + 6 sociétés)
-- ============================================================
INSERT INTO users
(email, password, nom, prenom, telephone, ville, photo_url, role, statut, trust_score, date_inscription)
VALUES
-- ---- 4 PERSONNES PHYSIQUES ----
('demo-paul.mboungou@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','MBOUNGOU','Paul','+237690112233','Douala','https://images.pexels.com/photos/9363113/pexels-photo-9363113.jpeg','RECRUTEUR','ACTIF',80,NOW()),
('demo-marie.ngono@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','NGONO','Marie','+237691223344','Yaoundé','https://images.pexels.com/photos/12008288/pexels-photo-12008288.jpeg','RECRUTEUR','ACTIF',75,NOW()),
('demo-jean.eteme@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','ETEME','Jean','+237692334455','Bafoussam','https://images.pexels.com/photos/12311537/pexels-photo-12311537.jpeg','RECRUTEUR','ACTIF',72,NOW()),
('demo-amina.sali@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','SALI','Amina','+237693445566','Garoua','https://images.pexels.com/photos/29852895/pexels-photo-29852895.jpeg','RECRUTEUR','ACTIF',78,NOW()),

-- ---- 6 SOCIÉTÉS ----
('demo-emploi-techcorp@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','TECHCORP','Recrutement','+237237112233','Douala','https://ui-avatars.com/api/?name=TechCorp&background=00B074&color=fff&size=256','RECRUTEUR','ACTIF',90,NOW()),
('demo-emploi-bankcmr@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','BANK CMR','Recrutement','+237237223344','Yaoundé','https://ui-avatars.com/api/?name=Bank+CMR&background=2B9BFF&color=fff&size=256','RECRUTEUR','ACTIF',92,NOW()),
('demo-emploi-agroplus@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','AGRO PLUS','Recrutement','+237237334455','Douala','https://ui-avatars.com/api/?name=Agro+Plus&background=4a154b&color=fff&size=256','RECRUTEUR','ACTIF',85,NOW()),
('demo-emploi-logistik@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','LOGISTIK','Recrutement','+237237445566','Kribi','https://ui-avatars.com/api/?name=Logistik&background=ecb22e&color=1d1d1d&size=256','RECRUTEUR','ACTIF',82,NOW()),
('demo-emploi-mediagroupe@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','MEDIA GROUPE','Recrutement','+237237556677','Yaoundé','https://ui-avatars.com/api/?name=Media+Groupe&background=4285f4&color=fff&size=256','RECRUTEUR','ACTIF',80,NOW()),
('demo-emploi-nova.tech@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','NOVA TECH','Recrutement','+237237667788','Douala','https://ui-avatars.com/api/?name=Nova+Tech&background=007a5a&color=fff&size=256','RECRUTEUR','ACTIF',88,NOW());

-- ---- Profils recruteurs (table recruteurs) ----
INSERT INTO recruteurs
(id, nom_entreprise, secteur_activite, site_web, description_entreprise, ville_entreprise, taille_entreprise)
SELECT u.id,
       CASE u.email
         WHEN 'demo-paul.mboungou@prolink.cm'   THEN 'Cabinet Mboungou Conseil'
         WHEN 'demo-marie.ngono@prolink.cm'     THEN 'Ngono Consulting'
         WHEN 'demo-jean.eteme@prolink.cm'      THEN 'Eteme Services'
         WHEN 'demo-amina.sali@prolink.cm'      THEN 'Sali Associés'
         WHEN 'demo-emploi-techcorp@prolink.cm' THEN 'TechCorp Cameroun'
         WHEN 'demo-emploi-bankcmr@prolink.cm'  THEN 'Bank CMR SA'
         WHEN 'demo-emploi-agroplus@prolink.cm' THEN 'Agro Plus Sarl'
         WHEN 'demo-emploi-logistik@prolink.cm' THEN 'Logistik Cameroun'
         WHEN 'demo-emploi-mediagroupe@prolink.cm' THEN 'Média Groupe SA'
         ELSE 'Nova Tech Solutions'
       END,
       CASE u.email
         WHEN 'demo-paul.mboungou@prolink.cm'   THEN 'Conseil'
         WHEN 'demo-marie.ngono@prolink.cm'     THEN 'Consulting'
         WHEN 'demo-jean.eteme@prolink.cm'      THEN 'Services'
         WHEN 'demo-amina.sali@prolink.cm'      THEN 'Juridique'
         WHEN 'demo-emploi-techcorp@prolink.cm' THEN 'Informatique'
         WHEN 'demo-emploi-bankcmr@prolink.cm'  THEN 'Banque / Finance'
         WHEN 'demo-emploi-agroplus@prolink.cm' THEN 'Agroalimentaire'
         WHEN 'demo-emploi-logistik@prolink.cm' THEN 'Logistique'
         WHEN 'demo-emploi-mediagroupe@prolink.cm' THEN 'Médias'
         ELSE 'Technologie'
       END,
       CASE u.email
         WHEN 'demo-paul.mboungou@prolink.cm'   THEN 'https://mboungou-conseil.cm'
         WHEN 'demo-marie.ngono@prolink.cm'     THEN 'https://ngono-consulting.cm'
         WHEN 'demo-jean.eteme@prolink.cm'      THEN 'https://eteme-services.cm'
         WHEN 'demo-amina.sali@prolink.cm'      THEN 'https://sali-associes.cm'
         WHEN 'demo-emploi-techcorp@prolink.cm' THEN 'https://techcorp.cm'
         WHEN 'demo-emploi-bankcmr@prolink.cm'  THEN 'https://bankcmr.com'
         WHEN 'demo-emploi-agroplus@prolink.cm' THEN 'https://agroplus.cm'
         WHEN 'demo-emploi-logistik@prolink.cm' THEN 'https://logistik.cm'
         WHEN 'demo-emploi-mediagroupe@prolink.cm' THEN 'https://mediagroupe.cm'
         ELSE 'https://novatech.cm'
       END,
       'Entreprise partenaire ProLink, recrute activement des talents locaux.',
       u.ville,
       CASE WHEN u.email LIKE 'demo-emploi-%' THEN '51-200' ELSE '1-10' END
FROM users u
WHERE u.email LIKE 'demo-%@prolink.cm'
  AND u.role = 'RECRUTEUR'
  AND NOT EXISTS (SELECT 1 FROM recruteurs r WHERE r.id = u.id);

-- ============================================================
-- 2) LES 15 OFFRES D'EMPLOI (statut APPROUVEE => visibles)
-- ============================================================
INSERT INTO offres
(titre, entreprise, description, type_contrat, lieu, remuneration,
 competences_requises, experience_requise, statut, date_publication, recruteur_id)
SELECT v.titre, v.entreprise, v.description, v.type_contrat, v.lieu, v.remuneration,
       v.competences, v.experience, 'APPROUVEE', NOW() - (v.jours * INTERVAL '1 day'), u.id
FROM (VALUES
  ('Développeur Java Spring Boot','TechCorp Cameroun','Rejoignez notre équipe produit pour développer des API REST robustes avec Spring Boot.','CDI','Douala','850 000 FCFA/mois','Java, Spring Boot, PostgreSQL, REST','3 ans',1,'demo-emploi-techcorp@prolink.cm'),
  ('Développeuse Frontend React','Nova Tech Solutions','Créer des interfaces modernes et réactives avec React et Tailwind.','CDI','Douala','750 000 FCFA/mois','React, JavaScript, CSS, Git','2 ans',1,'demo-emploi-nova.tech@prolink.cm'),
  ('Ingénieur DevOps','Nova Tech Solutions','Mettre en place CI/CD, Docker et supervision des environnements cloud.','CDI','Douala','900 000 FCFA/mois','Docker, Kubernetes, CI/CD, AWS','3 ans',1,'demo-emploi-nova.tech@prolink.cm'),
  ('Chargé de clientèle bancaire','Bank CMR SA','Accueillir et conseiller la clientèle, promouvoir les produits bancaires.','CDI','Yaoundé','600 000 FCFA/mois','Relation client, vente, Pack Office','1 an',1,'demo-emploi-bankcmr@prolink.cm'),
  ('Analyste financier','Bank CMR SA','Analyser les risques et préparer les rapports financiers.','CDI','Yaoundé','700 000 FCFA/mois','Analyse financière, Excel avancé, comptabilité','2 ans',1,'demo-emploi-bankcmr@prolink.cm'),
  ('Technicien agroalimentaire','Agro Plus Sarl','Superviser la production et le contrôle qualité en usine agroalimentaire.','CDI','Douala','550 000 FCFA/mois','Agroalimentaire, HACCP, maintenance','2 ans',1,'demo-emploi-agroplus@prolink.cm'),
  ('Chef de projet logistique','Logistik Cameroun','Piloter la chaîne d''approvisionnement et les livraisons.','CDI','Kribi','650 000 FCFA/mois','Logistique, gestion de stock, ERP','3 ans',1,'demo-emploi-logistik@prolink.cm'),
  ('Rédacteur web / SEO','Média Groupe SA','Produire des contenus web optimisés SEO pour nos sites.','Freelance','Yaoundé','200 000 FCFA/projet','Rédaction, SEO, WordPress','1 an',1,'demo-emploi-mediagroupe@prolink.cm'),
  ('Graphiste digital','Média Groupe SA','Concevoir des visuels pour campagnes web et réseaux sociaux.','Freelance','Yaoundé','180 000 FCFA/projet','Photoshop, Illustrator, Canva','1 an',1,'demo-emploi-mediagroupe@prolink.cm'),
  ('Assistant RH','Ngono Consulting','Assister le cabinet dans le recrutement et l''administration du personnel.','Stage','Yaoundé','150 000 FCFA/mois','RH, recrutement, Pack Office','Débutant',1,'demo-marie.ngono@prolink.cm'),
  ('Commercial terrain','Eteme Services','Développer un portefeuille clients pour des solutions B2B.','CDI','Bafoussam','400 000 FCFA + primes','Vente, négociation, mobilité','1 an',1,'demo-jean.eteme@prolink.cm'),
  ('Consultant juridique junior','Sali Associés','Assister l''équipe sur les dossiers juridiques et les contrats.','Stage','Garoua','175 000 FCFA/mois','Droit, rédaction, analyse','Débutant',1,'demo-amina.sali@prolink.cm'),
  ('Assistant de direction','Cabinet Mboungou Conseil','Gérer l''agenda, les rendez-vous et le suivi des dossiers.','CDI','Douala','500 000 FCFA/mois','Organisation, secrétariat, bureautique','2 ans',1,'demo-paul.mboungou@prolink.cm'),
  ('Data Analyst','Nova Tech Solutions','Analyser les données et produire des tableaux de bord.','CDI','Douala','800 000 FCFA/mois','SQL, Python, Power BI, statistiques','2 ans',1,'demo-emploi-nova.tech@prolink.cm'),
  ('Community Manager','Média Groupe SA','Animer et développer les communautés sur les réseaux sociaux.','Freelance','Yaoundé','160 000 FCFA/projet','Réseaux sociaux, création de contenu','1 an',0,'demo-emploi-mediagroupe@prolink.cm')
) AS v(titre, entreprise, description, type_contrat, lieu, remuneration, competences, experience, jours, email)
JOIN users u ON u.email = v.email;

-- ============================================================
-- 3) LES 10 FREELANCES
-- ============================================================
INSERT INTO users
(email, password, nom, prenom, telephone, ville, photo_url, role, statut, trust_score, date_inscription)
VALUES
('demo-free-alexis.simo@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','SIMO','Alexis','+237699112233','Douala','https://images.pexels.com/photos/12311567/pexels-photo-12311567.jpeg','FREELANCE','ACTIF',85,NOW()),
('demo-free-clarisse.ndoum@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','NDOUM','Clarisse','+237699223344','Yaoundé','https://images.pexels.com/photos/7468194/pexels-photo-7468194.jpeg','FREELANCE','ACTIF',80,NOW()),
('demo-free-samuel.mbaye@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','MBAYE','Samuel','+237699334455','Douala','https://images.pexels.com/photos/12311572/pexels-photo-12311572.jpeg','FREELANCE','ACTIF',78,NOW()),
('demo-free-nadia.kamga@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','KAMGA','Nadia','+237699445566','Bafoussam','https://images.pexels.com/photos/34769115/pexels-photo-34769115.jpeg','FREELANCE','ACTIF',82,NOW()),
('demo-free-yves.tchoua@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','TCHOUA','Yves','+237699556677','Douala','https://images.pexels.com/photos/12311549/pexels-photo-12311549.jpeg','FREELANCE','ACTIF',74,NOW()),
('demo-free-laurie.esso@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','ESSO','Laurie','+237699667788','Yaoundé','https://images.pexels.com/photos/32288633/pexels-photo-32288633.jpeg','FREELANCE','ACTIF',79,NOW()),
('demo-free-brice.kenfack@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','KENFACK','Brice','+237699778899','Kribi','https://images.pexels.com/photos/12311564/pexels-photo-12311564.jpeg','FREELANCE','ACTIF',76,NOW()),
('demo-free-grace.ngassa@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','NGASSA','Grâce','+237699889900','Garoua','https://images.pexels.com/photos/35572078/pexels-photo-35572078.jpeg','FREELANCE','ACTIF',73,NOW()),
('demo-free-eliott.fotso@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','FOTSO','Eliott','+237699990011','Douala','https://images.pexels.com/photos/33331334/pexels-photo-33331334.jpeg','FREELANCE','ACTIF',81,NOW()),
('demo-free-sonia.djomga@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','DJOMGA','Sonia','+237699001122','Yaoundé','https://images.pexels.com/photos/37079379/pexels-photo-37079379.jpeg','FREELANCE','ACTIF',77,NOW());

-- ---- Profils freelances (table freelances) ----
INSERT INTO freelances
(id, specialite, portfolio_url, tjm, competences, annees_experience, disponibilite, lien_professionnel)
SELECT u.id,
       v.specialite, v.portfolio, v.tjm, v.competences, v.annees, v.dispo, v.lien
FROM users u
JOIN (VALUES
  ('demo-free-alexis.simo@prolink.cm','Développement Web','https://alexissimo.dev','80 000 FCFA','Java, Spring Boot, Angular',5,'Disponible immédiatement','https://linkedin.com/in/alexissimo'),
  ('demo-free-clarisse.ndoum@prolink.cm','Design Graphique','https://clarissendoum.com','60 000 FCFA','Photoshop, Illustrator, Figma',4,'Disponible dans 2 semaines','https://linkedin.com/in/clarissendoum'),
  ('demo-free-samuel.mbaye@prolink.cm','Développement Mobile','https://samuelmbaye.dev','75 000 FCFA','Flutter, React Native, Firebase',4,'Disponible immédiatement','https://linkedin.com/in/samuelmbaye'),
  ('demo-free-nadia.kamga@prolink.cm','Marketing Digital','https://nadiakamga.com','50 000 FCFA','SEO, publicité, réseaux sociaux',3,'Disponible dans 1 mois','https://linkedin.com/in/nadiakamga'),
  ('demo-free-yves.tchoua@prolink.cm','Data Science','https://yvestchoua.com','90 000 FCFA','Python, SQL, Machine Learning, Power BI',6,'Disponible immédiatement','https://linkedin.com/in/yvestchoua'),
  ('demo-free-laurie.esso@prolink.cm','Rédaction / Contenu','https://laurieesso.com','35 000 FCFA','Rédaction web, SEO, WordPress',2,'Disponible immédiatement','https://linkedin.com/in/laurieesso'),
  ('demo-free-brice.kenfack@prolink.cm','Développement Backend','https://bricekenfack.dev','85 000 FCFA','Node.js, PostgreSQL, Docker',5,'Disponible dans 2 semaines','https://linkedin.com/in/bricekenfack'),
  ('demo-free-grace.ngassa@prolink.cm','Montage Vidéo','https://gracessa.com','45 000 FCFA','Premiere Pro, After Effects, DaVinci',3,'Disponible immédiatement','https://linkedin.com/in/gracessa'),
  ('demo-free-eliott.fotso@prolink.cm','Cybersécurité','https://eliottfotso.com','95 000 FCFA','Pentest, Kali Linux, analyse SIEM',5,'Disponible dans 1 mois','https://linkedin.com/in/eliottfotso'),
  ('demo-free-sonia.djomga@prolink.cm','Community Management','https://soniadjomga.com','40 000 FCFA','Réseaux sociaux, Canva, stratégie de contenu',3,'Disponible immédiatement','https://linkedin.com/in/soniadjomga')
) AS v(email, specialite, portfolio, tjm, competences, annees, dispo, lien)
ON u.email = v.email
AND u.role = 'FREELANCE';

-- ============================================================
-- 4) LES 10 ÉTUDIANTS
-- ============================================================
INSERT INTO users
(email, password, nom, prenom, telephone, ville, photo_url, role, statut, trust_score, date_inscription)
VALUES
('demo-etu-luc.ngassa@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','NGASSA','Luc','+237691011121','Douala','https://images.pexels.com/photos/35129364/pexels-photo-35129364.jpeg','ETUDIANT','ACTIF',70,NOW()),
('demo-etu-adeline.mefire@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','MEFIRE','Adeline','+237691122233','Yaoundé','https://images.pexels.com/photos/32846944/pexels-photo-32846944.jpeg','ETUDIANT','ACTIF',75,NOW()),
('demo-etu-kevin.abadie@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','ABADIE','Kevin','+237691233344','Douala','https://images.pexels.com/photos/34592823/pexels-photo-34592823.jpeg','ETUDIANT','ACTIF',68,NOW()),
('demo-etu-flora.tonye@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','TONYE','Flora','+237691344455','Bafoussam','https://images.pexels.com/photos/26985383/pexels-photo-26985383.jpeg','ETUDIANT','ACTIF',72,NOW()),
('demo-etu-boris.ebogo@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','EBOGO','Boris','+237691455566','Douala','https://images.pexels.com/photos/30124371/pexels-photo-30124371.jpeg','ETUDIANT','ACTIF',66,NOW()),
('demo-etu-ines.mbarga@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','MBARGA','Inès','+237691566677','Garoua','https://images.pexels.com/photos/27038743/pexels-photo-27038743.jpeg','ETUDIANT','ACTIF',71,NOW()),
('demo-etu-carlos.owo@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','OWO','Carlos','+237691677788','Douala','https://images.pexels.com/photos/32064778/pexels-photo-32064778.jpeg','ETUDIANT','ACTIF',74,NOW()),
('demo-etu-nelie.fonkwa@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','FONKWA','Nélie','+237691788899','Yaoundé','https://images.pexels.com/photos/18509401/pexels-photo-18509401.jpeg','ETUDIANT','ACTIF',69,NOW()),
('demo-etu-garrick.mbenoun@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','MBENOUN','Garrick','+237691899900','Kribi','https://images.pexels.com/photos/19111262/pexels-photo-19111262.jpeg','ETUDIANT','ACTIF',67,NOW()),
('demo-etu-tatiana.bekono@prolink.cm','$2a$12$s.Cn3k3tT4qShkL7KxpwAuDxh2U/2PusrSxfeCCmjWSGhmPzUoIQC','BEKONO','Tatiana','+237691900011','Douala','https://images.pexels.com/photos/10226305/pexels-photo-10226305.jpeg','ETUDIANT','ACTIF',73,NOW());

-- ---- Profils étudiants (table etudiants) ----
INSERT INTO etudiants
(id, universite, filiere, niveau_etude, cv_url, disponibilite, competences)
SELECT u.id,
       v.universite, v.filiere, v.niveau, v.cv, v.dispo, v.competences
FROM users u
JOIN (VALUES
  ('demo-etu-luc.ngassa@prolink.cm','Université de Douala','Informatique','Licence 3','Lisez-cv','Disponible en stage','Java, SQL, Web'),
  ('demo-etu-adeline.mefire@prolink.cm','Université de Yaoundé I','Gestion','Master 1','Lisez-cv','Disponible en alternance','Comptabilité, Excel'),
  ('demo-etu-kevin.abadie@prolink.cm','Institut Polytechnique','Réseaux & Télécoms','Licence 2','Lisez-cv','Disponible en stage','Réseaux, Linux, CCNA'),
  ('demo-etu-flora.tonye@prolink.cm','IUT de Bafoussam','Marketing','Licence 3','Lisez-cv','Disponible en stage','Marketing, Canva, réseaux sociaux'),
  ('demo-etu-boris.ebogo@prolink.cm','Université de Douala','Génie Civil','Master 1','Lisez-cv','Disponible en stage','AutoCAD, chantier, calcul'),
  ('demo-etu-ines.mbarga@prolink.cm','Université de Ngaoundéré','Agronomie','Master 1','Lisez-cv','Disponible en alternance','Agronomie, analyse de données'),
  ('demo-etu-carlos.owo@prolink.cm','Institut Universitaire','Développement Web','Licence 3','Lisez-cv','Disponible en stage','HTML, CSS, JavaScript, React'),
  ('demo-etu-nelie.fonkwa@prolink.cm','Université de Yaoundé II','Droit','Licence 3','Lisez-cv','Disponible en stage','Droit des affaires, rédaction'),
  ('demo-etu-garrick.mbenoun@prolink.cm','École des Mines','Électrotechnique','Master 2','Lisez-cv','Disponible en stage','Électricité, automatisme'),
  ('demo-etu-tatiana.bekono@prolink.cm','Université de Douala','Design','Licence 2','Lisez-cv','Disponible en stage','Photoshop, Illustrator, Figma')
) AS v(email, universite, filiere, niveau, cv, dispo, competences)
ON u.email = v.email
AND u.role = 'ETUDIANT';

-- ============================================================
-- 4bis) BLOG DE DÉMONSTRATION
-- ============================================================
-- Articles rédigés par des entreprises et des freelances.

INSERT INTO blog_posts (titre, contenu, date_publication, nb_vues, auteur_id)
SELECT 'TechCorp ouvre son centre d''innovation à Douala',
       'TechCorp est fière d''annoncer l''ouverture prochaine de son centre d''innovation au cœur de la ville de Douala. Ce hub accueillera développeurs, data scientists et designers. Nous recherchons des talents motivés pour rejoindre cette aventure : consultez nos offres dès maintenant et postulez via ProLink.',
       NOW() - INTERVAL '2 days', 340, u.id
FROM users u WHERE u.email = 'demo-emploi-techcorp@prolink.cm';

INSERT INTO blog_posts (titre, contenu, date_publication, nb_vues, auteur_id)
SELECT 'Les 5 conseils pour réussir un entretien à distance',
       'Que vous soyez étudiant ou freelance, l''entretien vidéo est devenu incontournable. Voici 5 conseils : testez votre connexion, choisissez un fond neutre, préparez vos questions, soyez ponctuel et surtout, montrez votre motivation. Notre équipe RH partage ces astuces avec la communauté ProLink.',
       NOW() - INTERVAL '1 day', 285, u.id
FROM users u WHERE u.email = 'demo-emploi-bankcmr@prolink.cm';

INSERT INTO blog_posts (titre, contenu, date_publication, nb_vues, auteur_id)
SELECT 'Retour d''expérience : mon premier contrat freelance en un mois',
       'Après trois ans en entreprise, j''ai décidé de me lancer en freelance. Grâce à la plateforme, j''ai décroché mon premier contrat en moins d''un mois. Dans cet article, je partage mes conseils : soignez votre portfolio, fixez un TJM réaliste et restez régulier dans vos candidatures. Le plus important est de bien communiquer avec vos clients.',
       NOW() - INTERVAL '3 days', 410, u.id
FROM users u WHERE u.email = 'demo-free-alexis.simo@prolink.cm';

-- ---- Commentaires de démonstration ----
INSERT INTO commentaires (contenu, date_creation, auteur_id, post_id)
SELECT 'Très belle initiative, j''ai hâte de découvrir ce centre !',
       NOW() - INTERVAL '1 day',
       (SELECT id FROM users WHERE email='demo-free-nadia.kamga@prolink.cm'),
       (SELECT id FROM blog_posts WHERE titre='TechCorp ouvre son centre d''innovation à Douala');

INSERT INTO commentaires (contenu, date_creation, auteur_id, post_id)
SELECT 'Merci pour ces conseils, très utiles pour mon premier entretien.',
       NOW() - INTERVAL '12 hours',
       (SELECT id FROM users WHERE email='demo-etu-luc.ngassa@prolink.cm'),
       (SELECT id FROM blog_posts WHERE titre='Les 5 conseils pour réussir un entretien à distance');

INSERT INTO commentaires (contenu, date_creation, auteur_id, post_id)
SELECT 'Motivant ! Cela me donne confiance pour me lancer aussi.',
       NOW() - INTERVAL '6 hours',
       (SELECT id FROM users WHERE email='demo-etu-carlos.owo@prolink.cm'),
       (SELECT id FROM blog_posts WHERE titre='Retour d''expérience : mon premier contrat freelance en un mois');

-- ---- Réactions (likes) de démonstration ----
INSERT INTO reactions (date_reaction, utilisateur_id, post_id)
SELECT NOW() - INTERVAL '1 day',
       (SELECT id FROM users WHERE email='demo-free-samuel.mbaye@prolink.cm'),
       (SELECT id FROM blog_posts WHERE titre='TechCorp ouvre son centre d''innovation à Douala');

INSERT INTO reactions (date_reaction, utilisateur_id, post_id)
SELECT NOW() - INTERVAL '20 hours',
       (SELECT id FROM users WHERE email='demo-etu-flora.tonye@prolink.cm'),
       (SELECT id FROM blog_posts WHERE titre='Les 5 conseils pour réussir un entretien à distance');

INSERT INTO reactions (date_reaction, utilisateur_id, post_id)
SELECT NOW() - INTERVAL '10 hours',
       (SELECT id FROM users WHERE email='demo-free-sonia.djomga@prolink.cm'),
       (SELECT id FROM blog_posts WHERE titre='Retour d''expérience : mon premier contrat freelance en un mois');

-- ============================================================
-- 5) VÉRIFICATION FINALE
-- ============================================================
SELECT 'Recruteurs' AS type, count(*) AS total
  FROM users WHERE role='RECRUTEUR' AND email LIKE 'demo-%@prolink.cm'
UNION ALL
SELECT 'Freelances', count(*)
  FROM users WHERE role='FREELANCE' AND email LIKE 'demo-%@prolink.cm'
UNION ALL
SELECT 'Étudiants', count(*)
  FROM users WHERE role='ETUDIANT' AND email LIKE 'demo-%@prolink.cm'
UNION ALL
SELECT 'Offres approuvées', count(*)
  FROM offres WHERE statut='APPROUVEE'
UNION ALL
SELECT 'Posts du blog', count(*)
  FROM blog_posts
UNION ALL
SELECT 'Commentaires', count(*)
  FROM commentaires
UNION ALL
SELECT 'Réactions (likes)', count(*)
  FROM reactions;
