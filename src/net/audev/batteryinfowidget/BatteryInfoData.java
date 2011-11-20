package net.audev.batteryinfowidget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Clase receptora de informacion de bateria, en un singleton para poder ser accedida facilmente desde varias clases
 * @author Nacho L.
 *
 */
public class BatteryInfoData extends BroadcastReceiver {
	
	public static final String TAG = "BatteryInfo";
	public static final String FILE_CARGA = "carga.data";
	public static final String FILE_DESCARGA = "descarga.data";
	private static final int MAX_ENTRIES = 1000;
	
	public static int accuracy = 0;
	   
    int scale = -1;
	int levelAnterior = -1;
	long cuandoLevelAnterior = -1;
    int level = -1;
	int voltage = -1;
	int temp = -1;
	int health = -1;
	String tech = "";
	
	LinkedList<Long> tiemposDescarga = new LinkedList<Long>();
	LinkedList<Long> tiemposCarga = new LinkedList<Long>();
	boolean isCargando = false;
	boolean isFirstRun = true;
	
	List<OnBatteryDataChanged> listeners = new LinkedList<OnBatteryDataChanged>();
	
	/**
	 * Constructor Singleton
	 */
	private BatteryInfoData() { 
	}
	
	/**
	 * Holder
	 * @author Nacho L.
	 *
	 */
	private static class BatteryInfoDataHolder {
		public static final BatteryInfoData instance = new BatteryInfoData();
	}
	
	/**
	 * Devuelve la instancia unica
	 * @return
	 */
	public static BatteryInfoData getInstance() {
		return BatteryInfoDataHolder.instance;
	}
	
	/**
	 * Introduce un listener para recibir notificaciones de cuando cambia el estado de la bateria
	 * @param obdc callback
	 */
	public void addBatteryDataChangedListener(OnBatteryDataChanged obdc) {
		listeners.add(obdc);
	}
	
	/**
	 * Elimina un listener que recibia notificaciones cuando cambiaba el estado de la bateria
	 * @param obdc callback
	 */
	public void removeBatteryDataChangedListener(OnBatteryDataChanged obdc) {
		if (!listeners.contains(obdc)) {
			listeners.remove(obdc);
		}	
	}
	
