package com.mbta.tid.mbta_app.android.favorites

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.GregorianCalendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.LabeledSwitch
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.formattedTime
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

@OptIn(ExperimentalUuidApi::class)
@Composable
fun NotificationSettingsWidget(
    settings: FavoriteSettings.Notifications,
    setSettings: (FavoriteSettings.Notifications) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledSwitch(
            Modifier.haloContainer(borderWidth = 1.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            label = {
                if (settings.enabled) {
                    Icon(
                        painterResource(R.drawable.fa_bell_filled),
                        null,
                        tint = colorResource(R.color.key),
                    )
                } else {
                    Icon(painterResource(R.drawable.fa_bell), null)
                }
                Text(
                    stringResource(R.string.get_disruption_notifications),
                    Modifier.padding(start = 12.dp).weight(1f),
                )
            },
            value = settings.enabled,
            onValueChange = {
                setSettings(
                    settings.copy(
                        enabled = it,
                        windows = settings.windows.ifEmpty { listOf(defaultWindow()) },
                    )
                )
            },
        )
        AnimatedVisibility(settings.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (window in settings.windows.ifEmpty { setOf(defaultWindow()) }) {
                    WindowWidget(
                        window,
                        setWindow = { newWindow ->
                            val windows = settings.windows - window + newWindow
                            setSettings(settings.copy(windows = windows))
                        },
                        deleteWindow =
                            {
                                    setSettings(settings.copy(windows = settings.windows - window))
                                }
                                .takeIf { settings.windows.size > 1 },
                    )
                }
                Surface(
                    onClick = {
                        setSettings(
                            settings.copy(
                                windows = settings.windows + defaultWindow(settings.windows)
                            )
                        )
                    },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = colorResource(R.color.fill3),
                    border = BorderStroke(1.5.dp, colorResource(R.color.halo)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.plus),
                            null,
                            Modifier.background(
                                colorResource(R.color.text).copy(alpha = 0.6f),
                                CircleShape,
                            ),
                            tint = colorResource(R.color.fill3),
                        )
                        Text(
                            stringResource(R.string.add_another_time_period),
                            color = colorResource(R.color.text).copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

private fun defaultWindow(
    existingWindows: List<FavoriteSettings.Notifications.Window> = emptyList()
): FavoriteSettings.Notifications.Window {
    if (existingWindows.isEmpty()) {
        return FavoriteSettings.Notifications.Window(
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
            daysOfWeek =
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                ),
        )
    }
    return FavoriteSettings.Notifications.Window(
        startTime = LocalTime(12, 0),
        endTime = LocalTime(13, 0),
        setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    )
}

@Composable
private fun WindowWidget(
    window: FavoriteSettings.Notifications.Window,
    setWindow: (FavoriteSettings.Notifications.Window) -> Unit,
    deleteWindow: (() -> Unit)?,
) {
    Row(
        Modifier.haloContainer(1.dp, backgroundColor = colorResource(R.color.halo))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (deleteWindow != null) {
            Surface(
                onClick = deleteWindow,
                Modifier.minimumInteractiveComponentSize().fillMaxHeight().semantics {
                    role = Role.Button
                },
                color = Color.Transparent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.fa_delete),
                        stringResource(R.string.delete),
                        tint = colorResource(R.color.error),
                    )
                }
            }
        }
        Column(Modifier.background(colorResource(R.color.fill3), RoundedCornerShape(8.dp))) {
            LabeledTimeInput(
                stringResource(R.string.from),
                window.startTime,
                setTime = { setWindow(window.copy(startTime = it)) },
            )
            HaloSeparator()
            LabeledTimeInput(
                stringResource(R.string.to),
                window.endTime,
                setTime = { setWindow(window.copy(endTime = it)) },
            )
            DaysOfWeekInput(
                window.daysOfWeek,
                setDaysOfWeek = { setWindow(window.copy(daysOfWeek = it)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledTimeInput(label: String, time: LocalTime, setTime: (LocalTime) -> Unit) {
    var isPicking by remember { mutableStateOf(false) }
    val timePickerState =
        rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Button(
            onClick = { isPicking = true },
            shape = RoundedCornerShape(6.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.fill1),
                    contentColor = colorResource(R.color.text),
                ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                EasternTimeInstant(LocalDateTime(EasternTimeInstant.now().local.date, time))
                    .formattedTime(),
                style = Typography.footnoteSemibold,
            )
        }
    }
    if (isPicking) {
        AlertDialog(
            onDismissRequest = { isPicking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        setTime(LocalTime(timePickerState.hour, timePickerState.minute))
                        isPicking = false
                    }
                ) {
                    Text(stringResource(R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { isPicking = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimeInput(timePickerState) },
        )
    }
}

private val formatWeekdayAbbr by lazy { DateFormat.getInstanceForSkeleton(DateFormat.ABBR_WEEKDAY) }
private val formatWeekday by lazy { DateFormat.getInstanceForSkeleton(DateFormat.WEEKDAY) }

@Composable
private fun DaysOfWeekInput(daysOfWeek: Set<DayOfWeek>, setDaysOfWeek: (Set<DayOfWeek>) -> Unit) {
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // DayOfWeek.entries has Monday first and Sunday last
        for (day in
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
            )) {
            val calendarOnDay = GregorianCalendar()
            // unfortunately, Calendar.DAY_OF_WEEK uses Sun-Sat instead of the ISO Mon-Sun
            // fortunately, converting 7 to 1, 1 to 2, etc is the easiest operation available
            calendarOnDay.set(Calendar.DAY_OF_WEEK, day.isoDayNumber % 7 + 1)
            val isIncluded = day in daysOfWeek
            Surface(
                checked = isIncluded,
                onCheckedChange = { setDaysOfWeek(if (it) daysOfWeek + day else daysOfWeek - day) },
                modifier =
                    Modifier.semantics { role = Role.Checkbox }
                        .weight(1f)
                        .height(IntrinsicSize.Max),
                shape = RoundedCornerShape(6.dp),
                color = colorResource(if (isIncluded) R.color.key else R.color.fill1),
                contentColor =
                    if (isIncluded) colorResource(R.color.fill3)
                    else colorResource(R.color.text).copy(alpha = 0.6f),
            ) {
                Column(
                    Modifier.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        formatWeekdayAbbr.format(calendarOnDay),
                        Modifier.semantics {
                            text = AnnotatedString(formatWeekday.format(calendarOnDay))
                        },
                        style = Typography.footnoteSemibold,
                    )
                    if (isIncluded) {
                        Icon(painterResource(R.drawable.fa_check), null)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NotificationSettingsWidgetPreview() {
    var settings by remember {
        mutableStateOf(
            FavoriteSettings.Notifications(
                enabled = true,
                windows = listOf(defaultWindow(), defaultWindow(listOf(defaultWindow()))),
            )
        )
    }
    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill2))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            NotificationSettingsWidget(settings, { settings = it })
        }
    }
}
