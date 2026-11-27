package org.cc.taxonomy;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.IntStream;

public class SIMDDotProduct {

    //--add-modules jdk.incubator.vector to VM arguments


    /**
     * Result of an exact nearest-neighbour search.
     *
     * Neighbours are sorted by descending similarity.
     * Ties are resolved by ascending data index.
     */
    public record KnnResult(int[] indices, float[] similarities) {

        public int size() {
            return indices.length;
        }

        public int index(int rank) {
            return indices[rank];
        }

        public float similarity(int rank) {
            return similarities[rank];
        }
    }


    /**
     * Find the k nearest neighbours of an external query vector.
     *
     * Both the query and all data vectors must be L2-normalized.
     *
     * @param query normalized query vector
     * @param data normalized candidate vectors
     * @param k requested number of neighbours
     * @return neighbours sorted by descending similarity
     */
    public static KnnResult knn(
            float[] query,
            float[][] data,
            int k) {

        validateK(k);
        validateData(data, query.length);

        return knnInternal(query, data, -1, k);
    }


    /**
     * Find the k nearest neighbours of an existing data vector.
     *
     * The query vector itself is excluded from the results.
     * All data vectors must be L2-normalized.
     *
     * @param data normalized vectors
     * @param queryIndex index of the query vector
     * @param k requested number of neighbours
     * @return neighbours sorted by descending similarity
     */
    public static KnnResult knn(
            float[][] data,
            int queryIndex,
            int k) {

        validateK(k);

        if (queryIndex < 0 || queryIndex >= data.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid query index: " + queryIndex);
        }

        float[] query = data[queryIndex];

        validateData(data, query.length);

        return knnInternal(query, data, queryIndex, k);
    }


    /**
     * Construct an exact directed k-nearest-neighbour graph.
     *
     * All input vectors must be L2-normalized.
     * Self-neighbours are excluded.
     *
     * The graph is constructed in parallel.
     *
     * @param data normalized vectors
     * @param k requested number of neighbours per node
     * @return one neighbour list per input vector
     */
    public static KnnResult[] knnGraph(float[][] data, int k) {
        return knnGraph(data, k, true);
    }


    /**
     * Construct an exact directed kNN graph with optional
     * parallel execution.
     */
    public static KnnResult[] knnGraph(
            float[][] data,
            int k,
            boolean parallel) {

        validateK(k);

        int n = data.length;

        if (n == 0) {
            return new KnnResult[0];
        }

        validateData(data, data[0].length);

        KnnResult[] graph = new KnnResult[n];

        int effectiveK = Math.min(k, n - 1);

        IntStream stream = IntStream.range(0, n);

        if (parallel) {
            stream = stream.parallel();
        }

        stream.forEach(i -> {
            graph[i] = knnInternal(
                    data[i],
                    data,
                    i,
                    effectiveK
            );
        });

        return graph;
    }


    /**
     * Internal exhaustive kNN search.
     *
     * Input validation is performed by the public methods.
     * Uses sorted primitive arrays for top-k selection.
     */
    private static KnnResult knnInternal(
            float[] query,
            float[][] data,
            int excludeIndex,
            int k) {

        int available = data.length;

        if (excludeIndex >= 0) {
            available--;
        }

        int capacity = Math.min(k, available);

        int[] indices = new int[capacity];
        float[] similarities = new float[capacity];

        if (capacity == 0) {
            return new KnnResult(indices, similarities);
        }

        int size = 0;

        for (int j = 0; j < data.length; j++) {

            if (j == excludeIndex) {
                continue;
            }

            float similarity = dotProduct(query, data[j]);

            // Fast rejection:
            // If the list is full, compare against its weakest
            // neighbour before doing any further work.
            if (size == capacity) {

                int last = capacity - 1;

                if (!isBetter(
                        similarity, j,
                        similarities[last], indices[last])) {

                    continue;
                }
            }

            // Determine insertion position.
            int pos = Math.min(size, capacity - 1);

            // Shift weaker neighbours one position to the right.
            while (pos > 0 &&
                    isBetter(
                            similarity, j,
                            similarities[pos - 1],
                            indices[pos - 1])) {

                similarities[pos] = similarities[pos - 1];
                indices[pos] = indices[pos - 1];

                pos--;
            }

            // Insert candidate.
            similarities[pos] = similarity;
            indices[pos] = j;

            if (size < capacity) {
                size++;
            }
        }

        return new KnnResult(indices, similarities);
    }


    /**
     * Compare two nearest-neighbour candidates.
     *
     * Higher similarity is better.
     * For equal similarities, lower data index is better.
     */
    private static boolean isBetter(
            float similarityA,
            int indexA,
            float similarityB,
            int indexB) {

        return similarityA > similarityB ||
                (similarityA == similarityB && indexA < indexB);
    }


    /**
     * Validate k.
     */
    private static void validateK(int k) {

        if (k < 0) {
            throw new IllegalArgumentException(
                    "k must be non-negative.");
        }
    }


