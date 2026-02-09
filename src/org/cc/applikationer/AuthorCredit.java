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


    /**
     * LiU-style ad-hoc "byline-aware" weighted fractionalization.
     *
     * <h2>What this method does</h2>
     * This is a pragmatic positional credit scheme that:
     * <ul>
     *   <li>Rewards first and last authors equally and strongly.</li>
     *   <li>Rewards second and second-last authors equally, but less than first/last.</li>
     *   <li>Allocates the remaining credit equally among all other ("middle") authors.</li>
     *   <li>Enforces two lower-bound ("floor") constraints:
     *       <ul>
     *         <li>First and last author never get less than 1/4 each.</li>
     *         <li>Second and second-last never get less than 1/8 each.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <h2>Why there is a two-regime algorithm</h2>
     * The informal documentation describes "never less than" constraints, which by themselves
     * would be underspecified. However, the accompanying example table is stated to be the
     * "exact distribution". The simplest scheme that reproduces those examples is:
     *
     * <ol>
     *   <li><b>Base (non-floor) rule:</b> Assign raw positional scores with a fixed 4:2:1 pattern
     *       and normalize to sum to 1:
     *       <ul>
     *         <li>First and last position: raw score 4 each</li>
     *         <li>Second and second-last: raw score 2 each (only when n >= 4)</li>
     *         <li>All remaining middle positions: raw score 1 each</li>
     *       </ul>
     *       Then w_i = score_i / sum(scores).</li>
     *   <li><b>Floor enforcement:</b> If the base rule would violate the floors for n >= 4,
     *       clamp the edge positions to the floor values and split the remaining mass equally
     *       among the middle authors.
     *       <ul>
     *         <li>w1 = wn = 1/4</li>
     *         <li>w2 = w_{n-1} = 1/8</li>
     *         <li>remaining = 1 - (2*(1/4) + 2*(1/8)) = 1/4</li>
     *         <li>each middle author gets remaining/(n-4) = 1/(4n - 16)</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <h2>Closed-form summary (for intuition)</h2>
     * <ul>
     *   <li>n = 1: [1]</li>
     *   <li>n = 2: [1/2, 1/2]</li>
     *   <li>n = 3: [4/10, 2/10, 4/10]  (base 4:2:4 normalization)</li>
     *   <li>4 <= n <= 8: normalize 4 (edges), 2 (near-edges), 1 (middle) i.e. denom = n + 8</li>
     *   <li>n >= 9: floors bind and weights are exactly
     *       w1=wn=1/4, w2=w_{n-1}=1/8, and middle=1/(4n-16)</li>
     * </ul>
     *
     * <h2>Usage</h2>
     * If your publication has a score S (e.g., JIF, citations, field-normalized score),
     * then author i receives: credit_i = S * liuBylineWeightedFractional(n, i).
     *
     * @param n total number of authors (n >= 1)
     * @param i 1-based index of the author in the byline (1 <= i <= n)
     * @return fractional credit for author i according to the LiU byline-aware scheme
     */
    public static double liuBylineWeightedFractional(int n, int i) {
        validateInputs(n, i);

        // Trivial edge cases: explicitly stated in the documentation and/or obvious.
        if (n == 1) {
            return 1.0;
        }
        if (n == 2) {
            return 0.5;
        }

        // n >= 3 from here
        //
        // STEP 1: Compute the "base" 4:2:1 normalized weights.
        //
        // For n == 3, the "second and second-last" position is the same author (i=2),
        // and the simplest consistent interpretation used by the example table is the
        // raw score pattern [4,2,4] (i.e., denom = 10).
        //
        // For n >= 4:
        //   sumScores = 4 + 4 + 2 + 2 + (n-4)*1 = n + 8.
        //
        // This base rule yields:
        //   w1 = wn = 4/(n+8)
        //   w2 = w_{n-1} = 2/(n+8)
        //   middle = 1/(n+8)
        //
        // STEP 2: Enforce floors for n >= 4:
        //   If base weights for edges would drop below floors, set edges to the floors
        //   and split the remainder equally among middle authors.
        //
        // The floors begin to bind at n >= 9 (because 4/(n+8) < 1/4 and 2/(n+8) < 1/8).
        // We compute it algorithmically rather than hard-coding the threshold.
        if (n == 3) {
            // raw: 4,2,4 -> denom 10
            if (i == 1 || i == 3) {
                return 4.0 / 10.0;
            }
            return 2.0 / 10.0;
        }

        // n >= 4
        final double baseDenom = n + 8.0;

        final double baseEdge = 4.0 / baseDenom;      // positions 1 and n
        final double baseNearEdge = 2.0 / baseDenom;  // positions 2 and n-1
        final double baseMiddle = 1.0 / baseDenom;    // positions 3..n-2

        final double edgeFloor = 0.25;   // 1/4
        final double nearEdgeFloor = 0.125; // 1/8

        // Check whether floors need to be enforced.
        // (This will be true for n >= 9, false for n <= 8.)
        final boolean floorsBind = (baseEdge < edgeFloor) || (baseNearEdge < nearEdgeFloor);

        if (!floorsBind) {
            // Base 4:2:1 normalized regime (n = 4..8).
            if (i == 1 || i == n) {
                return baseEdge;
            }
            if (i == 2 || i == n - 1) {
                return baseNearEdge;
            }
            return baseMiddle;
        }

        // Floor regime (typically n >= 9):
        //
        // Fix edges to floors, then distribute the remainder equally among middle authors.
        // Edges occupy 4 positions total: 1,2,n-1,n (since n >= 4 here).
        final double w1 = edgeFloor;
        final double wn = edgeFloor;
        final double w2 = nearEdgeFloor;
        final double wn1 = nearEdgeFloor;

        final double remainder = 1.0 - (w1 + wn + w2 + wn1); // = 0.25
        final int middleCount = n - 4;

        // Safety (should never happen for n >= 4)
        if (middleCount <= 0) {
            // This would only occur if n == 4, but floors don't bind at n == 4.
            // Still, guard against division by zero just in case.
            throw new IllegalStateException("Unexpected middleCount=" + middleCount + " for n=" + n);
        }

        final double middle = remainder / middleCount; // = 1/(4n-16)

        if (i == 1 || i == n) {
            return edgeFloor;
        }
        if (i == 2 || i == n - 1) {
            return nearEdgeFloor;
        }
        return middle;
    }


    public static void main(String[] args) {

        //test

        int N = 3;
        for(int i=1; i<=N; i++) {

            System.out.println(straightFractional(N, i) + " " + uShapedFractional(N, i) + " " + liuBylineWeightedFractional(N,i) );
        }



    }

}
