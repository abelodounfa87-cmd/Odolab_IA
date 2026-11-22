package com.profia.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    fun textPart(text: String): JSONObject = JSONObject().put("text", text)

    fun imagePart(base64Jpeg: String): JSONObject = JSONObject().put(
        "inline_data",
        JSONObject().put("mime_type", "image/jpeg").put("data", base64Jpeg)
    )

    fun generate(apiKey: String, model: String, system: String, parts: List<JSONObject>): String {
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(textPart(system)))
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("role", "user").put("parts", JSONArray(parts))
                )
            )
            .put("generationConfig", JSONObject().put("temperature", 0.3))
            .toString()

        var lastError = ""
        repeat(3) { attempt ->
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 30_000
                conn.readTimeout = 240_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code in 200..299) return parse(text)

                lastError = "HTTP $code : ${text.take(300)}"
                if (code != 429 && code != 503) throw RuntimeException(lastError)
            } finally {
                conn.disconnect()
            }
            Thread.sleep(3000L * (attempt + 1))
        }
        throw RuntimeException(lastError)
    }

    private fun parse(json: String): String {
        val parts = JSONObject(json)
            .optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw RuntimeException("Réponse vide (contenu bloqué ou illisible ?)")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
        return sb.toString().trim()
    }
}
