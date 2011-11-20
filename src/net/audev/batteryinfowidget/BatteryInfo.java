package net.audev.batteryinfowidget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class BatteryInfo extends AppWidgetProvider {
	private static final String TAG = "BatteryInfo";
	
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	
		Log.d(TAG,"++ onUpdate");		
		context.startService(new Intent(context, UpdateWidgetService.class));	
		Log.d(TAG,"-- onUpdate");
	}
	
	/*
	 * El servicio que lleva el peso de la aplicación
	 */
	public static class UpdateWidgetService extends Service {		
		public static final String TAG = "BatteryInfo";
			
		AppWidgetManager manager;
		RemoteViews updateViews; 
        ComponentName thisWidget;      
        Context ctx;
        
		/*
		 * Registro del Receiver del estado de la batería que mantendremos todo el tiempo
		 */
        @Override
        public void onCreate() {
            super.onCreate();            
            Log.d(TAG,"++ onCreate");
 		    
            ctx = this;
            
            BatteryInfoData.getInstance().addBatteryDataChangedListener(new OnBatteryDataChanged() {
				
				@Override
				public void onBatteryDataChanged() {
					actualizar(ctx);
				}
			});
            
 		    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED); 		    
 		    registerReceiver(BatteryInfoData.getInstance(), filter);
 		    Log.d(TAG,"batteryReceived on");
            Log.d(TAG,"-- onCreate");
        }
        
        /*
         * Aquí llegaremos 1 vez cada 30 minutos
         */
        @Override
		public void onStart(Intent intent, int startId) {        	
		    Log.d(TAG,"++ onStart");

            thisWidget = new ComponentName(this, BatteryInfo.class);
            manager = AppWidgetManager.getInstance(this);
            
            actualizar(this);
		    Log.d(TAG,"-- onStart");
		}
		
		@Override
		public IBinder onBind(Intent arg0) {
			return null;
		}
		
		/*
		 * El método de actualización, threaded para no provocar bloqueos de UI 
		 */
		public synchronized void actualizar(final Context c)
		{
			Thread t = new Thread(){
				public void run() 
				{
					updateViews = buildUpdate(c);
					miHandler.post(new Runnable()
					{
						public void run()
						{
							Log.d(TAG,"Actualizando el widget (ya en thread ui)");
							if (manager!=null) manager.updateAppWidget(thisWidget, updateViews);
						}
					});
				}
			};
			t.start();
		}
		
		final Handler miHandler = new Handler();
			
		/*
		 * Poner la información en su sitio
		 */
		public RemoteViews buildUpdate(Context context)
		{
			Log.d(TAG, "++ buildUpdate (thread)");  
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.battwidget);					

			Double porcentaje = ((double)BatteryInfoData.getInstance().level/(double)BatteryInfoData.getInstance().scale)*100.0;			
			views.setTextColor(R.id.nivel, Color.GREEN);
			if (porcentaje<75)
				views.setTextColor(R.id.nivel, Color.parseColor("#99CC00"));			
			if (porcentaje<50)
				views.setTextColor(R.id.nivel, Color.parseColor("#FF6666"));
			if (porcentaje<25)
				views.setTextColor(R.id.nivel, Color.RED);
			
			views.setTextViewText(R.id.nivel, porcentaje.intValue()+"%");
			views.setTextViewText(R.id.temp, ((double)BatteryInfoData.getInstance().temp/10.0)+"ºC "+BatteryInfoData.getInstance().voltage+"mV");
			
			if (BatteryInfoData.getInstance().isCargando) {
				views.setTextViewText(R.id.estado, context.getString(R.string.batt_charging));
				views.setTextColor(R.id.tiempo, Color.GREEN);
			} else {
				views.setTextViewText(R.id.estado, context.getString(R.string.batt_remaining));				
				views.setTextColor(R.id.tiempo, Color.YELLOW);
			}
			views.setTextViewText(R.id.tiempo, BatteryInfoData.getInstance().getRemainingTime(BatteryInfoData.getInstance().level, BatteryInfoData.getInstance().scale, BatteryInfoData.getInstance().isCargando));
			views.setTextViewText(R.id.salud, BatteryInfoData.getInstance().getHealthText(context,BatteryInfoData.getInstance().health));
			
			Log.d(TAG, "-- buildUpdate (thread)");  
			return views;
		}		

	}
	
}
