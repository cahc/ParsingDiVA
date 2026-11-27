package org.cc.taxonomy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmbedDivaTsv {
    private static final int EMBEDDING_DIMENSION = 2560;
    private static final int DEFAULT_BATCH_SIZE = 32;
    private static final int MAX_PREVIEW_CHARACTERS = 200;
    private static final String RESEARCH_TOPIC_TASK =
            "Identify the main and secondary research topics of this scholarly publication.";

    /** The PID, vector, and preview at index i describe the same publication. */
    public record Result(long[] pids, float[][] vectors, String[] textPreviews) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /** Reads the PID-aligned vectors written by this class. */
    public static Result read(Path path) throws IOException {
        try (ObjectInputStream stream = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            stream.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    Result.class.getName() + ";java.lang.String;!*"));
            if (stream.readObject() instanceof Result result) {
                return result;
            }
            throw new IOException("Expected serialized DiVA embeddings in " + path);
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not load serialized DiVA embeddings from " + path, e);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 4) {
            throw new IllegalArgumentException("Usage: EmbedDivaTsv [input.tsv] [output.ser] [server] [batchSize]");
        }
        Path input = Path.of(args.length > 0 ? args[0] : "diva-embedding-input.tsv");
        Path output = Path.of(args.length > 1 ? args[1] : "diva-embeddings.ser").toAbsolutePath().normalize();
        String server = args.length > 2 ? args[2] : "tcp://pro:5555";
        int batchSize = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_BATCH_SIZE;
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        if (Files.exists(output) && Files.isSameFile(input, output)) {
            throw new IOException("Embedding output must be a separate file from the input TSV");
        }

        Map<Long, String> textsByPid = readTsv(input);
        long[] pids = textsByPid.keySet().stream().mapToLong(Long::longValue).toArray();
        String[] textPreviews = textsByPid.values().stream()
                .map(EmbedDivaTsv::textPreview)
                .toArray(String[]::new);
        String[] texts = textsByPid.values().stream()
                .map(text -> EmbeddingClient.prefix(text, RESEARCH_TOPIC_TASK))
                .toArray(String[]::new);

        Path temporary = Files.createTempFile(output.getParent(), "diva-embeddings-", ".tmp");
        try {
            System.out.printf("Embedding %,d publications with %s in batches of %d%n",
                    pids.length, EmbeddingClient.QWEN_4B, batchSize);
            long startedAt = System.nanoTime();
            float[][] vectors;
            try (EmbeddingClient client = new EmbeddingClient(server)) {
                vectors = client.embedBatched(texts, EmbeddingClient.QWEN_4B, null,
                        EMBEDDING_DIMENSION, true, true, batchSize,
                        completed -> System.out.printf("Embedded %,d / %,d publications (%.1f%%) in %.1f s%n",
                                completed, pids.length, 100.0 * completed / pids.length,
                                (System.nanoTime() - startedAt) / 1_000_000_000.0));
            }

            try (ObjectOutputStream stream = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                stream.writeObject(new Result(pids, vectors, textPreviews));
            }
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        System.out.printf("Saved %,d embeddings to %s%n", pids.length, output);
    }

    private static String textPreview(String text) {
        if (text.codePointCount(0, text.length()) <= MAX_PREVIEW_CHARACTERS) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, MAX_PREVIEW_CHARACTERS)) + "[...]";
    }

    private static Map<Long, String> readTsv(Path input) throws IOException {
        Map<Long, String> textsByPid = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(input)) {
            int lineNumber = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                int tab = line.indexOf('\t');
                if (tab <= 0 || tab != line.lastIndexOf('\t') || line.substring(tab + 1).isBlank()) {
                    throw new IOException("Expected a PID and nonempty text on TSV line " + lineNumber);
                }

                long pid;
                try {
                    pid = Long.parseLong(line.substring(0, tab));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid PID on TSV line " + lineNumber, e);
                }
                if (textsByPid.putIfAbsent(pid, line.substring(tab + 1)) != null) {
                    throw new IOException("Duplicate PID " + pid + " on TSV line " + lineNumber);
                }
            }
        }
        if (textsByPid.isEmpty()) {
            throw new IOException("Embedding input TSV is empty: " + input);
        }
        return textsByPid;
    }
}
