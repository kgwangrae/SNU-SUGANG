package com.kgwangrae.snucrs.activity;

import java.io.IOException;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kgwangrae.snucrs.R;
import com.kgwangrae.snucrs.utils.BaseException;
import com.kgwangrae.snucrs.utils.CourseUtil.CaptchaLoadTask;
import com.kgwangrae.snucrs.utils.CourseUtil.Course;
import com.kgwangrae.snucrs.utils.CourseUtil.CourseAdapter;
import com.kgwangrae.snucrs.utils.CourseUtil.CourseUpdateTask;
import com.kgwangrae.snucrs.utils.CourseUtil.InterestCourseLoadTask;
import com.kgwangrae.snucrs.utils.LoginUtil.LoggedOutException;
import com.kgwangrae.snucrs.utils.LoginUtil.LoginTask;
import com.kgwangrae.snucrs.utils.PrefUtil;

public class SubmitActivity extends ActionBarActivity {
	private final static String TAG = "SubmitActivity"; 
		
	private LinkedList <Course> mCourses = null;
	private ExpandableListView courseListView = null;
	
	private String mStudentId = null;
	private String mPassword = null;
	
	private boolean isBusy = true;
	//private RefreshHandler mHandler = null;
	//TODO : maintain login every 5 minutes using the handler above
	
