package com.smartremote.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import timber.log.Timber

class QuickControlWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.d("Widget update requested for ${appWidgetIds.size} widgets")
    }
}
