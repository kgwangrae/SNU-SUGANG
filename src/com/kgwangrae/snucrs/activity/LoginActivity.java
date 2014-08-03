package com.kgwangrae.snucrs.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kgwangrae.snucrs.R;
import com.kgwangrae.snucrs.utils.LoginUtil.LoginTask;

public class LoginActivity extends ActionBarActivity {

	private static String TAG = "LoginActivity";
	private String mStudentId = null;
	private String mPassword = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		Button signInButton = (Button) findViewById(R.id.button_sign_in);
		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View clickedView) {
				int id = clickedView.getId();
				if (id == R.id.button_sign_in) {
					/**
					 * Get content when this button is clicked
					 */
					EditText studentIdInput = (EditText) findViewById(R.id.input_student_id);
					EditText passwordInput = (EditText) findViewById(R.id.input_password);
					mStudentId = studentIdInput.getText().toString();
					mPassword = passwordInput.getText().toString();
					attemptLogin();
				}
			}
		});
	}
	
	private void attemptLogin() {
		final ProgressDialog pd 
			= ProgressDialog.show(LoginActivity.this, getString(R.string.signing_in), getString(R.string.please_wait));
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		
		LoginTask loginTask = new LoginTask (mStudentId, mPassword) {
			@Override
			protected void onSuccess(String jSessionId, long timeStamp) {
				if (pd.isShowing()) pd.dismiss();
				Toast.makeText(LoginActivity.this, "성공"+Long.valueOf(timeStamp).toString(), Toast.LENGTH_SHORT).show();
			}
			@Override
			protected void onFailure(Exception e) {
				if (pd.isShowing()) pd.dismiss();
				Toast.makeText(LoginActivity.this, "실패", Toast.LENGTH_SHORT).show();
			}
		};
		loginTask.execute();
	}
}
