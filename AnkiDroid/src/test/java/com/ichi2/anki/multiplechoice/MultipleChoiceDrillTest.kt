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

package com.ichi2.anki.multiplechoice

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

/** Tests [MultipleChoiceDrill] */
class MultipleChoiceDrillTest {
    private val drill = MultipleChoiceDrill()

    @Test
    fun `an unseen card has no streak`() {
        assertThat(drill.streakOf(1), equalTo(0))
        assertThat(drill.isLearned(1), equalTo(false))
    }

    @Test
    fun `correct answers raise the streak`() {
        assertThat(drill.record(1, correct = true), equalTo(1))
        assertThat(drill.record(1, correct = true), equalTo(2))
        assertThat(drill.streakOf(1), equalTo(2))
    }

    @Test
    fun `a card is learned after four correct answers in a row`() {
        repeat(3) { drill.record(1, correct = true) }
        assertThat(drill.isLearned(1), equalTo(false))

        assertThat(drill.record(1, correct = true), equalTo(4))
        assertThat(drill.isLearned(1), equalTo(true))
    }

    @Test
    fun `a wrong answer sends the card back to the start`() {
        repeat(3) { drill.record(1, correct = true) }

        assertThat(drill.record(1, correct = false), equalTo(0))
        assertThat(drill.isLearned(1), equalTo(false))
    }

    @Test
    fun `the streak has to be consecutive`() {
        drill.record(1, correct = true)
        drill.record(1, correct = true)
        drill.record(1, correct = false)
        repeat(3) { drill.record(1, correct = true) }

        assertThat(drill.streakOf(1), equalTo(3))
        assertThat(drill.isLearned(1), equalTo(false))
    }

    @Test
    fun `the streak does not grow beyond what is required`() {
        repeat(10) { drill.record(1, correct = true) }

        assertThat(drill.streakOf(1), equalTo(drill.requiredStreak))
    }

    @Test
    fun `cards are tracked independently`() {
        repeat(4) { drill.record(1, correct = true) }
        drill.record(2, correct = true)

        assertThat(drill.isLearned(1), equalTo(true))
        assertThat(drill.streakOf(2), equalTo(1))
        assertThat(drill.learnedCount(), equalTo(1))
    }

    @Test
    fun `clear forgets every streak`() {
        repeat(4) { drill.record(1, correct = true) }
        drill.clear()

        assertThat(drill.streakOf(1), equalTo(0))
        assertThat(drill.learnedCount(), equalTo(0))
    }

    @Test
    fun `there is one learning step per required repetition`() {
        assertThat(MultipleChoiceDrill.DRILL_LEARNING_STEPS.size, equalTo(MultipleChoiceDrill.REQUIRED_STREAK))
        assertThat(MultipleChoiceDrill.DRILL_RELEARNING_STEPS.size, equalTo(MultipleChoiceDrill.REQUIRED_STREAK))
    }

    @Test
    fun `the learning steps grow, so other cards come in between`() {
        val steps = MultipleChoiceDrill.DRILL_LEARNING_STEPS
        assertThat(steps.zipWithNext().all { (a, b) -> b > a }, equalTo(true))
    }
}
