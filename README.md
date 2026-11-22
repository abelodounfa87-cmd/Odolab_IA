# ODOLAB AI

Appli Android : envoie des photos de pages de cours, l'IA (Gemini) en fait un résumé fidèle (≈ 45 % de la longueur : 10 pages → 4-5 pages), exportable en PDF.

## Installer l'APK depuis GitHub
1. Pousse ce dossier sur GitHub (branche `main`).
2. Onglet **Actions** → workflow **Build APK debug** (il se lance à chaque push, ou bouton *Run workflow*).
3. Quand il est vert, ouvre l'exécution → **Artifacts** → télécharge `ProfIA-debug-apk` → dézippe → installe `app-debug.apk`.

## Utilisation
- Colle ta clé API Gemini dans l'appli (gratuite sur aistudio.google.com). Elle reste sur le téléphone, jamais dans le code.
- Ajoute les photos dans l'ordre, puis « Résumer le cours ».

## Modifier le comportement
Tous les prompts sont dans `app/src/main/java/com/profia/app/Prompts.kt`.
Ratio de résumé et taille des groupes de pages : `Summarizer.kt` (`RATIO`, `CHUNK`).
