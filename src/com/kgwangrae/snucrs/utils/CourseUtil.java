package com.kgwangrae.snucrs.utils;

import java.util.List;

import android.content.Context;
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
	}
	public static class CourseLoadTask extends BaseAsyncTask {
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
