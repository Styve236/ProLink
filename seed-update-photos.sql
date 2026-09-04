-- ============================================================
-- ProLink - UPDATE des photos de profil (portraits africains Pexels)
-- À exécuter sur la base Neon (SQL Editor) APRÈS le seed-demo.sql
-- Idempotent : ne modifie que les emails listés, leave les societes et
-- les autres comptes intacts.
-- Photos validees HTTP 200 (16/09/2026).
-- ============================================================

UPDATE users
SET photo_url = CASE email
  -- ---- Recruteurs (personnes physiques) ----
  WHEN 'demo-paul.mboungou@prolink.cm'   THEN 'https://images.pexels.com/photos/9363113/pexels-photo-9363113.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-marie.ngono@prolink.cm'     THEN 'https://images.pexels.com/photos/12008288/pexels-photo-12008288.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-jean.eteme@prolink.cm'      THEN 'https://images.pexels.com/photos/12311537/pexels-photo-12311537.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-amina.sali@prolink.cm'      THEN 'https://images.pexels.com/photos/29852895/pexels-photo-29852895.jpeg?auto=compress&cs=tinysrgb&w=200'

  -- ---- Freelances ----
  WHEN 'demo-free-alexis.simo@prolink.cm'    THEN 'https://images.pexels.com/photos/12311567/pexels-photo-12311567.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-clarisse.ndoum@prolink.cm' THEN 'https://images.pexels.com/photos/7468194/pexels-photo-7468194.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-samuel.mbaye@prolink.cm'   THEN 'https://images.pexels.com/photos/12311572/pexels-photo-12311572.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-nadia.kamga@prolink.cm'    THEN 'https://images.pexels.com/photos/34769115/pexels-photo-34769115.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-yves.tchoua@prolink.cm'    THEN 'https://images.pexels.com/photos/12311549/pexels-photo-12311549.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-laurie.esso@prolink.cm'    THEN 'https://images.pexels.com/photos/32288633/pexels-photo-32288633.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-brice.kenfack@prolink.cm'  THEN 'https://images.pexels.com/photos/12311564/pexels-photo-12311564.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-grace.ngassa@prolink.cm'   THEN 'https://images.pexels.com/photos/35572078/pexels-photo-35572078.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-eliott.fotso@prolink.cm'   THEN 'https://images.pexels.com/photos/33331334/pexels-photo-33331334.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-free-sonia.djomga@prolink.cm'   THEN 'https://images.pexels.com/photos/37079379/pexels-photo-37079379.jpeg?auto=compress&cs=tinysrgb&w=200'

  -- ---- Etudiants ----
  WHEN 'demo-etu-luc.ngassa@prolink.cm'       THEN 'https://images.pexels.com/photos/35129364/pexels-photo-35129364.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-adeline.mefire@prolink.cm'  THEN 'https://images.pexels.com/photos/32846944/pexels-photo-32846944.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-kevin.abadie@prolink.cm'    THEN 'https://images.pexels.com/photos/34592823/pexels-photo-34592823.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-flora.tonye@prolink.cm'     THEN 'https://images.pexels.com/photos/26985383/pexels-photo-26985383.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-boris.ebogo@prolink.cm'     THEN 'https://images.pexels.com/photos/30124371/pexels-photo-30124371.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-ines.mbarga@prolink.cm'     THEN 'https://images.pexels.com/photos/27038743/pexels-photo-27038743.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-carlos.owo@prolink.cm'      THEN 'https://images.pexels.com/photos/32064778/pexels-photo-32064778.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-nelie.fonkwa@prolink.cm'    THEN 'https://images.pexels.com/photos/18509401/pexels-photo-18509401.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-garrick.mbenoun@prolink.cm' THEN 'https://images.pexels.com/photos/19111262/pexels-photo-19111262.jpeg?auto=compress&cs=tinysrgb&w=200'
  WHEN 'demo-etu-tatiana.bekono@prolink.cm'  THEN 'https://images.pexels.com/photos/10226305/pexels-photo-10226305.jpeg?auto=compress&cs=tinysrgb&w=200'

  ELSE photo_url
END
WHERE email IN (
  -- Recruteurs
  'demo-paul.mboungou@prolink.cm',
  'demo-marie.ngono@prolink.cm',
  'demo-jean.eteme@prolink.cm',
  'demo-amina.sali@prolink.cm',
  -- Freelances
  'demo-free-alexis.simo@prolink.cm',
  'demo-free-clarisse.ndoum@prolink.cm',
  'demo-free-samuel.mbaye@prolink.cm',
  'demo-free-nadia.kamga@prolink.cm',
  'demo-free-yves.tchoua@prolink.cm',
  'demo-free-laurie.esso@prolink.cm',
  'demo-free-brice.kenfack@prolink.cm',
  'demo-free-grace.ngassa@prolink.cm',
  'demo-free-eliott.fotso@prolink.cm',
  'demo-free-sonia.djomga@prolink.cm',
  -- Etudiants
  'demo-etu-luc.ngassa@prolink.cm',
  'demo-etu-adeline.mefire@prolink.cm',
  'demo-etu-kevin.abadie@prolink.cm',
  'demo-etu-flora.tonye@prolink.cm',
  'demo-etu-boris.ebogo@prolink.cm',
  'demo-etu-ines.mbarga@prolink.cm',
  'demo-etu-carlos.owo@prolink.cm',
  'demo-etu-nelie.fonkwa@prolink.cm',
  'demo-etu-garrick.mbenoun@prolink.cm',
  'demo-etu-tatiana.bekono@prolink.cm'
);

-- Vérification : nombres attendus après UPDATE
SELECT role,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE photo_url LIKE '%pexels.com%') AS avec_photo_pexels
FROM users
WHERE email LIKE 'demo-%'
GROUP BY role
ORDER BY role;
