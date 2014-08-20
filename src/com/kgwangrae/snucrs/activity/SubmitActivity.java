package com.kgwangrae.snucrs.activity;

import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Toast;

import com.kgwangrae.snucrs.R;
import com.kgwangrae.snucrs.utils.CourseUtil.CaptchaLoadTask;
import com.kgwangrae.snucrs.utils.CourseUtil.Course;
import com.kgwangrae.snucrs.utils.CourseUtil.CourseAdapter;
import com.kgwangrae.snucrs.utils.CourseUtil.InterestCourseLoadTask;

public class SubmitActivity extends ActionBarActivity {
	private final static String TAG = "SubmitActivity"; 
		
	private ExpandableListView courseListView = null;
	//private RefreshHandler mHandler = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit);
		setTitle("관심강좌에서 신청하기");
		
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
				}
				courseListView.setAdapter(new CourseAdapter(mContext, result));
				courseListView.setVisibility(View.VISIBLE);
				findViewById(R.id.progressbar_loading_interest_courses).setVisibility(View.GONE);
			}
			@Override
			protected void onFailure(Exception exceptionInstance) {
				if (isFinishing()) return;
				
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
			new CaptchaLoadTask(this) {
				@Override
				protected void onSuccess(Bitmap result) {
					if (isFinishing()) return;
					
					ImageView iv = new ImageView(mContext);
					iv.setImageBitmap(Bitmap.createScaledBitmap(result, 4*result.getWidth(), 4*result.getHeight(), true));
					Toast captchaToast = new Toast(mContext);
					captchaToast.setDuration(Toast.LENGTH_SHORT);
					captchaToast.setView(iv);
					captchaToast.show();
				}
				@Override
				protected void onFailure(Exception exceptionInstance) {
					Toast.makeText(mContext, "보안문자 가져오기 실패!", Toast.LENGTH_SHORT).show();
				}
			}.execute();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
