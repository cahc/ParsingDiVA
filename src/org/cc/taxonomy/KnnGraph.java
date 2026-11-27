package org.cc.taxonomy;

import org.cc.taxonomy.SIMDDotProduct.KnnResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A directed kNN graph with the DiVA PID for each vertex. */
public final class KnnGraph {
    private static final int MAGIC = 0x4B4E4E47; // KNNG
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 3 * Integer.BYTES;
    private static final int VERTEX_BYTES = Long.BYTES + Integer.BYTES;
    private static final int ARC_BYTES = Integer.BYTES + Float.BYTES;

    private final long[] pids;
    private final KnnResult[] rows;

    public enum Symmetrization {
        /** Keep any linked pair and add its available arc weights. */
        UNION_SUM,
        /** Keep reciprocal pairs and average their arc weights. */
        MUTUAL_MEAN
    }

    /** Endpoints are zero-based vertex indices with source less than target. */
    public record UndirectedEdge(int source, int target, double weight) {}

    private KnnGraph(long[] pids, KnnResult[] rows) {
        this.pids = pids;
        this.rows = rows;
        validate();
    }

    /** Take a snapshot of the PIDs and the ranked rows returned by the search. */
    public static KnnGraph of(long[] pids, KnnResult[] rows) {
        if (pids.length != rows.length) {
            throw new IllegalArgumentException("Expected one neighbour row per PID");
        }
        KnnResult[] copy = new KnnResult[rows.length];
        for (int i = 0; i < rows.length; i++) {
            KnnResult row = rows[i];
            if (row == null || row.indices() == null || row.similarities() == null) {
                throw new IllegalArgumentException("Missing neighbour data at vertex " + i);
            }
            copy[i] = copyRow(row);
        }
        return new KnnGraph(pids.clone(), copy);
    }

    public int vertexCount() {
        return pids.length;
    }

    public long pid(int vertex) {
        return pids[vertex];
    }

    public KnnResult neighbours(int vertex) {
        return copyRow(rows[vertex]);
    }

    private static KnnResult copyRow(KnnResult row) {
        return new KnnResult(row.indices().clone(), row.similarities().clone());
    }

    private void validate() {
        Set<Long> seenPids = new HashSet<>();
        int[] seenInRow = new int[vertexCount()];
        for (int source = 0; source < vertexCount(); source++) {
            if (!seenPids.add(pids[source])) {
                throw new IllegalArgumentException("Duplicate PID " + pids[source]);
            }
            KnnResult row = rows[source];
            if (row.size() > vertexCount() - 1 || row.size() != row.similarities().length) {
                throw new IllegalArgumentException("Invalid neighbour count at vertex " + source);
            }
            int rowMark = source + 1;
            for (int rank = 0; rank < row.size(); rank++) {
                int target = row.index(rank);
                if (target < 0 || target >= vertexCount() || target == source || seenInRow[target] == rowMark
                        || !Float.isFinite(row.similarity(rank))) {
                    throw new IllegalArgumentException("Invalid neighbour at vertex " + source + ", rank " + rank);
                }
                seenInRow[target] = rowMark;
            }
        }
    }