    /**
     * Validate matrix dimensionality.
     *
     * Normalization and finite-value validation are assumed
     * to have been performed when preparing the embeddings.
     */
    private static void validateData(
            float[][] data,
            int dimensions) {

        if (dimensions <= 0) {
            throw new IllegalArgumentException(
                    "Vector dimensionality must be positive.");
        }

        for (int i = 0; i < data.length; i++) {

            if (data[i] == null ||
                    data[i].length != dimensions) {

                throw new IllegalArgumentException(
                        "Invalid vector dimensionality at index " + i);
            }
        }
    }

    // Use the preferred species for your hardware
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    public static float dotProduct(float[] a, float[] b) {
        float sum = 0.0f;
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);
        FloatVector acc = FloatVector.zero(SPECIES);

        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
            acc = va.fma(vb, acc); // one could consider acc = acc.add(va.mul(vb)); but that would probably be slower (see simple benchmark numers below).
        }
        sum += acc.reduceLanes(VectorOperators.ADD);

        // Handle any remaining elements
        for (; i < a.length; i++) {
            sum += a[i] * b[i];
        }

        return sum;

    }

    // Compute the L2 norm (Euclidean norm) of a float array using SIMD
    private static float l2Norm(float[] a) {
        float sum = 0.0f;
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);
        FloatVector acc = FloatVector.zero(SPECIES);

        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            acc = acc.add(va.mul(va));
        }
        sum += acc.reduceLanes(VectorOperators.ADD);

        // Process any leftover elements
        for (; i < a.length; i++) {
            sum += a[i] * a[i];
        }
        return (float) Math.sqrt(sum);
    }

    // Compute cosine similarity between two float arrays
    public static float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be of the same length.");
        }
        float dot = dotProduct(a, b);
        float normA = l2Norm(a);
        float normB = l2Norm(b);

        // Avoid division by zero if one of the norms is zero
        if (normA == 0 || normB == 0) {
            throw new IllegalArgumentException("Input arrays must not be zero-vectors.");
        }
        float sim = dot / (normA * normB);

        return Math.max(-1.0f, Math.min(1.0f, sim));
    }

    // Normalize the vector in place using L2 norm and return the same array
    public static float[] normalizeInPlace(float[] a) {
        float norm = l2Norm(a);
        if (norm == 0) {
            throw new IllegalArgumentException("Input array must not be a zero-vector.");
        }
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);
        // SIMD loop to divide each element by the norm
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector v = FloatVector.fromArray(SPECIES, a, i);
            v = v.div(norm);
            v.intoArray(a, i);
        }
        // Process any remaining elements
        for (; i < a.length; i++) {
            a[i] /= norm;
        }
        return a;
    }

    // Return a new L2 normalized vector without altering the original
    public static float[] normalize(float[] a) {
        float[] copy = a.clone();
        return normalizeInPlace(copy);
    }


    /**
     * L2-normalise each row of a matrix in place.
     *
     * @param m 2-D float array (m[row][col])
     * @return the same matrix instance, now row-normalised
     */
    public static float[][] normalizeInPlace(float[][] m) {
        for (float[] row : m) {
            normalizeInPlace(row);   // uses the SIMD path already defined
        }
        return m;
    }

    /**
     * L2-normalise each row of a matrix without touching the original.
     *
     * @param m 2-D float array
     * @return a *new* matrix whose rows are L2-normalised
     */
    public static float[][] normalize(float[][] m) {
        float[][] copy = new float[m.length][];
        for (int r = 0; r < m.length; r++) {
            copy[r] = normalize(m[r]);   // clone + SIMD normalisation
        }
        return copy;
    }

    public static void main(String[] args) throws IOException {

        //EXAMPLES

        // --------------- Find the neighbours of an existing record ----------------

        EmbedDivaTsv.Result data = EmbedDivaTsv.read(Path.of("diva-embeddings.ser"));

        int queryIndex = 123;
        int k = 15;

        SIMDDotProduct.KnnResult result =
                SIMDDotProduct.knn(
                        data.vectors(),
                        queryIndex,
                        k
                );



        // -------------- Search using an external vector -------------

        float[][] normalizedData =
                SIMDDotProduct.normalize(data.vectors());

        float[] normalizedQuery =
                SIMDDotProduct.normalize(data.vectors()[7]); //"fake external"

        result =
                SIMDDotProduct.knn(
                        normalizedQuery,
                        normalizedData,
                        15
                );

        for (int rank = 0; rank < result.size(); rank++) {

            System.out.printf(
                    "%d: index=%d, similarity=%.6f text=%s%n",
                    rank + 1,
                    result.index(rank),
                    result.similarity(rank),
                    data.textPreviews()[result.index(rank)]
            );
        }




        // ------- Construct the entire sparse kNN graph -----

        /*
        long start = System.nanoTime();

        SIMDDotProduct.KnnResult[] graph =
                SIMDDotProduct.knnGraph(
                        normalizedData,
                        k
                );

        double seconds =
                (System.nanoTime() - start) / 1e9;

        System.out.printf(
                "Constructed kNN graph in %.3f seconds%n",
                seconds
        );

        */
    }

}
