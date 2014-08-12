package com.kgwangrae.snucrs.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.kgwangrae.snucrs.R;

public class SubmitActivity extends ActionBarActivity implements Handler.Callback {
	private final static String TAG = "SubmitActivity"; 
	private Handler mHandler = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit);
		mHandler = new Handler(this);
		final long tmp = System.currentTimeMillis();
		Runnable refresher = new Runnable() {
			@Override
			public void run() {
				if (System.currentTimeMillis() > tmp ) {
					Log.e(TAG,Long.valueOf(System.currentTimeMillis()).toString());
				}
				mHandler.postDelayed(this, 2000);
			}
		};
		mHandler.post(refresher);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.submit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_add) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean handleMessage(Message msg) {
		
		// TODO Auto-generated method stub
		return false;
	} 
}
