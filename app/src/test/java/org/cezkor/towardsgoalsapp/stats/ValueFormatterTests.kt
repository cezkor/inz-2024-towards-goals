package org.cezkor.towardsgoalsapp.stats

import com.google.common.truth.Truth.assertThat
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.Translation
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class ValueFormatterTests {

    @Test
    fun `unit formatter test`() {
        val f = UnitFormatter(Locale.US.language)
        assertThat(f.getFormattedValue(0f)).isEqualTo("0")
        assertThat(f.getFormattedValue(1.254f)).isEqualTo("1.25")
        f.unit = "cool unit"
        assertThat(f.getFormattedValue(0f)).isEqualTo("0 cool unit")
        assertThat(f.getFormattedValue(1.254f)).isEqualTo("1.25 cool unit")
    }

    @Test
    fun `epoch formatter test`() {
        val f = EpochFormatter(Locale.US.language)
        val pushBy = Instant.now().epochSecond
        f.pushForwardBySeconds = pushBy

        val formatter = DateTimeFormatterBuilder()
            .appendLocalized(FormatStyle.SHORT, null)
            .appendLiteral('\n')
            .appendLocalized(null, FormatStyle.SHORT)
            .toFormatter(Locale(Locale.US.language))

        val oneDaySeconds = 24*60*60L
        val odsToF = oneDaySeconds.toFloat()

        assertThat(f.getFormattedValue(0f))
            .isEqualTo(formatter.format(
                LocalDateTime.ofEpochSecond(pushBy, 0, ZoneOffset.UTC)
            )
        )
        assertThat(f.getFormattedValue(odsToF))
            .isEqualTo(formatter.format(
                LocalDateTime.ofEpochSecond(pushBy + oneDaySeconds, 0, ZoneOffset.UTC)
            )
        )
        assertThat(f.getFormattedValue(-odsToF))
            .isEqualTo(formatter.format(
                LocalDateTime.ofEpochSecond(pushBy - oneDaySeconds, 0, ZoneOffset.UTC)
            )
        )

    }

    @Test
    fun `day values formatter test`() {
        val mockTranslation = object : Translation() {
            override fun getString(resId: Int): String {
                return when (resId) {
                    R.string.stats_one_day_ago -> "day ago"
                    R.string.stats_today -> "today"
                    R.string.stats_next_day -> "next day"
                    R.string.stats_days_ago -> "past %s"
                    R.string.stats_in_x_days -> "future %s"
                    else -> "bad"
                }
            }

        }
        val f = DaysValueFormatter(mockTranslation)
        assertThat(f.nowIsPreviousDay).isFalse()

        val now = 10f

        f.current = now
        assertThat(f.getFormattedValue(now)).isEqualTo("today")
        assertThat(f.getFormattedValue(now-1f)).isEqualTo("day ago")
        assertThat(f.getFormattedValue(now+1f)).isEqualTo("next day")
        assertThat(f.getFormattedValue(now-5f)).isEqualTo("past 5")
        assertThat(f.getFormattedValue(now+5f)).isEqualTo("future 5")
        f.nowIsPreviousDay = true
        assertThat(f.getFormattedValue(now)).isEqualTo("next day")
        assertThat(f.getFormattedValue(now-1f)).isEqualTo("today")
        assertThat(f.getFormattedValue(now+1f)).isEqualTo("future 2")
        assertThat(f.getFormattedValue(now-5f)).isEqualTo("past 4")
        assertThat(f.getFormattedValue(now+5f)).isEqualTo("future 6")
    }
}