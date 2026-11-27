package org.cc.taxonomy;

import java.io.IOException;
import java.nio.file.Path;

/** Loads a saved kNN graph and writes one Pajek network. */
public final class ExportKnnGraphToPajek {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: ExportKnnGraphToPajek graph.bin directed|union|mutual output.net");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[2]);
        KnnGraph.requireSeparateOutput(input, output);
        KnnGraph graph = KnnGraph.read(input);
        switch (args[1]) {
            case "directed" -> KnnGraph.writeDirectedPajek(output, graph);
            case "union" -> KnnGraph.writeUndirectedPajek(output, graph, KnnGraph.Symmetrization.UNION_SUM);
            case "mutual" -> KnnGraph.writeUndirectedPajek(output, graph, KnnGraph.Symmetrization.MUTUAL_MEAN);
            default -> throw new IllegalArgumentException("Expected directed, union, or mutual, got " + args[1]);
        }
        System.out.printf("Exported %s Pajek network for %,d publications to %s%n",
                args[1], graph.vertexCount(), output);
    }
}
