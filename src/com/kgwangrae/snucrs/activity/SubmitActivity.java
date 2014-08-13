package com.kgwangrae.snucrs.activity;

import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.kgwangrae.snucrs.R;
import com.kgwangrae.snucrs.utils.PrefUtil;
import com.kgwangrae.snucrs.utils.LoginUtil.RefreshHandler;

public class SubmitActivity extends ActionBarActivity {
	private final static String TAG = "SubmitActivity"; 
	private RefreshHandler mHandler = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit);
		
		//Sending message test
		mHandler = new RefreshHandler(this);
		Message msg = Message.obtain();
		msg.what = RefreshHandler.jSessionIdMsg;
		mHandler.sendMessage(msg);
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
}
