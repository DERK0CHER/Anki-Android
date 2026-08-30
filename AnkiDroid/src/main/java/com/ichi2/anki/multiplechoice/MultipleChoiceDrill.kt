/*
 *  Copyright (c) 2026 AnkiDroid Open Source Team
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Leitner style drilling for multiple choice cards.
 *
 * A card counts as learned once it has been answered correctly [REQUIRED_STREAK] times in a
 * row; a single wrong pick sends it back to the start. The repetitions themselves are carried
 * out by Anki's own scheduler: the deck's learning steps decide how often a card comes back and
 * how many other cards are shown in between, which is why [DRILL_LEARNING_STEPS] holds one step
 * per required repetition.
 *
 * This file deliberately contains no Android dependencies so that it can be unit tested on the JVM.
 */

package com.ichi2.anki.multiplechoice

/**
 * Tracks, for the current review session, how often each card was answered correctly in a row.
 *
 * The counts are intentionally session-scoped and in memory only: they describe how the drill is
 * going right now, whereas the durable scheduling lives in the collection.
 */
class MultipleChoiceDrill(
    /** Correct answers in a row needed before a card counts as learned */
    val requiredStreak: Int = REQUIRED_STREAK,
) {
    init {
        require(requiredStreak >= 1) { "requiredStreak must be at least 1, was $requiredStreak" }
    }

    private val streaks = mutableMapOf<Long, Int>()

    /** How often [cardId] has been answered correctly in a row, `0` if it hasn't or was wrong */
    fun streakOf(cardId: Long): Int = streaks[cardId] ?: 0

    /**
     * Records an answer.
     *
     * @return the card's new streak: one higher than before if [correct], otherwise `0`
     */
    fun record(
        cardId: Long,
        correct: Boolean,
    ): Int {
        val streak = if (correct) (streakOf(cardId) + 1).coerceAtMost(requiredStreak) else 0
        streaks[cardId] = streak
        return streak
    }

    /** Whether [cardId] has reached [requiredStreak] correct answers in a row */
    fun isLearned(cardId: Long): Boolean = streakOf(cardId) >= requiredStreak

    /** The number of cards which reached [requiredStreak] in this session */
    fun learnedCount(): Int = streaks.count { it.value >= requiredStreak }

    /** Forgets every streak, e.g. because the user restarted the session */
    fun clear() = streaks.clear()

    companion object {
        /** Correct answers in a row needed before a card counts as learned */
        const val REQUIRED_STREAK = 4

        /**
         * Learning steps, in minutes, which make the scheduler ask for [REQUIRED_STREAK]
         * correct answers before a card graduates.
         *
         * One step per required repetition. The steps grow so that later repetitions are further
         * apart, which is what puts other cards in between rather than showing the same card twice
         * in a row.
         */
        val DRILL_LEARNING_STEPS = listOf(1, 5, 15, 60)

        /** The same idea for a card which was already learned and then answered wrongly */
        val DRILL_RELEARNING_STEPS = listOf(5, 15, 30, 60)
    }
}
