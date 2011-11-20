package net.audev.batteryinfowidget;

import net.audev.batteryinfowidget.BatteryInfo.UpdateWidgetService;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BatteryInfoRedux extends AppWidgetProvider {
	private static final String TAG = "BatteryInfo";
	
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	
		Log.d(TAG,"++ onUpdate");		
		context.startService(new Intent(context, UpdateWidgetService.class));	
		Log.d(TAG,"-- onUpdate");
	}

}
