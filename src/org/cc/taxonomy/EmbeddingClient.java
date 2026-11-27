package org.cc.taxonomy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

public class EmbeddingClient implements AutoCloseable {
    public static final String HARRIER_270M = "microsoft/harrier-oss-v1-270m";
    public static final String HARRIER_0_6B = "microsoft/harrier-oss-v1-0.6b";
    public static final String QWEN_0_6B = "qwen3-embedding-0.6b";
    public static final String QWEN_4B = "qwen3-embedding-4b";

    // Microsoft's STS22.v2 document-document instruction. Use on BOTH sides.
    public static final String DOCUMENT_SIMILARITY_TASK_HARRIER =
            "Given a document, retrieve semantically related documents";

    public static final String MESH_QUERY_TASK_QWEN =
            "Retrieve the MeSH concept that is denoted by the biomedical keyword. Prefer conceptual equivalence over broader topical relatedness.";
    public static final String MESH_QUERY_INSTRUCTION =
            "Given a biomedical keyword or phrase, retrieve the MeSH concept that denotes the same meaning. "
                    + "Prefer conceptual equivalence over broader topical relatedness.";
    public static final String MESH_QUERY_PREFIX = "Instruct: " + MESH_QUERY_INSTRUCTION + "\nQuery:";

    /**
     * Format a Harrier/Qwen instruction in the client. A null task leaves text intact.
     * Use the same task for every document in a symmetric similarity collection.
     * For asymmetric retrieval, prefix queries and leave candidate documents raw.
     * Pass null as embed's wire task to avoid server-side prompt processing.
     */
    public static String prefix(String text, String taskDefinition) {
        Objects.requireNonNull(text, "text");
        if (taskDefinition == null) {
            return text;
        }
        if (taskDefinition.isBlank()) {
            throw new IllegalArgumentException("taskDefinition must be nonblank or null");
        }
        return "Instruct: " + taskDefinition + "\nQuery: " + text;
    }

