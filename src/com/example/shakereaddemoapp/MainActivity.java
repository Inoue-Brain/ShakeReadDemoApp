package com.example.shakereaddemoapp;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKResults.STATUS_CODE;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.barcode.*;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;

public class MainActivity 
		extends Activity 
		implements SensorEventListener,
					EMDKListener, 
					DataListener, 
					StatusListener, 
					ScannerConnectionListener, 
					OnCheckedChangeListener,
					OnClickListener{

	private long time = 0;
	private List<ScannerInfo> deviceList = new ArrayList<ScannerInfo>();
	boolean timeFlg = false;
	
	Scanner scanner;
	
	SensorManager manager;
	float gx, gy, gz, dpi;
	
	//Assign the profile name used in EMDKConfig.xml  
	private String profileName = "DataCaptureProfile";  

	//Declare a variable to store ProfileManager object  
	private ProfileManager mProfileManager = null;  

	//Declare a variable to store EMDKManager object  
	private EMDKManager emdkManager = null;     
	
	BarcodeManager barcodeManager = null;
	
	
	private int dataLength = 0;
	
	private String statusString = "";
	private boolean bContinuousMode = false;
	private boolean isScanning = false;
	
	private long startTime = 0;
	
	EditText edtSample;
	TextView txtScanStatus;
	TextView txtScanData;
	Button btnScanStart;
	
	private final String PREF_SETTING = "setting";
	private final String SETTING_LOWER = "lower";
	private final String SETTING_HIGHER = "higher";
	private final String SETTING_WAIT = "wait";
	private final String SETTING_RAPID = "rapid";
	private final String SETTING_DIRECTION = "direction";
	
	int sensorSensitivityLower = 5;
	int sensorSensitivityHigher = 15;
	int controlWait = 3;
	int rapidMode = 0;
	int sensorDirection = 2;
	
	boolean isReading = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//タイトルバーを使用しない
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		screenInit();

	}
	
	private void dataInit(){
		
		isScanning = false;
		startTime = System.currentTimeMillis();
		time = startTime;
		
		SharedPreferences preferences = getSharedPreferences(PREF_SETTING, MODE_PRIVATE);
		sensorSensitivityLower = preferences.getInt(SETTING_LOWER, 5);
		sensorSensitivityHigher = preferences.getInt(SETTING_HIGHER, 15);
		controlWait = preferences.getInt(SETTING_WAIT, 3);
		rapidMode = preferences.getInt(SETTING_RAPID, 0);
		sensorDirection = preferences.getInt(SETTING_DIRECTION, 2);
		
		EMDKManager.getEMDKManager(getApplicationContext(), this);
		
	}
	
	private void screenInit(){
		edtSample = (EditText)findViewById(R.id.edit_barcode);
		txtScanStatus = (TextView)findViewById(R.id.txt_read_status);
		txtScanData = (TextView)findViewById(R.id.txt_read_data);
		btnScanStart = (Button)findViewById(R.id.btn_scan);
		btnScanStart.setOnClickListener(this);
		
		//EditTextのソフトウエアキーボード設定
		getBaseContext();
		//設定用のインスタンス生成
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //画面遷移直後にソフトウエアキーボードが出ないようにする設定
		inputMethodManager.hideSoftInputFromWindow(edtSample.getWindowToken(), InputMethodManager.SHOW_FORCED);
		//上記設定をEditTextにセット
		getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
        //ソフトウエアキーボード入力を無効化する
		edtSample.setRawInputType(InputType.TYPE_NULL);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		dataInit();
		if (barcodeManager != null) {
			barcodeManager.addConnectionListener(this);
		}
		
		manager = (SensorManager)getSystemService(SENSOR_SERVICE);
	    List<Sensor> sensors =
	            manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
	    if (0 < sensors.size()) {
	        manager.registerListener(
	            this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
	    }
	    
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(barcodeManager != null){
			barcodeManager.removeConnectionListener(this);
			barcodeManager = null;
		}
		if(manager != null){
			manager.unregisterListener(this);
			manager = null;
		}
		if(emdkManager != null){
			emdkManager.release();
			emdkManager = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		menu.clear();
		menu.add(0, 1, 0, "Setting");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == 1){
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, SetUpActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btn_scan){
			long cTime = System.currentTimeMillis();
			if(cTime < startTime + 3000){
				return;
			}
			if(time + (controlWait * 100) > cTime){
				return;
			}
			time = System.currentTimeMillis();
			
			if(barcodeManager != null){
				
				try {
					if(isScanning){
						
						bContinuousMode = false;
						
						scanner.cancelRead();
						scanner.disable();
						scanner = null;
						
						isScanning = false;
					}else{
						
						bContinuousMode = true;
						
						deviceList = barcodeManager.getSupportedDevicesInfo();
						scanner = barcodeManager.getDevice(deviceList.get(1));
						scanner.triggerType = TriggerType.SOFT_ALWAYS;
						if (scanner != null) {
							scanner.addDataListener(this);
							scanner.addStatusListener(this);
						}
						
						scanner.enable();
						scanner.read();
						isScanning = true;
					}
					
				} catch (ScannerException e) {
					e.printStackTrace();
					bContinuousMode = false;
					isScanning = false;
					scanner = null;
				}
			}
		}
	}
	

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		float fval = 0f;
		
		switch (sensorDirection) {
		case 0:
			fval = (float)(gx * 0.9 + event.values[0] * 0.1) - (float)gx;
			gx = (float)(gx * 0.9 + event.values[0] * 0.1);
			break;
		case 1:
			fval = (float)(gy * 0.9 + event.values[1] * 0.1) - (float)gy;
			gy = (float)(gy * 0.9 + event.values[1] * 0.1);
			break;
		case 2:
			fval = (float)(gz * 0.9 + event.values[2] * 0.1) - (float)gz;
			gz = (float)(gz * 0.9 + event.values[2] * 0.1);
			break;
		default:
			break;
		}
		
		long cTime = System.currentTimeMillis();
		if(cTime < startTime + 3000){
			return;
		}
		if(time + (controlWait * 100) > cTime){
			return;
		}
		
		if(fval > ((float)sensorSensitivityLower * 0.1) && 
				fval < (float)sensorSensitivityHigher * 0.1){
			time = System.currentTimeMillis();
			
			if(barcodeManager != null){
				
				try {
					if(isScanning){
						
						bContinuousMode = false;
						
						scanner.cancelRead();
						scanner.disable();
						scanner = null;
						
						isScanning = false;
					}else{
						
						bContinuousMode = true;
						
						deviceList = barcodeManager.getSupportedDevicesInfo();
						scanner = barcodeManager.getDevice(deviceList.get(1));
						scanner.triggerType = TriggerType.SOFT_ALWAYS;
						if (scanner != null) {
							scanner.addDataListener(this);
							scanner.addStatusListener(this);
						}
						
						scanner.enable();
						scanner.read();
						isScanning = true;
					}
					
				} catch (ScannerException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onClosed() {
		if (scanner != null) {
			try {
				scanner.cancelRead();
				
			} catch (ScannerException e) {
			}
		}
	}

	@Override
	public void onOpened(EMDKManager emdkManager) {
		
		this.emdkManager = emdkManager; 
		barcodeManager = (BarcodeManager)emdkManager.getInstance(FEATURE_TYPE.BARCODE);
		
		 
		mProfileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);   
		if(mProfileManager != null)  
		{  
		    try{

		    String[] modifyData = new String[1];  
		    //Call processPrfoile with profile name and SET flag to create the profile. The modifyData can be null.  

		    EMDKResults results = mProfileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, modifyData);  
		     if(results.statusCode == STATUS_CODE.FAILURE)  
		     {  
		         //Failed to set profile  
		     }  
		 }catch (Exception ex){
		    // Handle any exception
		}


		}  
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void onConnectionChange(ScannerInfo arg0, ConnectionState arg1) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void onStatus(StatusData statusData) {
		ScannerStates state = statusData.getState();
		switch(state) {
			case IDLE:
				if (bContinuousMode) { 
					try {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {	
							e.printStackTrace();
						}
						
						scanner.read();	
						if(scanner.isEnabled()){
							statusString = "継続スキャン中";
						}else{
							statusString = "停止";
						}
					} catch (ScannerException e) {
					}
				}else{
					txtScanStatus.setText("アイドル中");
				}
				new AsyncStatusUpdate().execute(statusString);
				new AsyncUiControlUpdate().execute(true);
				break;
			case WAITING:
				statusString = "待機中";
				new AsyncStatusUpdate().execute(statusString);
				new AsyncUiControlUpdate().execute(false);
				break;
			case SCANNING:
				
				if(scanner.isEnabled()){
					statusString = "スキャン中";
				}else{
					statusString = "停止";
				}
				new AsyncStatusUpdate().execute(statusString);
				new AsyncUiControlUpdate().execute(false);
				break;
			case DISABLED:
				statusString = "停止";
				new AsyncStatusUpdate().execute(statusString);
				new AsyncUiControlUpdate().execute(true);
				
				break;
			case ERROR:
				statusString = "エラー";
				new AsyncStatusUpdate().execute(statusString);
				new AsyncUiControlUpdate().execute(true);
				break;
			default:
				break;
		}
	}

	@Override
	public void onData(ScanDataCollection scanDataCollection) {
		if(isReading){
			return;
		}
		if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
			ArrayList <ScanData> scanData = scanDataCollection.getScanData();
	
			String dataString =  scanData.get(0).getData();
			isReading = true;
			new AsyncDataUpdate().execute(dataString);
		}
	}
	
	private class AsyncDataUpdate extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			
			return params[0];
		}
		
		protected void onPostExecute(String result) {
			
			if (result != null) {
				if(dataLength ++ > 100) { //Clear the cache after 100 scans
					edtSample.setText("");
					dataLength = 0;
				}
				
				edtSample.setText(result); 
				txtScanData.append(result + "\n");
				
				if(rapidMode == 1){
					try {
						if(scanner != null){
							scanner.enable();
							scanner.read();
						}
					} catch (ScannerException e) {
						e.printStackTrace();
					}
					
					isScanning = true;
					bContinuousMode = true;
				}else{
					try {
						if(scanner != null){
							scanner.cancelRead();
							scanner.disable();
							scanner = null;
						}
						
					} catch (ScannerException e) {
						e.printStackTrace();
					}
					
					isScanning = false;
					bContinuousMode = false;
				}
			}
			isReading = false;
		}
	}
	
	private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			
			return params[0];
		}
		
		@Override
		protected void onPostExecute(String result) {
			txtScanStatus.setText(result);
		}
	}

	private class AsyncUiControlUpdate extends AsyncTask<Boolean, Void, Boolean> {

		
		@Override
		protected void onPostExecute(Boolean bEnable) {
			
		}

		@Override
		protected Boolean doInBackground(Boolean... arg0) {
			
			return arg0[0];
		}
	}



}
