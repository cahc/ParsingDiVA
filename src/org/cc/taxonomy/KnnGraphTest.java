package org.cc.taxonomy;

import org.cc.taxonomy.KnnGraph.UndirectedEdge;
import org.cc.taxonomy.SIMDDotProduct.KnnResult;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.cc.taxonomy.KnnGraph.Symmetrization.MUTUAL_MEAN;
import static org.cc.taxonomy.KnnGraph.Symmetrization.UNION_SUM;

/** Standalone regression checks for graph persistence, symmetrization, and export. */
public final class KnnGraphTest {
    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("knn-graph-test-");
        try {
            testCommandLine(directory);
            testRoundTripAndExport(directory);
        } finally {
            try (var files = Files.newDirectoryStream(directory)) {
                for (Path file : files) {
                    Files.delete(file);
                }
            }
            Files.delete(directory);
        }
        System.out.println("All kNN graph regression checks passed.");
    }

    private static void testRoundTripAndExport(Path directory) throws IOException {
        long[] pids = {700, 42, 4_294_967_301L, 98, 63};
        KnnResult[] rows = {
                new KnnResult(new int[]{2, 1}, new float[]{0.875f, 0.75f}),
                new KnnResult(new int[]{0, 2}, new float[]{0.5f, -0.0f}),
                new KnnResult(new int[]{1}, new float[]{0.0f}),
                new KnnResult(new int[]{1}, new float[]{-0.25f}),
                new KnnResult(new int[0], new float[0])
        };
        KnnGraph graph = KnnGraph.of(pids.clone(), rows);
        Path binary = directory.resolve("fixture.bin");
        graph.write(binary);
        KnnGraph loaded = KnnGraph.read(binary);
        check(loaded.vertexCount() == pids.length, "The vertex count must round-trip");
        for (int vertex = 0; vertex < pids.length; vertex++) {
            check(loaded.pid(vertex) == pids[vertex], "The PID must stay with its vertex");
            KnnResult row = loaded.neighbours(vertex);
            check(Arrays.equals(row.indices(), rows[vertex].indices()), "Neighbour rank must round-trip");
            check(Arrays.equals(row.similarities(), rows[vertex].similarities()), "Float weights must round-trip");
        }
        rows[0].indices()[0] = 4;
        loaded.neighbours(0).indices()[0] = 4;
        check(graph.neighbours(0).index(0) == 2 && loaded.neighbours(0).index(0) == 2, "Graph must own its rows");

        check(loaded.symmetrize(UNION_SUM).equals(List.of(
                new UndirectedEdge(0, 1, 1.25), new UndirectedEdge(0, 2, 0.875),
                new UndirectedEdge(1, 2, 0.0), new UndirectedEdge(1, 3, -0.25))), "Union weights and endpoints");
        check(loaded.symmetrize(MUTUAL_MEAN).equals(List.of(
                new UndirectedEdge(0, 1, 0.625), new UndirectedEdge(1, 2, 0.0))), "Mutual weights and endpoints");

        Path directed = directory.resolve("fixture-directed.net");
        Path union = directory.resolve("fixture-union.net");
        Path mutual = directory.resolve("fixture-mutual.net");
        Locale previousLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("sv-SE"));
        try {
            KnnGraph.writeDirectedPajek(directed, loaded);
            KnnGraph.writeUndirectedPajek(union, loaded, UNION_SUM);
            KnnGraph.writeUndirectedPajek(mutual, loaded, MUTUAL_MEAN);
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previousLocale);
        }
        String vertices = "*Vertices 5\n1 \"700\"\n2 \"42\"\n3 \"4294967301\"\n4 \"98\"\n5 \"63\"\n";
        check(Files.readString(directed).equals(vertices
                + "*Arcs\n1 3 0.875\n1 2 0.75\n2 1 0.5\n2 3 -0.0\n3 2 0.0\n4 2 -0.25\n"), "Directed Pajek file");
        check(Files.readString(union).equals(vertices
                + "*Edges\n1 2 1.25\n1 3 0.875\n2 3 0.0\n2 4 -0.25\n"), "Union Pajek file");
        check(Files.readString(mutual).equals(vertices + "*Edges\n1 2 0.625\n2 3 0.0\n"), "Mutual Pajek file");

        byte[] bytes = Files.readAllBytes(binary);
        Path corrupt = directory.resolve("corrupt.bin");
        checkRejected(corrupt, Arrays.copyOf(bytes, bytes.length - 1));
        checkRejected(corrupt, Arrays.copyOf(bytes, bytes.length + 1));
        checkRejected(corrupt, ByteBuffer.wrap(bytes.clone()).putInt(4, 2).array()); // Version
        checkRejected(corrupt, ByteBuffer.wrap(bytes.clone()).putInt(8, Integer.MAX_VALUE).array()); // Vertex count
        checkRejected(corrupt, ByteBuffer.wrap(bytes.clone()).putInt(20, Integer.MAX_VALUE).array()); // Degree
        checkRejected(corrupt, ByteBuffer.wrap(bytes.clone()).putInt(24, 5).array()); // Target outside graph
        checkRejected(corrupt, ByteBuffer.wrap(bytes.clone()).putFloat(28, Float.NaN).array()); // Weight
        checkRejected(corrupt, ByteBuffer.wrap(bytes.clone()).putInt(32, 2).array()); // Repeated target

        KnnGraph.of(new long[0], new KnnResult[0]).write(binary);
        check(KnnGraph.read(binary).vertexCount() == 0, "An empty graph must round-trip");
    }

    private static void testCommandLine(Path directory) throws IOException {
        Path embeddings = directory.resolve("embeddings.ser");
        Path binary = directory.resolve("graph.bin");
        Path directed = directory.resolve("directed.net");
        Path union = directory.resolve("union.net");
        Path mutual = directory.resolve("mutual.net");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(embeddings))) {
            out.writeObject(new EmbedDivaTsv.Result(new long[]{3, 1, 2},
                    new float[][]{{2, 0}, {10, 10}, {0, 10}}, new String[]{"A", "B", "C"}));
        }
        BuildKnnGraph.main(new String[]{embeddings.toString(), binary.toString(), "1"});
        try (var files = Files.list(directory)) {
            check(files.count() == 2, "Construction must only save the binary graph");
        }
        KnnGraph graph = KnnGraph.read(binary);
        check(graph.vertexCount() == 3 && graph.pid(0) == 3 && graph.pid(1) == 1 && graph.pid(2) == 2,
                "Graph construction must save the input PIDs in row order");
        check(graph.neighbours(0).index(0) == 1 && graph.neighbours(1).index(0) == 0
                        && Math.abs(graph.neighbours(0).similarity(0) - Math.sqrt(0.5)) < 1e-6,
                "Search must use normalized vectors and keep index-based tie-breaking");

        Files.delete(embeddings);
        ExportKnnGraphToPajek.main(new String[]{binary.toString(), "directed", directed.toString()});
        check(Files.notExists(union) && Files.notExists(mutual), "Export must write only the requested network");
        ExportKnnGraphToPajek.main(new String[]{binary.toString(), "union", union.toString()});
        ExportKnnGraphToPajek.main(new String[]{binary.toString(), "mutual", mutual.toString()});
        String header = "*Vertices 3\n1 \"3\"\n2 \"1\"\n3 \"2\"\n";
        check(Files.readString(directed).startsWith(header + "*Arcs\n"),
                "Directed export must work with only the serialized graph");
        check(Files.readString(union).startsWith(header + "*Edges\n"),
                "Union export must work with only the serialized graph");
        check(Files.readString(mutual).startsWith(header + "*Edges\n"),
                "Mutual export must work with only the serialized graph");
    }

    private static void checkRejected(Path path, byte[] bytes) throws IOException {
        Files.write(path, bytes);
        try {
            KnnGraph.read(path);
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Invalid binary graph was accepted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