	@Override 
	protected void onSaveInstanceState(Bundle outState) {
		//TODO : save courses List to Pref. 
		outState.putString(PrefUtil.studentIdKey, mStudentId);
		outState.putString(PrefUtil.passwordKey, mPassword);
	}
	
 	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit);
		setTitle("관심강좌에서 신청하기");
		
		Intent callIntent = getIntent();
		mStudentId = callIntent.getStringExtra(PrefUtil.studentIdKey);
		mPassword = callIntent.getStringExtra(PrefUtil.passwordKey);
		if (mStudentId==null || mPassword == null) {
			mStudentId = savedInstanceState.getString(PrefUtil.studentIdKey);
			mPassword = savedInstanceState.getString(PrefUtil.passwordKey);
		}
		if (mStudentId==null || mPassword == null) {
			Toast.makeText(this, "필요한 정보가 없습니다.", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		courseListView = (ExpandableListView) findViewById(R.id.list_courses);
		//Sending message test
		//mHandler = new RefreshHandler(this);
		//Message msg = Message.obtain();
		//msg.what = RefreshHandler.jSessionIdMsg;
		//mHandler.sendMessage(msg);
		
		//Fetch course list
		new InterestCourseLoadTask(this) {
			@Override
			protected void onSuccess(LinkedList<Course> result) {
				if (isFinishing()) return;
				
				if (result.size() == 0) {
					Toast.makeText(mContext, "관심강좌가 없습니다.", Toast.LENGTH_LONG).show();
					finish();
				}
				isBusy = false;
				courseListView.setAdapter(new CourseAdapter(mContext, result));
				courseListView.setVisibility(View.VISIBLE);
				findViewById(R.id.progressbar_loading_interest_courses).setVisibility(View.GONE);
			}
			@Override
			protected void onFailure(Exception exceptionInstance) {
				if (isFinishing()) return;
				isBusy = false;
				
				Toast.makeText(mContext, "관심강좌 가져오기 실패!", Toast.LENGTH_SHORT).show();
			}	
		}.execute();
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
			Toast.makeText(this, "지원 예정인 기능입니다.", Toast.LENGTH_SHORT).show();
			return true;
		}
		else if (id == R.id.action_delete) {
			Toast.makeText(this, "지원 예정인 기능입니다.", Toast.LENGTH_SHORT).show();
			return true;
		}
		else if (id == R.id.action_next) {
			CourseAdapter adapter = (CourseAdapter) (courseListView.getExpandableListAdapter());
			if (adapter == null || isBusy) 
				return super.onOptionsItemSelected(item);
			mCourses = adapter.getCourses();
			loadCaptcha(0);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void loadCaptcha (final int courseIdx) {
		isBusy = true;
		new CaptchaLoadTask(this) {
			@Override
			protected void onSuccess(Bitmap result) {
				if (isFinishing()) return;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				LayoutInflater inflater = LayoutInflater.from(mContext);
				
				View inputCaptchaView = inflater.inflate(R.layout.dialog_captcha_input, null);
				ImageView captchaView = (ImageView) inputCaptchaView.findViewById(R.id.captcha_image_view);
				captchaView.setImageBitmap(
						Bitmap.createScaledBitmap(result, 4*result.getWidth(), 4*result.getHeight(), true));
				final TextView captchaInput = (TextView) inputCaptchaView.findViewById(R.id.captcha_input);
				TextView currentCourseView = (TextView) inputCaptchaView.findViewById(R.id.current_course_textView);
				currentCourseView.setText(currentCourseView.getText()+" "+mCourses.get(0).toString());
				
				builder.setTitle("보안문자를 입력하세요");
				builder.setView(inputCaptchaView);
				builder.setPositiveButton("확인", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onCaptchaInput (captchaInput.getText().toString(), 0);
					}
				});
				builder.show();
			}
			@Override
			protected void onFailure(Exception e) {
				if (isFinishing()) return;
				
				if (e instanceof LoggedOutException) {
					new LoginTask (mContext, mStudentId, mPassword) {
						@Override
						protected void onSuccess(Boolean result) {
							if (isFinishing()) return;
							loadCaptcha(courseIdx);
						}
						
						@Override
						protected void onFailure(Exception exceptionInstance) {
							if (isFinishing()) return;
							Toast.makeText(mContext,"재로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
							//TODO : handle exception flag.
							isBusy = false;
						}
					}.execute();
				}
				else {
					Toast.makeText(mContext, "보안문자 가져오기 실패!", Toast.LENGTH_SHORT).show();
					isBusy = false;
				}
			}
		}.execute();
	}
	
	public void onCaptchaInput (String captcha, final int courseIdx) {
		new CourseUpdateTask(this, false, mCourses.get(courseIdx), captcha, CourseUpdateTask.WORK_INPUT) {
			String courseName = mCourse.getData(Course.NAME);
			@Override
			protected void onSuccess(Boolean result) {
				if (isFinishing()) return;
				
				Toast.makeText(mContext, courseName+" 성공!", Toast.LENGTH_SHORT).show();
				if (courseIdx < (mCourses.size()-1)) {
					loadCaptcha(courseIdx+1);
				}
				else {
					//TODO : show result.
					Toast.makeText(mContext, "수고하셨습니다!", Toast.LENGTH_SHORT).show();
					isBusy = false;
				}
			}
			
			@Override
			protected void onFailure(Exception e) {
				if (isFinishing()) return;
				
				if (e instanceof LoggedOutException) {
					BaseException baseException = (BaseException) e;
					Toast.makeText(mContext, courseName + " 실패 : "+ baseException.getReason()
										+ "\n자동으로 로그인을 다시 시도합니다."
										, Toast.LENGTH_SHORT).show();
				}
				else if (e instanceof BaseException) {
					BaseException baseException = (BaseException) e;
					//TODO : message can be awkward sometimes
					Toast.makeText(mContext, courseName + " 실패 : "+ baseException.getReason()
										+ "\n다시 시도하세요!"
										, Toast.LENGTH_SHORT).show();
				}
				else if (e instanceof IOException) {
					Toast.makeText(mContext, courseName + " 실패 : 인터넷 연결 오류"
										, Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(mContext, courseName + " 실패 : "+ e.getClass().getSimpleName()
							, Toast.LENGTH_SHORT).show();
				}
				
				if (e instanceof NotPeriodException || e instanceof WrongCaptchaException) {
					//Case 1 : needs simple repetition
					loadCaptcha(courseIdx);
				}
				else if (e instanceof AlreadySubmittedException || e instanceof OtherHandleableException) {
					//Case 2 : needs to ahead.
					loadCaptcha(courseIdx+1);
				}
				else if (e instanceof NotEligibleException) {
					//Case 3 : needs to stop.
					isBusy = false;
					return;
				}
				else if (e instanceof LoggedOutException) {
					//Case 4 : needs to sign in again and repeat.
					new LoginTask (mContext, mStudentId, mPassword) {
						//TODO : duplicate code
						@Override
						protected void onSuccess(Boolean result) {
							if (isFinishing()) return;
							loadCaptcha(courseIdx);
						}
						
						@Override
						protected void onFailure(Exception exceptionInstance) {
							if (isFinishing()) return;
							Toast.makeText(mContext,"재로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
							//TODO : handle exception flag.
							isBusy = false;
						}
					}.execute();
				}
				else {
					//Case 5 : unknown error occurred. Just repeat.
					loadCaptcha(courseIdx);
				}
			}
		}.execute();
	};
}
