package org.cc.taxonomy;

import java.io.IOException;
import java.nio.file.Path;

/** Builds and saves a directed kNN graph from the stored DiVA embeddings. */
public final class BuildKnnGraph {
    public static void main(String[] args) throws IOException {
        if (args.length > 3) {
            throw new IllegalArgumentException("Usage: BuildKnnGraph [embeddings.ser [graph.bin [k]]]");
        }
        Path input = Path.of(args.length >= 1 ? args[0] : "diva-embeddings.ser");
        Path output = Path.of(args.length >= 2 ? args[1] : "diva-knn.bin");
        int k = args.length >= 3 ? Integer.parseInt(args[2]) : 15;
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative");
        }
        KnnGraph.requireSeparateOutput(input, output);

        EmbedDivaTsv.Result embeddings = EmbedDivaTsv.read(input);
        float[][] vectors = SIMDDotProduct.normalizeInPlace(embeddings.vectors());
        System.out.printf("Building directed kNN graph for %,d publications with k=%d%n", vectors.length, k);
        long start = System.nanoTime();
        KnnGraph graph = KnnGraph.of(embeddings.pids(), SIMDDotProduct.knnGraph(vectors, k));
        graph.write(output);
        double seconds = (System.nanoTime() - start) / 1e9;
        System.out.printf("Saved graph with %,d publication PIDs to %s (%.3f seconds)%n",
                graph.vertexCount(), output, seconds);
    }
}
