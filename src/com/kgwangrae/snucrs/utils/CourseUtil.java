package com.kgwangrae.snucrs.utils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class CourseUtil {
	public static class Course {
		//Human-readable attributes for each course
		public static final Integer TYPE = 0;
		public static final Integer OPEN_FROM = 1;
		public static final Integer YEAR = 2;
		public static final Integer CODE = 3;
		public static final Integer SECTION = 4;
		public static final Integer NAME = 5;
		public static final Integer CREDIT = 6;
		public static final Integer TIME = 7;
		public static final Integer FORM = 8;
		public static final Integer VENUE = 9;
		public static final Integer INSTRUCTOR = 10;
		public static final Integer MAX_CAPACITY = 11;
		public static final Integer CURR_CAPACITY = 12;
		public static final Integer EXTRAS = 13;
		
		//TODO : also get apply codes (if necessary) later
		
		private HashMap<Integer, String> courseData = new HashMap<Integer,String>();
		/**
		 * @param attr One of the constants defined in this class.
		 * @param value
		 */
		public void saveData (Integer attr, String value) {
			courseData.put(attr, value);
		}
		/**
		 * @param attr One of the constants defined in this class.
		 * @param value
		 */
		public String getData (Integer attr, String value) {
			return courseData.get(attr);
		}
	}
	public static class CourseLoadTask extends BaseAsyncTask {
		private final static String TAG = "CourseLoadTask";
		private WeakReference<Context> mContext = null;
		private String result = null;
		
		public CourseLoadTask (Context c) {
			this.mContext = new WeakReference<Context>(c);
			this.context = null; //this old context value will be deprecated soon
		}
		
		@Override 
		protected boolean backgroundTask() {
			try {
				Context c = mContext.get();
				if (c==null) 
					throw new NullPointerException("Current context is null. Exiting..");
				
				Document coursesDoc = CommUtil.getJsoupDoc(c,CommUtil.getURL(CommUtil.INTEREST), 
																				CommUtil.getURL(CommUtil.MAIN));
				Elements coursesTable = coursesDoc.select("div.gray_top");
				result = coursesTable.toString();
				return true;
			}
			catch (IOException e) {
				raisedException = e;
			}
			catch (Exception e) {
				raisedException = e;
			}
			finally {
				
			}
			return false;
		}
		
		@Override
		protected void onSuccess() {
			Log.i(TAG,result);
		}
		
		@Override
		protected void onFailure(Exception exceptionInstance) {
			Log.e(TAG,"Loading course list have failed.",exceptionInstance);
		}
	}
	public static class CaptchaLoadTask extends BaseAsyncTask {
		@Override 
		protected boolean backgroundTask() {
			return false;
		}
		
		@Override
		protected void onSuccess() {
		}
		
		@Override
		protected void onFailure(Exception exceptionInstance) {
		}
	}
	public static class CourseSubmitTask extends BaseAsyncTask {
		@Override 
		protected boolean backgroundTask() {
			return false;
		}
		
		@Override
		protected void onSuccess() {
		}
		
		@Override
		protected void onFailure(Exception exceptionInstance) {
		}
	}
	public static class CourseAdapter extends ArrayAdapter<Course> {
		public CourseAdapter(Context context, int layoutToInflate, List<Course> courses) {
			super(context, layoutToInflate, courses);
		}
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			return null;
		}
	}
}