    /** Write the versioned binary graph, including PIDs and neighbour rank order. */
    public void write(Path path) throws IOException {
        writeFile(path, temporary -> {
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(vertexCount());
                for (int source = 0; source < vertexCount(); source++) {
                    out.writeLong(pids[source]);
                    KnnResult row = rows[source];
                    out.writeInt(row.size());
                    for (int rank = 0; rank < row.size(); rank++) {
                        out.writeInt(row.index(rank));
                        out.writeFloat(row.similarity(rank));
                    }
                }
            }
        });
    }

    /** Read a graph written by {@link #write(Path)}. */
    public static KnnGraph read(Path path) throws IOException {
        try (FileChannel file = FileChannel.open(path);
             DataInputStream in = new DataInputStream(new BufferedInputStream(Channels.newInputStream(file)))) {
            long remaining = file.size() - HEADER_BYTES;
            if (in.readInt() != MAGIC) {
                throw new IOException("Invalid kNN graph header in " + path);
            }
            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported kNN graph version " + version + " in " + path);
            }
            int vertices = in.readInt();
            if (vertices < 0 || (long) vertices * VERTEX_BYTES > remaining) {
                throw new IOException("Invalid vertex count in " + path);
            }
            long[] pids = new long[vertices];
            KnnResult[] rows = new KnnResult[vertices];
            for (int source = 0; source < vertices; source++) {
                pids[source] = in.readLong();
                int degree = in.readInt();
                remaining -= VERTEX_BYTES;
                long arcBytes = (long) degree * ARC_BYTES;
                long verticesLeft = (long) (vertices - source - 1) * VERTEX_BYTES;
                if (degree < 0 || degree >= vertices || arcBytes > remaining - verticesLeft) {
                    throw new IOException("Invalid neighbour count at vertex " + source + " in " + path);
                }
                int[] indices = new int[degree];
                float[] weights = new float[degree];
                for (int rank = 0; rank < degree; rank++) {
                    indices[rank] = in.readInt();
                    weights[rank] = in.readFloat();
                }
                remaining -= arcBytes;
                rows[source] = new KnnResult(indices, weights);
            }
            if (in.read() != -1) {
                throw new IOException("Unexpected trailing data in " + path);
            }
            return new KnnGraph(pids, rows);
        } catch (EOFException e) {
            throw new IOException("Truncated kNN graph in " + path, e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid kNN graph in " + path + ": " + e.getMessage(), e);
        }
    }

    /** Return each undirected edge once, sorted by its two endpoint indices. */
    public List<UndirectedEdge> symmetrize(Symmetrization mode) {
        Objects.requireNonNull(mode, "mode");
        Map<Long, Float> weights = new HashMap<>();
        for (int source = 0; source < vertexCount(); source++) {
            KnnResult row = rows[source];
            for (int rank = 0; rank < row.size(); rank++) {
                weights.put(arcKey(source, row.index(rank)), row.similarity(rank));
            }
        }

        List<UndirectedEdge> edges = new ArrayList<>();
        for (int source = 0; source < vertexCount(); source++) {
            KnnResult row = rows[source];
            for (int rank = 0; rank < row.size(); rank++) {
                int target = row.index(rank);
                Float reverse = weights.get(arcKey(target, source));
                double weight = row.similarity(rank);
                if (reverse != null) {
                    if (source > target) {
                        continue;
                    }
                    weight += reverse;
                    if (mode == Symmetrization.MUTUAL_MEAN) {
                        weight /= 2;
                    }
                } else if (mode == Symmetrization.MUTUAL_MEAN) {
                    continue;
                }
                edges.add(new UndirectedEdge(Math.min(source, target), Math.max(source, target), weight));
            }
        }
        edges.sort(Comparator.comparingInt(UndirectedEdge::source).thenComparingInt(UndirectedEdge::target));
        return List.copyOf(edges);
    }

    private static long arcKey(int source, int target) {
        return ((long) source << Integer.SIZE) | target;
    }

    public static void writeDirectedPajek(Path path, KnnGraph graph) throws IOException {
        writeFile(path, temporary -> {
            try (BufferedWriter out = Files.newBufferedWriter(temporary)) {
                graph.writeVertices(out);
                out.write("*Arcs\n");
                for (int source = 0; source < graph.vertexCount(); source++) {
                    KnnResult row = graph.rows[source];
                    for (int rank = 0; rank < row.size(); rank++) {
                        writeLink(out, source, row.index(rank), Float.toString(row.similarity(rank)));
                    }
                }
            }
        });
    }

    public static void writeUndirectedPajek(Path path, KnnGraph graph, Symmetrization mode) throws IOException {
        List<UndirectedEdge> edges = graph.symmetrize(mode);
        writeFile(path, temporary -> {
            try (BufferedWriter out = Files.newBufferedWriter(temporary)) {
                graph.writeVertices(out);
                out.write("*Edges\n");
                for (UndirectedEdge edge : edges) {
                    writeLink(out, edge.source(), edge.target(), Double.toString(edge.weight()));
                }
            }
        });
    }

    private void writeVertices(BufferedWriter out) throws IOException {
        out.write("*Vertices " + vertexCount() + "\n");
        for (int vertex = 0; vertex < vertexCount(); vertex++) {
            out.write((vertex + 1) + " \"" + pids[vertex] + "\"\n");
        }
    }

    private static void writeLink(BufferedWriter out, int source, int target, String weight) throws IOException {
        out.write((source + 1) + " " + (target + 1) + " " + weight + "\n");
    }

    static void requireSeparateOutput(Path input, Path output) throws IOException {
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                || (Files.exists(output) && Files.isSameFile(input, output))) {
            throw new IOException("Output must be a separate file from the input: " + output);
        }
    }

    @FunctionalInterface
    private interface FileWriter {
        void write(Path temporary) throws IOException;
    }

    private static void writeFile(Path path, FileWriter writer) throws IOException {
        Path output = path.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(output.getParent(), "knn-graph-", ".tmp");
        try {
            writer.write(temporary);
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
