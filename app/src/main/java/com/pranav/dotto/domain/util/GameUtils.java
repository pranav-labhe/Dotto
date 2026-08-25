package com.pranav.dotto.domain.util;

import java.util.Locale;

/**
 * Java utility class to demonstrate mixed Kotlin+Java project integration.
 * Provides helper methods for formatting game-related strings.
 */
public class GameUtils {

    /**
     * Formats the score string for display.
     * Consumed by Kotlin UI components.
     */
    public static String formatScore(String playerName, int score) {
        return String.format(Locale.getDefault(), "%s: %d", playerName, score);
    }

    /**
     * Returns a celebratory message for the winner.
     */
    public static String getWinMessage(String winnerName) {
        return winnerName.toUpperCase(Locale.getDefault()) + " WINS!";
    }
}
