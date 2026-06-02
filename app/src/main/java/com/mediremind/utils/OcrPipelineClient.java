package com.mediremind.utils;



import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles the full OCR pipeline:
 *   1. Upload image → PaddleOCR → get jobId
 *   2. Poll until done → get jsonUrl
 *   3. Download JSONL → extract text
 *   4. Send text to Qwen/HuggingFace → get structured JSON
 *
 * Uses plain HttpURLConnection (no Dio/OkHttp) — compatible with Java Android.
 */
public class OcrPipelineClient {

    private static final String TAG = "OcrPipelineClient";

    // ── Endpoints ─────────────────────────────────────────────────────────────
    private static final String PADDLE_JOB_URL =
            "https://paddleocr.aistudio-app.com/api/v2/ocr/jobs";
    private static final String PADDLE_MODEL   = "PaddleOCR-VL-1.6";
    private static final String HF_API_URL     =
            "https://router.huggingface.co/v1/chat/completions";

    private static final String PADDLE_TOKEN = "From paddle OCR official Website Generate your own API key for PaddleVLOCR-1.6";
    private static final String HF_TOKEN     = "generate your own token from hugging face and place here";

    private static final int POLL_INTERVAL_MS  = 3000;
    private static final int MAX_POLL_ATTEMPTS = 40; // 2 minutes max

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler       = new Handler(Looper.getMainLooper());

    // ── Callback ──────────────────────────────────────────────────────────────
    public interface PipelineCallback {
        void onProgress(String message);
        void onSuccess(String structuredJson);
        void onError(String error);
    }

    // ── System prompt (same as pipeline file, verbatim) ───────────────────────
    private static final String SYSTEM_PROMPT =
        "You are a STRICT medical OCR extraction engine for Urdu/English prescription parsing.\n\n" +
        "ABSOLUTE RULE: Extract ONLY what is explicitly written. NO inference. NO guessing. NO hallucination.\n" +
        "Strip all HTML tags from input before processing.\n\n" +
        "════════════════════════════════\nMEDICINE NAME RULE\n════════════════════════════════\n" +
        "Always preserve full prefix in medicine_name:\n" +
        "Tab / Cap / Syp / Inj / Sachet / Rotacap → keep as-is\n" +
        "NEVER strip prefix. NEVER rename.\n\n" +
        "════════════════════════════════\nMEDICINE TYPE MAPPING\n════════════════════════════════\n" +
        "Tab → Tablet\nCap → Capsule\nSyp → Syrup\nInj → Injection\nSachet → Sachet\nRotacap → Rotacap\n" +
        "If unclear → null\n\n" +
        "════════════════════════════════\nFREQUENCY RULES\n════════════════════════════════\n" +
        "1+1+1 or صبح+دوپھر+شام → tds\n" +
        "1+1 or صبح+شام → morning_evening\n" +
        "1+0+1 → morning_evening\n" +
        "0+1+1 → afternoon_evening\n" +
        "0+0+1 or رات alone → night_only\n" +
        "1+0+0 or صبح alone → daily\n" +
        "روزانہ / روزانه → daily\n" +
        "ہر دو ہفتے / بفٹے → every_14_days\n" +
        "SOS / ضرورت پر / درد پر / بخار پر → sos\n\n" +
        "HARD RULES:\n" +
        "- NEVER map شام → night_only\n" +
        "- bd and morning_evening are the same — always use morning_evening\n" +
        "- schedule_pattern MUST match frequency_type exactly\n\n" +
        "════════════════════════════════\nTIMING HINT RULE\n════════════════════════════════\n" +
        "صبح → morning\nشام → evening\nرات → night\nدوپھر → afternoon\n" +
        "نہار منہ → morning\nfrequency=tds → morning_afternoon_evening\n" +
        "frequency=morning_evening → morning_evening\nfrequency=night_only → night\n" +
        "frequency=sos → null\nNo time word → null (needs_user_input=true)\n\n" +
        "════════════════════════════════\nMEAL RELATION RULES\n════════════════════════════════\n" +
        "کھانے سے پہلے → before_meal\nکھانے کے بعد → after_meal\n" +
        "نہار منہ → empty_stomach\n\n" +
        "════════════════════════════════\nDOSE QUANTITY RULE\n════════════════════════════════\n" +
        "ایک=1, دو=2, تین=3, چار=4. Digits as integer. Unknown → null\n\n" +
        "════════════════════════════════\nDURATION RULE\n════════════════════════════════\n" +
        "5 دن → 5, 1 ہفتہ → 7, 2 ہفتے → 14. Not written → null\n\n" +
        "════════════════════════════════\nSOS RULE\n════════════════════════════════\n" +
        "SOS → frequency_type:sos, timing_hint:null, needs_user_input:false\n\n" +
        "════════════════════════════════\nCONFIDENCE SCORING\n════════════════════════════════\n" +
        "1.0=perfect, 0.8=minor noise, 0.6=some noise, 0.3=heavy noise, 0.0=unreadable\n\n" +
        "════════════════════════════════\nOUTPUT — JSON ONLY\n════════════════════════════════\n" +
        "No preamble. No explanation. No markdown fences.\n\n" +
        "{\"follow_up\":\"string or null\",\"medicines\":[{" +
        "\"medicine_name\":\"string\",\"medicine_type\":\"tablet|capsule|syrup|injection|sachet|rotacap|null\"," +
        "\"strength\":\"string\",\"dose_quantity\":\"integer or null\",\"dose_raw\":\"string or null\"," +
        "\"frequency_type\":\"daily|tds|morning_evening|night_only|every_14_days|sos|null\"," +
        "\"timing_hint\":\"morning|evening|night|afternoon|morning_evening|morning_afternoon_evening|null\"," +
        "\"meal_relation\":\"before_meal|after_meal|empty_stomach|null\"," +
        "\"special_instruction\":\"string or null\",\"schedule_pattern\":\"weekly|every_14_days|monthly|null\"," +
        "\"duration_days\":\"integer or null\",\"stat_dose\":\"true|false\"," +
        "\"needs_user_input\":\"true|false\",\"confidence\":\"0.0|0.3|0.6|0.8|1.0\"}]}";