	/**
	 * Evitar que se pueda clonar al ser singleton
	 */
	public Object clone() throws CloneNotSupportedException {
	        throw new CloneNotSupportedException(); 
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {

		if (isFirstRun) {
			// en el primer lanzamiento, intentamos recuperar datos de la cache
			File cacheDir = context.getFilesDir();
			File cargaFile = new File(cacheDir,FILE_CARGA);
			File descargaFile = new File(cacheDir,FILE_DESCARGA);
			if (cargaFile.exists()) {
				try {
					String datos = inputStreamToString(context.openFileInput(FILE_CARGA));
					String[] numeros = datos.split("[,]");
					for (String s: numeros) {
						try {
							Long l = Long.parseLong(s);
							Log.d(TAG,"cargado @ carga = "+l);
							tiemposCarga.add(l);
						} catch (Exception e2) { }
					}
				} catch (Exception ex) { }
			}
			if (descargaFile.exists()) {
				try {
					String datos = inputStreamToString(context.openFileInput(FILE_DESCARGA));
					String[] numeros = datos.split("[,]");
					for (String s: numeros) {
						try {
							Long l = Long.parseLong(s);
							tiemposDescarga.add(l);
							Log.d(TAG,"cargado @ descarga = "+l);
						} catch (Exception e2) { }
					}
				} catch (Exception ex) { }
			} 		        		
		}
		level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		long cuandoLevelActual  = System.currentTimeMillis();
		boolean isCambioLevel = false;
		if (levelAnterior!=-1 && cuandoLevelAnterior!=-1) {
			long tmp = cuandoLevelActual-cuandoLevelAnterior;
			if (levelAnterior>level) { 				// está descargándose
				isCambioLevel = true;

				isCargando = false;	 		        		
				Log.d(TAG,"descargandose = "+tmp);
				tiemposDescarga.add(tmp);

				writeListToFile(context,tiemposDescarga,FILE_DESCARGA);

			} else if (level>levelAnterior) {		// está cargándose
				isCambioLevel = true;

				isCargando = true;
				Log.d(TAG,"cargandose = "+tmp);
				tiemposCarga.add(tmp);
				writeListToFile(context,tiemposCarga,FILE_CARGA);
			}
		}

		scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
		voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
		tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

		for (OnBatteryDataChanged obdc: listeners) {
			obdc.onBatteryDataChanged();
		}

		// guardamoss la información para el próximo cambio (sacar estadísticas más correctas) 		        	
		levelAnterior = level;

		if (isCambioLevel||isFirstRun) {
			cuandoLevelAnterior = cuandoLevelActual;
		}
		if (isFirstRun)
			isFirstRun=false;

	}

	/**
	 * Metodo privado para escribir el listado a disco
	 * @param context 
	 * @param td tiempos de carga o descarga
	 * @param fileDescarga archivo al que escribir
	 */
	private void writeListToFile(Context context, LinkedList<Long> td, String fileDescarga) {

		LinkedList<Long> theList = new LinkedList<Long>(td);
		while (theList.size()>MAX_ENTRIES)
			theList.remove(0);

		String str = "";
		for (Long l: theList) {
			str+=String.valueOf(l)+",";
		}
		str = str.substring(0,str.length()-1);

		try {
			File file = new File(context.getFilesDir(),fileDescarga);
			PrintWriter writer = new PrintWriter(file);
			writer.write(str);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(TAG,"archivo escrito "+fileDescarga);
	}

	
	/**
	 * Obtenemos el texto de salud de la bateria
	 * @param ctx contexto desde el que se llama
	 * @param h valor de salud
	 * @return texto de salud de la bateria
	 */
	public String getHealthText(Context ctx, int h) {
		String res = "";
		switch (h) {
			case BatteryManager.BATTERY_HEALTH_GOOD:
				res = ctx.getString(R.string.batt_status_good);
				break;
			case BatteryManager.BATTERY_HEALTH_DEAD:
				res = ctx.getString(R.string.batt_status_dead);
				break;
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				res = ctx.getString(R.string.batt_status_overvoltage);
				break;
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				res = ctx.getString(R.string.batt_status_overheat);
				break;
			default:
				res = "?";
		}
		return res;
	}
	
	/**
	 * Obtener tiempos interpolados
	 * @param battLevel nivel de bateria actual
	 * @param battScale nivel de bateria total
	 * @param isCharging TRUE si esta cargando, FALSE si no
	 * @return una cadena de texto indicando horas y minutos restantes de bateria
	 */
	public String getRemainingTime(int battLevel, int battScale, boolean isCharging) {
		LinkedList<Long> tiempos;
		double restantes = 0.0;
		Log.d(TAG,"++getRemainingTime");
		if (isCharging) {
			tiempos = tiemposCarga;
			restantes = battScale-battLevel; // lo que falta hasta llegar a 100 (o al max que sea)
		}
		else { 
			tiempos = tiemposDescarga; // lo que falta para llegar a 0
			restantes = battLevel;
		}
		
		if (tiempos.size()==0)
			return "?";
		
		double sumatorio = 0.0;
		
		for (Long l: tiempos) {
			double valor = ((double)l)/(1000.0*60.0);
			sumatorio +=  valor;// pasamos a minutos
		}			
		
		double media = (double)sumatorio / (double)tiempos.size(); // calculamos la media aritmetica - lo que se supone que tarda
		double total = Math.round(media*restantes); // calculamos los minutos totales que tardará
		
		int horas = (int)total/60;
		int minutos = (int)total % 60;

		
		return horas+"h"+minutos+"m";
	}
	
	/*
	 * Mecanismos de lectura de ficheros / streams
	 */
	private String inputStreamToString(InputStream is)
	{
		ByteArrayOutputStream cont = new ByteArrayOutputStream(); 		
		copyStream(is, cont);		
		return new String(cont.toByteArray());
	}

	private void copyStream(InputStream is, OutputStream os)
	{
		final int buffer_size=1024;
		try
		{
			byte[] bytes=new byte[buffer_size];
			for(;;)
			{
				int count=is.read(bytes, 0, buffer_size);
				if(count==-1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch(Exception ex){}
	}
	
}
