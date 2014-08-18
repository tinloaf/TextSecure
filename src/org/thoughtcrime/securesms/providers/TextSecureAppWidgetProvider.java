/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.securesms.providers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.RoutingActivity;
import org.thoughtcrime.securesms.database.DatabaseFactory;

import java.util.List;

/**
 * The provider for the TextSecure AppWidget
 *
 * @author Lukas Barth
 */
public class TextSecureAppWidgetProvider extends AppWidgetProvider {

  final static String UNREAD_COUNT = "unreadCount";

  public static void triggerUpdate(Context context, int unread) {
    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
    ComponentName widgetComponent = new ComponentName(context, TextSecureAppWidgetProvider.class);
    int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
    // widget notification
    Intent updateIntent = new Intent();
    updateIntent.putExtra(UNREAD_COUNT, unread);
    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    updateIntent.setClass(context, TextSecureAppWidgetProvider.class);
    context.sendBroadcast(updateIntent);
    // Samsung TouchWiz notification
    String launcherClassName = getLauncherClassName(context);
    if (launcherClassName != null) {
      Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
      intent.putExtra("badge_count", unread);
      intent.putExtra("badge_count_package_name", context.getPackageName());
      intent.putExtra("badge_count_class_name", launcherClassName);
      context.sendBroadcast(intent);
    }
    // Sony BadgeReceiver notification
    Intent intent = new Intent();
    intent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", RoutingActivity.class.getCanonicalName());
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", "org.thoughtcrime.securesms");
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", Integer.toString(unread));
    if(unread>0)
      intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", true);
    else
      intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", false);
    context.sendBroadcast(intent);
  }

  private static String getLauncherClassName(Context context) {
    PackageManager pm = context.getPackageManager();

    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);

    List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
    for (ResolveInfo resolveInfo : resolveInfos) {
      String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
      if (pkgName.equalsIgnoreCase(context.getPackageName())) {
        String className = resolveInfo.activityInfo.name;
        return className;
      }
    }
    return null;
  }

  public void onReceive(Context context, Intent intent) {
    // Protect against rogue update broadcasts (not really a security issue,
    // just filter bad broacasts out so subclasses are less likely to crash).
    String action = intent.getAction();
    if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
      Bundle extras = intent.getExtras();
      if (extras != null) {
        int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        int unread = extras.getInt(UNREAD_COUNT);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
          final int n = appWidgetIds.length;

          for (int i = 0; i < n; i++) {
            int appWidgetId = appWidgetIds[i];

            Intent clickIntent = new Intent(context, RoutingActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, clickIntent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.textsecure_appwidget);
            views.setOnClickPendingIntent(R.id.icon_view, pendingIntent);

            if (unread > 0) {
              if (unread > 9)
                views.setTextViewText(R.id.unread_count_text, "9+");
              else
                views.setTextViewText(R.id.unread_count_text, Integer.toString(unread));
              views.setViewVisibility(R.id.unread_count_text, View.VISIBLE);
            } else {
              views.setViewVisibility(R.id.unread_count_text, View.INVISIBLE);
            }

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
          }
        }
      }
    } else
      super.onReceive(context, intent);
  }
}
