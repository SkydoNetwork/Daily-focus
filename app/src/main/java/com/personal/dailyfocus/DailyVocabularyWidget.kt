package com.personal.dailyfocus

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import java.util.Calendar

class DailyVocabularyWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        cancelScheduledUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DAILY_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, DailyVocabularyWidget::class.java)
                updateWidgets(context, manager, manager.getAppWidgetIds(component))
                scheduleNextUpdate(context)
            }

            else -> super.onReceive(context, intent)
        }
    }

    private fun updateWidgets(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return

        val words = FocusRepository(context.applicationContext).getTodaysWords()
        val launchIntent = Intent(context, HomeActivity::class.java)
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_daily_vocabulary)
            views.setOnClickPendingIntent(R.id.widgetRoot, launchPendingIntent)
            val detailsIntent = Intent(context, WordDetailsActivity::class.java)
            bindWord(
                context,
                views,
                R.id.widgetWordOne,
                R.id.widgetMeaningOne,
                R.id.widgetExampleOne,
                R.id.widgetEntryOne,
                words.getOrNull(0),
                detailsIntent,
            )
            bindWord(
                context,
                views,
                R.id.widgetWordTwo,
                R.id.widgetMeaningTwo,
                R.id.widgetExampleTwo,
                R.id.widgetEntryTwo,
                words.getOrNull(1),
                detailsIntent,
            )
            bindWord(
                context,
                views,
                R.id.widgetWordThree,
                R.id.widgetMeaningThree,
                R.id.widgetExampleThree,
                R.id.widgetEntryThree,
                words.getOrNull(2),
                detailsIntent,
            )
            views.setViewVisibility(
                R.id.widgetEmptyMessage,
                if (words.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE,
            )
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun bindWord(
        context: Context,
        views: RemoteViews,
        wordViewId: Int,
        meaningViewId: Int,
        exampleViewId: Int,
        entryViewId: Int,
        word: VocabularyWord?,
        detailsIntent: Intent,
    ) {
        views.setViewVisibility(
            entryViewId,
            if (word == null) android.view.View.GONE else android.view.View.VISIBLE,
        )
        if (word == null) return

        views.setTextViewText(wordViewId, word.word)
        views.setTextViewText(
            meaningViewId,
            context.getString(R.string.word_meaning, context.getString(R.string.meaning_label), word.meaning),
        )
        views.setTextViewText(
            exampleViewId,
            context.getString(R.string.word_example, context.getString(R.string.example_label), word.example),
        )
        val wordIntent = Intent(detailsIntent)
            .putExtra(WordDetailsActivity.EXTRA_WORD, word.word)
            .setData(Uri.parse("dailyfocus://word/${Uri.encode(word.word)}"))
        val pendingIntent = PendingIntent.getActivity(
            context,
            word.word.hashCode(),
            wordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(entryViewId, pendingIntent        )
    }

    private fun scheduleNextUpdate(context: Context) {
        val nextMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight.timeInMillis,
            updatePendingIntent(context),
        )
    }

    private fun cancelScheduledUpdate(context: Context) {
        alarmManager(context).cancel(updatePendingIntent(context))
    }

    private fun updatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyVocabularyWidget::class.java).setAction(ACTION_DAILY_UPDATE)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    private companion object {
        const val ACTION_DAILY_UPDATE = "com.personal.dailyfocus.action.DAILY_WIDGET_UPDATE"
        const val REQUEST_CODE_DAILY_UPDATE = 1
    }
}
