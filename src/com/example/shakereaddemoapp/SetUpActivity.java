package com.example.shakereaddemoapp;

import android.os.Bundle;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SetUpActivity extends Activity implements android.view.View.OnClickListener{

	NumberPicker nmbSensorLower;
	NumberPicker nmbSensorHigher;
	NumberPicker nmbControlWait;
	RadioGroup rdgReadMode;
	RadioGroup rdgSensorDirection;
	Button btnOK;
	Button btnCancel;
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_up);
		
		screenInit();
		
	}
	
	private void screenInit(){
		nmbSensorLower = (NumberPicker)findViewById(R.id.num_sonsor_lower);
		nmbSensorHigher = (NumberPicker)findViewById(R.id.num_sonsor_higher);
		nmbControlWait = (NumberPicker)findViewById(R.id.num_control_wait);
		rdgReadMode = (RadioGroup)findViewById(R.id.rdg_read_mode);
		rdgSensorDirection = (RadioGroup)findViewById(R.id.rdg_sensor_direction);
		btnOK = (Button)findViewById(R.id.btn_ok);
		btnCancel = (Button)findViewById(R.id.btn_cancel);
		btnOK.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		
		nmbSensorLower.setMinValue(1);
		nmbSensorLower.setMaxValue(9);
		nmbSensorHigher.setMinValue(10);
		nmbSensorHigher.setMaxValue(40);
		nmbControlWait.setMaxValue(10);
		
		dataInit();
		
		nmbSensorLower.setValue(sensorSensitivityLower);
		nmbSensorHigher.setValue(sensorSensitivityHigher);
		nmbControlWait.setValue(controlWait);
		
		if(rapidMode == 0){
			rdgReadMode.check(R.id.rdb_read_mode_single);
		}else{
			rdgReadMode.check(R.id.rdb_read_mode_spit);
		}
		
		if(sensorDirection == 0){
			rdgSensorDirection.check(R.id.rdb_read_sensor_x);
		}else if(sensorDirection == 1){
			rdgSensorDirection.check(R.id.rdb_read_sensor_y);
		}else{
			rdgSensorDirection.check(R.id.rdb_read_sensor_z);
		}
		
		
		
	}
	
	private void dataInit(){

		SharedPreferences preferences = getSharedPreferences(PREF_SETTING, MODE_PRIVATE);
		sensorSensitivityLower = preferences.getInt(SETTING_LOWER, 5);
		sensorSensitivityHigher = preferences.getInt(SETTING_HIGHER, 15);
		controlWait = preferences.getInt(SETTING_WAIT, 3);
		rapidMode = preferences.getInt(SETTING_RAPID, 0);
		sensorDirection = preferences.getInt(SETTING_DIRECTION, 2);
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_ok:
			SharedPreferences preferences = getSharedPreferences(PREF_SETTING, MODE_PRIVATE);
			Editor edit = preferences.edit();
			edit.putInt(SETTING_LOWER, nmbSensorLower.getValue());
			edit.putInt(SETTING_HIGHER, nmbSensorHigher.getValue());
			edit.putInt(SETTING_WAIT, nmbControlWait.getValue());
			switch (rdgReadMode.getCheckedRadioButtonId()) {
			case R.id.rdb_read_mode_single:
				edit.putInt(SETTING_RAPID, 0);
				break;
			case R.id.rdb_read_mode_spit:
				edit.putInt(SETTING_RAPID, 1);
				break;
			default:
				break;
			}
			switch (rdgSensorDirection.getCheckedRadioButtonId()) {
			case R.id.rdb_read_sensor_x:
				edit.putInt(SETTING_DIRECTION, 0);
				break;
			case R.id.rdb_read_sensor_y:
				edit.putInt(SETTING_DIRECTION, 1);
				break;
			case R.id.rdb_read_sensor_z:
				edit.putInt(SETTING_DIRECTION, 2);
				break;
			default:
				break;
			}
			edit.commit();
			finish();
			break;
		case R.id.btn_cancel:
			finish();
			break;
		default:
			break;
		}
	}
}
