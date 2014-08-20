package com.kgwangrae.snucrs.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.kgwangrae.snucrs.R;
import com.kgwangrae.snucrs.utils.LoginUtil.LoginTask;
import com.kgwangrae.snucrs.utils.PrefUtil;

public class LoginActivity extends ActionBarActivity {

	@SuppressWarnings("unused")
	private final static String TAG = "LoginActivity";
	private String mStudentId = null;
	private String mPassword = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		final EditText studentIdInput = (EditText) findViewById(R.id.input_student_id);
		final EditText passwordInput = (EditText) findViewById(R.id.input_password);
		
		//Try to fetch the login information  
		mStudentId = PrefUtil.getStudentId(this);
		mPassword = PrefUtil.getPassword(this);
		
		if(mStudentId!=null && mPassword!=null) {
			//If the login information exists, automatically attempt login.
			studentIdInput.setText(mStudentId);
			passwordInput.setText(mPassword);
			attemptLogin();
		}
		
		Button signInButton = (Button) findViewById(R.id.button_sign_in);
		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View clickedView) {
				int id = clickedView.getId();
				if (id == R.id.button_sign_in) {
					/**
					 * Get content when this button is clicked
					 */
					//May perform validation later.
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
		
		LoginTask loginTask = new LoginTask (LoginActivity.this,mStudentId, mPassword) {
			@Override
			protected void onSuccess(Boolean result) {
				//IMPORTANT : Avoid doing anything after this activity instance is destroyed
				if (isFinishing()) return;
				
				Toast.makeText(mContext, "성공"
					+Long.valueOf(PrefUtil.getTimeStamp(LoginActivity.this)).toString(), Toast.LENGTH_SHORT).show();
				//Save the login information if the user wants it, otherwise delete it.
				CheckBox rememberCheckBox = (CheckBox) findViewById(R.id.checkbox_remember_credential);
				if (rememberCheckBox.isChecked()) PrefUtil.saveLoginInfo(mContext, mStudentId, mPassword);
				else PrefUtil.deleteLoginInfo(mContext);
				
				startActivity(new Intent(mContext,SubmitActivity.class));
				if (pd.isShowing()) pd.dismiss();
			}
			@Override
			protected void onFailure(Exception e) {
				if (isFinishing()) return;
				
				if (pd.isShowing()) pd.dismiss();
				Toast.makeText(LoginActivity.this, "실패", Toast.LENGTH_SHORT).show();
			}
		};
		loginTask.execute();
	}
}
