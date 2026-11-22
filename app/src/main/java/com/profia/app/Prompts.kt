package com.profia.app

object Prompts {

    val SYSTEM = """
Tu es un assistant qui résume des cours à partir de photos de pages.
Règles :
- Lis attentivement chaque image (écriture manuscrite ou imprimée, schémas, tableaux, formules).
- Reste fidèle au cours : n'invente rien, ne rajoute aucune information extérieure.
- Conserve les définitions, formules, dates, noms et exemples clés.
- Garde l'ordre et la structure du cours.
- Écris en français, de façon claire et concise.
- Utilise le format Markdown : titres avec #, ##, ###, listes avec "- ", mots importants en **gras**.
- Si un passage est illisible, écris [illisible] au lieu de deviner.
- Réponds uniquement avec le résumé, sans introduction ni commentaire.
""".trimIndent()

    private fun subjectLine(subject: String) =
        if (subject.isBlank()) "" else "Matière : $subject.\n"

    fun finalPrompt(pages: Int, targetPages: Int, words: Int, subject: String): String = """
${subjectLine(subject)}Les images ci-dessus sont les $pages pages du cours, dans l'ordre.
Rédige un résumé fidèle d'environ $words mots (≈ $targetPages page(s)), soit environ 45 % de la longueur du cours.
Structure-le avec des titres et des listes.
""".trimIndent()

    fun chunkPrompt(start: Int, end: Int, total: Int, words: Int, subject: String): String = """
${subjectLine(subject)}Les images ci-dessus sont les pages $start à $end d'un cours de $total pages.
Résume fidèlement ces pages en environ $words mots, en gardant toutes les notions importantes.
Structure avec des titres et des listes. Ce résumé sera fusionné avec les autres parties.
""".trimIndent()

    fun mergePrompt(partials: List<String>, targetPages: Int, words: Int, subject: String): String {
        val body = partials.mapIndexed { i, p -> "=== Partie ${i + 1} ===\n$p" }.joinToString("\n\n")
        return """
${subjectLine(subject)}Voici les résumés successifs des parties d'un même cours :

$body

Fusionne-les en UN seul résumé cohérent d'environ $words mots (≈ $targetPages page(s)).
Supprime les répétitions, garde l'ordre du cours et une structure claire avec titres et listes.
""".trimIndent()
    }
}