    // ── Public entry point ─────────────────────────────────────────────────────

    /**
     * Run the full pipeline on a JPEG/PNG image file.
     */
    public void runPipeline(File imageFile, PipelineCallback callback) {
        executor.execute(() -> {
            try {
                // ── STEP 1: Upload image ──────────────────────────────────────
                post(callback, "Uploading prescription image…");
                String jobId = uploadImage(imageFile);
                post(callback, "Upload successful. Processing OCR…");

                // ── STEP 2: Poll for result ───────────────────────────────────
                String jsonUrl = pollForResult(jobId, callback);
                post(callback, "OCR complete. Extracting text…");

                // ── STEP 3: Download JSONL & extract text ─────────────────────
                String ocrText = downloadAndExtractText(jsonUrl);
                if (ocrText.trim().isEmpty()) {
                    throw new IOException("OCR returned empty text. Check image quality.");
                }
                post(callback, "Analysing prescription with AI…");

                // ── STEP 4: Send to Qwen via HuggingFace ─────────────────────
                String structuredJson = callLlm(ocrText);

                // Deliver on main thread
                mainHandler.post(() -> callback.onSuccess(structuredJson));

            } catch (Exception e) {
                Log.e(TAG, "Pipeline error", e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null
                        ? e.getMessage() : "Unknown pipeline error"));
            }
        });
    }

    // ── STEP 1: Upload ─────────────────────────────────────────────────────────

    private String uploadImage(File imageFile) throws IOException, JSONException {
        String boundary = "----MediRemindBoundary" + System.currentTimeMillis();
        URL url = new URL(PADDLE_JOB_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60_000);
        conn.setReadTimeout(120_000);
        conn.setRequestProperty("Authorization", "Bearer " + PADDLE_TOKEN);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        // Build optional payload JSON
        JSONObject optionalPayload = new JSONObject();
        optionalPayload.put("useDocOrientationClassify", false);
        optionalPayload.put("useDocUnwarping", false);
        optionalPayload.put("useChartRecognition", false);

        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

        // -- model field
        writeFormField(dos, boundary, "model", PADDLE_MODEL);

        // -- optionalPayload field
        writeFormField(dos, boundary, "optionalPayload", optionalPayload.toString());

        // -- file field
        String fileName = imageFile.getName();
        String mimeType = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"\r\n");
        dos.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

        FileInputStream fis = new FileInputStream(imageFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = fis.read(buf)) != -1) dos.write(buf, 0, len);
        fis.close();
        dos.writeBytes("\r\n");
        dos.writeBytes("--" + boundary + "--\r\n");
        dos.flush();
        dos.close();

        int status = conn.getResponseCode();
        String body = readStream(status < 400
                ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();

        if (status >= 400) throw new IOException("Upload failed (" + status + "): " + body);

        JSONObject resp = new JSONObject(body);
        return resp.getJSONObject("data").getString("jobId");
    }

    private void writeFormField(DataOutputStream dos, String boundary,
                                 String name, String value) throws IOException {
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        dos.write(value.getBytes(StandardCharsets.UTF_8));
        dos.writeBytes("\r\n");
    }

    // ── STEP 2: Poll ───────────────────────────────────────────────────────────

    private String pollForResult(String jobId, PipelineCallback callback)
            throws IOException, JSONException, InterruptedException {
        int attempt = 0;
        while (attempt < MAX_POLL_ATTEMPTS) {
            Thread.sleep(POLL_INTERVAL_MS);
            attempt++;
            post(callback, "Processing… (" + (attempt * 3) + "s)");

            URL url = new URL(PADDLE_JOB_URL + "/" + jobId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("Authorization", "Bearer " + PADDLE_TOKEN);

            int status = conn.getResponseCode();
            String body = readStream(status < 400
                    ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (status >= 400) throw new IOException("Poll failed (" + status + "): " + body);

            JSONObject resp = new JSONObject(body);
            JSONObject data = resp.getJSONObject("data");
            String state = data.getString("state");

            if ("done".equals(state)) {
                return data.getJSONObject("resultUrl").getString("jsonUrl");
            }
            if ("failed".equals(state)) {
                throw new IOException("OCR job failed on server.");
            }
            // state == "processing" → continue polling
        }
        throw new IOException("OCR timed out after " + (MAX_POLL_ATTEMPTS * 3) + " seconds.");
    }

    // ── STEP 3: Download JSONL & extract text ──────────────────────────────────

    private String downloadAndExtractText(String jsonUrl)
            throws IOException, JSONException {
        URL url = new URL(jsonUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(30_000);

        String body = readStream(conn.getInputStream());
        conn.disconnect();

        StringBuilder ocrText = new StringBuilder();
        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            JSONObject parsed = new JSONObject(line);
            JSONArray results = parsed.getJSONObject("result")
                    .getJSONArray("layoutParsingResults");
            for (int i = 0; i < results.length(); i++) {
                JSONObject r = results.getJSONObject(i);
                if (!r.isNull("markdown")) {
                    JSONObject md = r.getJSONObject("markdown");
                    if (!md.isNull("text")) {
                        String text = md.getString("text");
                        // Strip HTML tags
                        text = text.replaceAll("<[^>]*>", "").trim();
                        if (!text.isEmpty()) {
                            ocrText.append(text).append("\n");
                        }
                    }
                }
            }
        }
        return ocrText.toString().trim();
    }

    // ── STEP 4: Call LLM (Qwen via HuggingFace) ───────────────────────────────

    private String callLlm(String ocrText) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "Qwen/Qwen2.5-72B-Instruct");
        requestBody.put("max_tokens", 2048);

        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.put(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content",
                "Extract all medicines from this prescription OCR text:\n\n" + ocrText);
        messages.put(userMsg);

        requestBody.put("messages", messages);
        byte[] body = requestBody.toString().getBytes(StandardCharsets.UTF_8);

        URL url = new URL(HF_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(120_000);
        conn.setReadTimeout(120_000);
        conn.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));

        conn.getOutputStream().write(body);
        conn.getOutputStream().flush();

        int status = conn.getResponseCode();
        String response = readStream(status < 400
                ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();

        if (status >= 400) throw new IOException("LLM call failed (" + status + "): " + response);

        JSONObject respJson = new JSONObject(response);
        String content = respJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

        // Strip markdown fences if present
        if (content.startsWith("```")) {
            content = content.replaceAll("```json\\s*", "")
                             .replaceAll("```\\s*", "")
                             .trim();
        }
        return content;
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        return sb.toString().trim();
    }

    private void post(PipelineCallback callback, String message) {
        mainHandler.post(() -> callback.onProgress(message));
    }
}