    /** Format a batch without modifying the caller's array. */
    public static String[] prefix(String[] texts, String taskDefinition) {
        Objects.requireNonNull(texts, "texts");
        String[] result = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            result[i] = prefix(texts[i], taskDefinition);
        }
        return result;
    }

    // --- Inner classes for JSON requests/responses ---
    static class Request {
        String[] text;
        String action;
        String model;
        String task;
        int embedding_dim;
        boolean useHalfPrecision;
        boolean normalize;

        Request(String[] texts, String action, String model, String task,
                int embedding_dim, boolean useHalfPrecision, boolean normalize) {
            this.text = texts;
            this.action = action;
            this.model = model;
            this.task = task;
            this.embedding_dim = embedding_dim;
            this.useHalfPrecision = useHalfPrecision;
            this.normalize = normalize;
        }
    }

    static class ResponseHeader {
        int[] shape;  // e.g. [batchSize, embeddingDim]
        String dtype; // e.g. "float32"
        ModelInfo model_info;
    }

    public record ModelInfo(String repository, String revision, String device, String inference_dtype,
                            int max_sequence_length) { }

    public ModelInfo modelInfo(String model, boolean useHalfPrecision) {
        Request request = new Request(null, "model_info", model, null, 0, useHalfPrecision, true);
        send(request);
        byte[] reply = socket.recv(0);
        if (reply == null || socket.hasReceiveMore()) {
            throw new IllegalStateException("Expected a single-frame response for model_info.");
        }
        String json = new String(reply, ZMQ.CHARSET);
        throwIfServerError(json);
        return gson.fromJson(json, ModelInfo.class);
    }

    private void send(Request request) {
        if (!socket.send(gson.toJson(request).getBytes(ZMQ.CHARSET), 0)) {
            throw new IllegalStateException("Could not send embedding service request.");
        }
    }

    static void throwIfServerError(String responseJson) {
        JsonElement response;
        try {
            response = JsonParser.parseString(responseJson);
        } catch (JsonParseException invalidJson) {
            return; // The caller reports the unexpected response format.
        }
        if (response.isJsonObject()) {
            JsonElement error = response.getAsJsonObject().get("error");
            if (error != null) {
                String message = error.isJsonPrimitive() ? error.getAsString() : error.toString();
                throw new RuntimeException("Server error: " + message);
            }
        }
    }

    // --- Instance fields for ZMQ ---
    private final ZContext context;
    private final ZMQ.Socket socket;
    private final Gson gson;

    // --- Constructor ---
    public EmbeddingClient(String address) {
        // For instance: address = "tcp://localhost:5555"
        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.REQ);
        this.socket.connect(address);
        this.socket.setReceiveTimeOut(180000); // 60*1000*3 = 3 minutes timeout
        this.gson = new Gson();
    }

    /**
     * Send an embed request to the Python server.
     * Returns embeddings as a float[batchSize][embeddingDim].
     *
     * @param useHalfPrecision If true, request half-precision (float16) on the server
     *                         (bfloat16 for Harrier on supported CUDA GPUs)
     * @param task Legacy model task/prompt selector; pass null for Harrier and
     *             apply {@link #prefix(String, String)} before calling embed.
     */
    public float[][] embed(String[] texts, String model, String task, int embeddingDim, boolean useHalfPrecision, boolean normalize) {
        return embed(texts, model, task, embeddingDim, useHalfPrecision, normalize, null);
    }

    float[][] embed(String[] texts, String model, String task, int embeddingDim,
                    boolean useHalfPrecision, boolean normalize, ModelInfo expectedModel) {
        Request request = new Request(texts, "embed", model, task, embeddingDim, useHalfPrecision, normalize);
        send(request);

        byte[] part1 = socket.recv(0); // might be header or error
        if (part1 == null) {
            throw new RuntimeException("No response from server (header).");
        }

        byte[] part2 = null;
        if (socket.hasReceiveMore()) {
            part2 = socket.recv(0); // raw data
        }

        // If there is no second frame, treat first as a JSON error message
        if (part2 == null) {
            String maybeErrorJson = new String(part1, ZMQ.CHARSET);
            throwIfServerError(maybeErrorJson);
            throw new RuntimeException("Unexpected single-frame response from server. Raw: " + maybeErrorJson);
        }

        String headerJson = new String(part1, ZMQ.CHARSET);
        ResponseHeader header = gson.fromJson(headerJson, ResponseHeader.class);

        if (header == null || header.shape == null || header.shape.length != 2 || socket.hasReceiveMore()) {
            throw new RuntimeException("Invalid shape in header: " + headerJson);
        }
        if (!"float32".equals(header.dtype)) {
            throw new RuntimeException("Unsupported dtype from server: " + header.dtype);
        }
        if (expectedModel != null && !expectedModel.equals(header.model_info)) {
            throw new IllegalStateException("Model metadata changed during the run: " + header.model_info);
        }

        int batchSize = header.shape[0];
        int dimReceived = header.shape[1];
        if (batchSize != texts.length || dimReceived <= 0 || (embeddingDim > 0 && dimReceived != embeddingDim)) {
            throw new IllegalStateException("Unexpected embedding response shape: " + headerJson);
        }
        long expectedBytes = (long) batchSize * dimReceived * Float.BYTES;
        if (part2.length != expectedBytes) {
            throw new RuntimeException(
                    "Data size mismatch: got " + part2.length +
                            " bytes, expected " + expectedBytes
            );
        }



        FloatBuffer floats = ByteBuffer.wrap(part2).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[][] embeddings = new float[batchSize][dimReceived];
        for (float[] row : embeddings) {
            floats.get(row);
        }
        return embeddings;
    }

    /**
     * Embed sequential requests of at most batchSize texts, preserving input order.
     * Works with every model; arguments have the same meaning as in embed().
     * For Harrier, each request is also one GPU inference batch. Models using
     * SentenceTransformer.encode may additionally split requests internally.
     * An empty input returns an empty result without contacting the server.
     */
    public float[][] embedBatched(String[] texts, String model, String task,
                                 int embeddingDim, boolean useHalfPrecision,
                                 boolean normalize, int batchSize) {
        return embedBatched(texts, model, task, embeddingDim, useHalfPrecision, normalize, batchSize, completed -> {});
    }

    /** Reports the cumulative number of vectors after each successful batch. */
    float[][] embedBatched(String[] texts, String model, String task,
                          int embeddingDim, boolean useHalfPrecision,
                          boolean normalize, int batchSize, IntConsumer onBatchComplete) {
        Objects.requireNonNull(texts, "texts");
        Objects.requireNonNull(onBatchComplete, "onBatchComplete");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        float[][] result = new float[texts.length][];
        int dimension = -1;
        for (int start = 0; start < texts.length;) {
            int end = start + Math.min(batchSize, texts.length - start);
            float[][] batch = embed(Arrays.copyOfRange(texts, start, end),
                    model, task, embeddingDim, useHalfPrecision, normalize);
            if (batch.length != end - start) {
                throw new IllegalStateException("Server returned an unexpected number of embeddings");
            }
            for (float[] row : batch) {
                if (dimension < 0) {
                    dimension = row.length;
                } else if (row.length != dimension) {
                    throw new IllegalStateException("Server returned inconsistent embedding dimensions");
                }
            }
            System.arraycopy(batch, 0, result, start, batch.length);
            onBatchComplete.accept(end);
            start = end;
        }
        return result;
    }

    /**
     * Example for tokenization, if you need it.
     */
    public Map<String, List<List<Integer>>> tokenize(String[] texts, String model, boolean useHalfPrecision) {
        // 1) Create JSON request
        // "useHalfPrecision" is meaningless for tokenization here, so pass false or ignore
        Request request = new Request(texts, "tokenize", model, null, 0, useHalfPrecision, true);
        String requestJson = gson.toJson(request);

        // 2) Send JSON request
        socket.send(requestJson.getBytes(ZMQ.CHARSET), 0);

        // 3) Receive single JSON reply
        byte[] replyBytes = socket.recv(0);
        if (replyBytes == null) {
            throw new RuntimeException("No response from server for tokenize request.");
        }
        String replyJson = new String(replyBytes, ZMQ.CHARSET);
        throwIfServerError(replyJson);

        // 4) Parse tokens
        Type type = new TypeToken<Map<String, Map<String, List<List<Integer>>>>>() {}.getType();
        Map<String, Map<String, List<List<Integer>>>> tokenResponse = gson.fromJson(replyJson, type);
        Map<String, List<List<Integer>>> tokens = tokenResponse.get("tokens");
        return tokens;
    }

    // --- Clean up resources ---

    @Override
    public void close() {
        if (this.socket != null) {
            try {
                this.socket.setLinger(0);
            } catch (Exception ignored) {
            }
            this.socket.close();
        }
        this.context.close();
    }

}




