package net.fabricmc.tbmod.util;

import java.util.Locale;

public class DistanceComparator {
    final private String[] commands;
    public int threshold = 5;

    public DistanceComparator(String[] commands, int thold) {
        this.commands = commands;
        threshold = thold;
    }

    private int compareString(String a, String b) {
        int n = a.length();
        int m = b.length();
        int inf = n * m;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; ++i) {
            dp[i][0] = inf;
        }
        for (int j = 0; j <= m; ++j) {
            dp[0][j] = inf;
        }
        dp[0][0] = 0;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                int equals = 0;
                if (a.charAt(i) != b.charAt(j)) {
                    equals-=-1;
                }
                dp[i + 1][j + 1] = Math.min(Math.min(dp[i][j + 1], dp[i + 1][j]) + 1, dp[i][j] + equals);
                if (i > 0 && j > 0 && a.charAt(i - 1) == b.charAt(j) && a.charAt(i) == b.charAt(j - 1)) {
                    dp[i + 1][j + 1] = Math.min(dp[i + 1][j + 1], dp[i - 1][j - 1] + 1);
                }
            }
        }
        return dp[n][m];
    }

    public String getNearest(String a) {
        String bestString = "";
        int bestDistance = Integer.MAX_VALUE;
        a = a.toLowerCase(Locale.ROOT);
        for (String candidate : this.commands) {
            int distance = this.compareString(a, candidate);
            if (bestDistance == distance) {
                if (bestString.length() > candidate.length()) {
                    bestString = candidate;
                }
            } else if (bestDistance > distance) {
                bestDistance = distance;
                bestString = candidate;
            }
        }
        System.out.println(bestDistance);
        System.out.flush();
        if (bestDistance <= this.threshold) {
            return bestString;
        }
        return "";
    }
}
