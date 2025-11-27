package org.cc.applikationer;

public final class AuthorCredit {

    private AuthorCredit() {
        // utility class, no instances
    }

    /**
     * Straight fractionalization: every author gets 1/N.
     *
     * @param n total number of authors (N >= 1)
     * @param i 1-based index of the author in the byline (1 <= i <= N)
     * @return fractional credit for author i
     */
    public static double straightFractional(int n, int i) {
        validateInputs(n, i);
        return 1.0 / n;
    }

    /**
     * U-shaped fractional credit as in Aziz & Rozing (2013), Eq. (7).
     * First and last authors receive the highest weight; middle authors the lowest.
     * Best in Sundling "From Acknowledgment to Authorship" 2025
     * @param n total number of authors (N >= 1)
     * @param i 1-based index of the author in the byline (1 <= i <= N)
     * @return U-shaped fractional credit w'(n,i) for author i
     */
    public static double uShapedFractional(int n, int i) {
        validateInputs(n, i);

        // Relative weight w(n,i) from Eq. (4):
        // w(n,i) = (1 + |n + 1 - 2i|) / n
        //Thus, the formula is so constructed that 1/n <= w <= 1, whereby in the case of the first and the last author w always will equal 1, whereas in the case of authors near the median w will approach 1/n.
        double relative = (1.0 + Math.abs((double) n + 1.0 - 2.0 * i)) / n;

        // Sum over all authors from Eq. (6):
        // sum_{k=1}^n w(n,k) = n/2 + 1 - D
        // with D = 0 for even n, and D = 1/(2n) for odd n.
        double D = (n % 2 == 0) ? 0.0 : 1.0 / (2.0 * n);
        double sum = n / 2.0 + 1.0 - D;

        // Absolute weight w'(n,i) from Eq. (7)
        return relative / sum;
    }

    private static void validateInputs(int n, int i) {
        if (n <= 0) {
            throw new IllegalArgumentException("Number of authors n must be >= 1");
        }
        if (i < 1 || i > n) {
            throw new IllegalArgumentException("Author index i must be in [1, n]");
        }
    }

    /**
     * Edge-biased scheme using raw weights that are normalized to sum to 1.
     *
     * Raw weights:
     *  - first author: firstRaw
     *  - last author:  firstRaw * lastMultiplier   (if n > 1)
     *  - middle authors: middleRaw (all equal)
     *
     * Final fractional credit for author i is:
     *      r_i / sum_k r_k
     *
     * @param n             total number of authors (n >= 1)
     * @param i             1-based index of the author (1 <= i <= n)
     * @param firstRaw      raw weight for the first author (α >= 0)
     * @param lastMultiplier multiplier for the last author's raw weight (w >= 0)
     * @param middleRaw     raw weight for each middle author (m >= 0)
     * @return normalized fractional credit for author i
     */
    public static double edgeBiasedNormalized(int n, int i,
                                              double firstRaw,
                                              double lastMultiplier,
                                              double middleRaw) {
        validateInputs(n, i);

        if (firstRaw < 0.0) {
            throw new IllegalArgumentException("firstRaw must be >= 0");
        }
        if (lastMultiplier < 0.0) {
            throw new IllegalArgumentException("lastMultiplier must be >= 0");
        }
        if (middleRaw < 0.0) {
            throw new IllegalArgumentException("middleRaw must be >= 0");
        }

        // Handle n == 1 cleanly: single author gets everything.
        if (n == 1) {
            return 1.0;
        }

        // Compute sum of raw weights S(N)
        double sumRaw = 0.0;

        // First author
        sumRaw += firstRaw;

        if (n > 1) {
            // Last author (different from first because n >= 2 here)
            sumRaw += firstRaw * lastMultiplier;
        }

        // Middle authors (if any)
        if (n > 2) {
            sumRaw += (n - 2) * middleRaw;
        }

        if (sumRaw == 0.0) {
            throw new IllegalArgumentException(
                    "Sum of raw weights is zero; "
                            + "at least one of firstRaw, lastMultiplier*firstRaw, middleRaw must be > 0."
            );
        }

        // Raw weight for author i
        double raw;
        if (i == 1) {
            raw = firstRaw;
        } else if (i == n) {
            raw = firstRaw * lastMultiplier;
        } else {
            raw = middleRaw;
        }

        return raw / sumRaw;
    }


    /**
     * U-shaped edge-biased scheme using tunable U-shape and edge multipliers.
     *
     * Baseline U-shaped term (before shaping):
     *      base_i = 1 + |n + 1 - 2i|
     *
     * Shape:
     *      u_i = base_i^shape, with shape > 0
     *
     * Edge multipliers:
     *      m_1 = firstMultiplier
     *      m_n = lastMultiplier    (if n > 1)
     *      m_i = 1 for 1 < i < n
     *
     * Raw weights:
     *      r_i = m_i * u_i
     *
     * Normalized credit:
     *      c_i = r_i / sum_k r_k
     *
     * Special cases:
     *  - n == 1: single author gets 1.0, multipliers and shape irrelevant.
     *
     * @param n               total number of authors (n >= 1)
     * @param i               1-based index of the author (1 <= i <= n)
     * @param firstMultiplier multiplier for first author (>= 0)
     * @param lastMultiplier  multiplier for last author (>= 0)
     * @param shape           shape parameter for U-curve (> 0).
     *                        1.0 = original U-shape, >1 more extreme, <1 flatter.
     * @return normalized fractional credit for author i
     */
    public static double edgeBiasedUShapedNormalized(int n, int i,
                                                     double firstMultiplier,
                                                     double lastMultiplier,
                                                     double shape) {
        validateInputs(n, i);

        if (firstMultiplier < 0.0) {
            throw new IllegalArgumentException("firstMultiplier must be >= 0");
        }
        if (lastMultiplier < 0.0) {
            throw new IllegalArgumentException("lastMultiplier must be >= 0");
        }
        if (shape <= 0.0) {
            throw new IllegalArgumentException("shape must be > 0");
        }

        if (n == 1) {
            // Only one author, gets everything.
            return 1.0;
        }

        // Compute sum of raw weights
        double sumRaw = 0.0;
        for (int k = 1; k <= n; k++) {
            double base = 1.0 + Math.abs((double) n + 1.0 - 2.0 * k);
            double u = Math.pow(base, shape);

            double mult;
            if (k == 1) {
                mult = firstMultiplier;
            } else if (k == n) {
                mult = lastMultiplier;
            } else {
                mult = 1.0;
            }

            sumRaw += mult * u;
        }

        if (sumRaw == 0.0) {
            throw new IllegalArgumentException(
                    "Sum of raw weights is zero; at least one multiplier must be > 0."
            );
        }

        // Raw weight for author i
        double baseI = 1.0 + Math.abs((double) n + 1.0 - 2.0 * i);
        double uI = Math.pow(baseI, shape);

        double multI;
        if (i == 1) {
            multI = firstMultiplier;
        } else if (i == n) {
            multI = lastMultiplier;
        } else {
            multI = 1.0;
        }

        double rawI = multI * uI;
        return rawI / sumRaw;
    }





    public static void main(String[] args) {

        //test

        int N = 15;
        for(int i=1; i<=N; i++) {

            System.out.println(straightFractional(N, i) + " " + uShapedFractional(N, i) + " " + edgeBiasedNormalized(N,i,2,1.0,1) + "\t" + edgeBiasedUShapedNormalized(N,i,1,1,0.5) );
        }



    }

}
