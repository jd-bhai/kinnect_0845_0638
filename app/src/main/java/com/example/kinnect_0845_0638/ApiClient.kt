package com.example.kinnect_0845_0638

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {
    private const val TAG = "ApiClient"

    fun post(endpoint: String, params: Map<String, String>): String? {
        return try {
            val url = URL(ApiConfig.BASE_URL + endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val postData = StringBuilder()
            for ((key, value) in params) {
                if (postData.isNotEmpty()) postData.append('&')
                postData.append(URLEncoder.encode(key, "UTF-8"))
                postData.append('=')
                postData.append(URLEncoder.encode(value, "UTF-8"))
            }

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(postData.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                response.toString()
            } else {
                Log.e(TAG, "POST $endpoint failed with code ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST $endpoint Exception: ${e.message}", e)
            null
        }
    }

    fun get(endpoint: String, params: Map<String, String> = emptyMap()): String? {
        return try {
            val urlBuilder = StringBuilder(ApiConfig.BASE_URL + endpoint)
            if (params.isNotEmpty()) {
                urlBuilder.append('?')
                for ((key, value) in params) {
                    urlBuilder.append(URLEncoder.encode(key, "UTF-8"))
                    urlBuilder.append('=')
                    urlBuilder.append(URLEncoder.encode(value, "UTF-8"))
                    urlBuilder.append('&')
                }
            }

            val url = URL(urlBuilder.toString())
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                response.toString()
            } else {
                Log.e(TAG, "GET $endpoint failed with code ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $endpoint Exception: ${e.message}", e)
            null
        }
    }
}