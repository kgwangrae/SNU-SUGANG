package com.kgwangrae.snucrs.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.kgwangrae.snucrs.utils.LoginUtil.IPChangedException;
import com.kgwangrae.snucrs.utils.CommUtil.PageChangedException;

/**
 * @author Gwangrae Kim
 */
public class CourseUtil {
	/**
	 * Class for representing a course
	 */
	public static class Course {
		//Human-readable attributes for each course
		public static final Integer TYPE = 0;
		public static final Integer OPEN_FROM = 1;
		public static final Integer YEAR = 2;
		public static final Integer CODE = 3; //sbjtCd
		public static final Integer SECTION = 4; //ltNo (Warning : not itNo, ltNo (ListNumber))
		public static final Integer NAME = 5;
		public static final Integer CREDIT = 6;
		public static final Integer TIME = 7;
		public static final Integer FORM = 8;
		public static final Integer VENUE = 9;
		public static final Integer INSTRUCTOR = 10;
		public static final Integer CAPACITY = 11;
		public static final Integer CURR_CAPACITY = 12;
		public static final Integer EXTRAS = 13;
		
		@SuppressLint("UseSparseArrays")
		private HashMap<Integer, String> mCourseData = new HashMap<Integer,String>();
		/**
		 * @param attr One of the constants defined in this class, from 0 to 13.
		 * @param value
		 */
		public void putData (Integer attr, String value) {
			if (value.replaceAll("[ 	\n]+", "").length() != 0)
				mCourseData.put(attr, value);
		}
		/**
		 * @param attr One of the constants defined in this class.
		 */
		public String getData (Integer attr) {
			return mCourseData.get(attr);
		}
		@Override
		public String toString() {
			//May be inefficient, but quite clear.
			String result = getData(NAME)+" ("+getData(CODE)+", "+getData(TYPE)+")";
			if (getData(INSTRUCTOR) != null) {
				result +=  ", "+getData(INSTRUCTOR);
			}
			if (getData(TIME) != null) {
				result += ", "+getData(TIME);
			}
			if (getData(VENUE) != null) {
				result += ", "+getData(VENUE);
			} 
			if (getData(CAPACITY) != null) {
				result += ", 정원 "+getData(CAPACITY);
			}
			return result;
		}
	}
	
	/** The core class for loading list of courses the user is interested.
	 *  An example of unparsed information is like below.
	 *  <td rowspan="1">
	 *  	<input type="checkbox" title="선택" name="check" value="0" /> : The position in the list
	 *  	<input type="hidden" name="openSchyy" value="2014" />
	 *  	<input type="hidden" name="openShtmFg" value="U000200001" />
	 *  	<input type="hidden" name="openDetaShtmFg" value="U000300001" />
	 *  	<input type="hidden" name="sbjtCd" value="051.014" /> : Subject code.
	 *  	<input type="hidden" name="ltNo" value="001" />  : Item Number.
	 *  	: Above information is partly needed for course submission. (Only check = 0, sbjtCd, ItNo is essential.)
	 *  	: Below information is the human-readable information about courses which will be displayed to user.
	 *  </td>
	 *  <td rowspan="1">교양</td>
	 *  <td rowspan="1" class="ta_left">체육교육과</td>
	 *  <td rowspan="1">1학년</td>
	 *  <td rowspan="1">051.014</td>
	 *  <td rowspan="1">001</td>
	 *  <td rowspan="1" class="ta_left"><a href="javascript:fnDetailSubject(0)" class="deco_under">수영 3(자유형)</a></td>
	 *  <td rowspan="1">1-0-2</td>
	 *  <td class="blue_st">월(15:00~16:50)</td>
	 *  <td class="blue_st">실습</td>
	 *  <td class="blue_st"></td> (강의실)
	 *  <td rowspan="1">이병호</td>
	 *  <td rowspan="1">30 (21)</td>
	 *  <td rowspan="1">21</td>
	 *  <td rowspan="1" class="line_no">포스코수영장</td>
	 *  TYPE, OPEN_FROM, YEAR, CODE, SECTION, NAME, CREDIT, 
	 *  TIME, FORM, VENUE, INSTRUCTOR, MAX_CAPACITY, CURR_CAPACITY, EXTRAS (in order)
	 *  is specified in the class {@link Course}
	 */
	public static abstract class InterestCourseLoadTask extends BaseAsyncTask <LinkedList <Course>> {
		private final static String TAG = "InterestCourseLoadTask";
		private LinkedList <Course> mCourses = null;
		
		public InterestCourseLoadTask (Context context) {
			super(context);
			mCourses = new LinkedList<Course> ();
		}
		
		@Override 
		protected LinkedList<Course> backgroundTask() {
			try {				
				Document coursesDoc = CommUtil.getJsoupDoc(context,CommUtil.getURL(CommUtil.INTEREST), 
																				CommUtil.getURL(CommUtil.MAIN));
				Element coursesTable = null;
				try {
					coursesTable = coursesDoc.select("tbody").get(0);
				}
				catch (IndexOutOfBoundsException e) {
					throw new IPChangedException(TAG);
					// Note : LoadInterest cannot handle IPChangedException by itself
					// because the LoginTask(AsyncTask) cannot be executed inside this AsyncTask. 
				}
				Iterator<Element> coursesIter = coursesTable.children().iterator();
				Course currCourse = null;
				while(coursesIter.hasNext()) {
					Element currCourseElem = coursesIter.next();
					Iterator <Element> currCourseIter = currCourseElem.children().iterator();
					if (currCourseElem.select("input").size()!=0) { 
						/**
						 * Some rows only have additional time information of the course in the previous row.
						 * Such rows have no input tag inside.
						 */
						// case 0 : A new course appeared!
						currCourse = new Course();
						int foundAttrsCount = 0;
						//Look for codes which are necessary for submitting a course.
						Iterator <Element> submissionCodesIter = currCourseIter.next().children().iterator();
						while(submissionCodesIter.hasNext()) {
							String rawData = submissionCodesIter.next().toString();
							Pattern submissionCodePattern = Pattern.compile("name.*=.*\"(.+)\".*value.*=.*\"(.+)\"");
							Matcher submissionCodeMatcher = submissionCodePattern.matcher(rawData);
							if (submissionCodeMatcher.find()) {
								String attrName = submissionCodeMatcher.group(1);
								String value = submissionCodeMatcher.group(2);
								if (foundAttrsCount > 2) throw new PageChangedException(TAG);
								else if (attrName.equalsIgnoreCase("sbjtCd")) {
									currCourse.putData(Course.CODE, value);
									foundAttrsCount++;
								}
								else if (attrName.equalsIgnoreCase("ltNo")) {
									currCourse.putData(Course.SECTION, value);
									foundAttrsCount++;
								}
							}
							else throw new PageChangedException(TAG);	
						}
						if (foundAttrsCount != 2) throw new PageChangedException(TAG);
						
						for (int idx = 0; currCourseIter.hasNext(); idx++) {
							String rawData = currCourseIter.next().toString();
							Pattern infoPattern = Pattern.compile("<td .*\">([^<]*).*</td>");
							Matcher infoMatcher = infoPattern.matcher(rawData.toString());
							if (infoMatcher.find()) {
								if (foundAttrsCount > 14) throw new PageChangedException(TAG);
								String data = infoMatcher.group(1);
								/**
								 * The values sbjtCd and ltNo respectively appear two times for each course
								 * so the code below checks parity of those values.
								 */
								String existingData = currCourse.getData(idx);
								if (existingData != null) {
									if (existingData.equalsIgnoreCase(data)) {
										//Two values are the same. Do Nothing.
									}
									else {
										Log.e(TAG, "Given course submission value is unexpected. "
																			+existingData+" and "+data+"!");
										throw new PageChangedException(TAG);
									}
								}
								else {
									currCourse.putData(idx, data);
									foundAttrsCount++;
								}
							}
							else throw new PageChangedException(TAG);
						}
						
						if (foundAttrsCount != 14) throw new PageChangedException(TAG);
						mCourses.add(currCourse);
					}
					else { 
						// case 1 : The currElement (current row) only has some part of timetable of the previous course

						/**
						 * idx starts from 7 because this data starts from TIME.
						 * Order is : TIME(7) -> FORM(8) -> VENUE(9)
						 */
						for (int idx = 7; currCourseIter.hasNext(); idx++) {
							String rawInfo = currCourseIter.next().toString();
							Pattern infoPattern = Pattern.compile("<td .*\">([^<]*).*</td>");
							Matcher infoMatcher = infoPattern.matcher(rawInfo.toString());
							if (infoMatcher.find()) {
								if (idx > 9) throw new PageChangedException(TAG);
								
								String data = infoMatcher.group(1);
								String existingData = currCourse.getData(idx);
								if (existingData == null) {
									currCourse.putData(idx, data);
								}
								else if (existingData.contains(data)) {
									//Same data. Do nothing
								}
								else currCourse.putData(idx, existingData+" & "+data);
							}
							else throw new PageChangedException(TAG);
						}
					}
				}
				return mCourses;
			}
			catch (IOException e) {
				if (isRetrialRequired())
					return retryDelayed(TAG);
				raisedException = e;
			}
			catch (IPChangedException e) {
				if (isRetrialRequired())
					return retry();
				raisedException = e;
			}
			catch (Exception e) {
				raisedException = e;
			}
			Log.e(TAG, "An error occurred while getting the interest course list.", raisedException);
			return null;
		}
	}
	
	public static abstract class CaptchaLoadTask extends BaseAsyncTask<Bitmap> {
		private final static String TAG = "CaptchaLoadTask";
		public CaptchaLoadTask (Context context) {
			super(context);
		}
		@Override 
		protected Bitmap backgroundTask() {
			HttpURLConnection numberCon = null;
			try {
				numberCon = CommUtil.getSugangConnection(context, CommUtil.getURL(CommUtil.CAPTCHA)
																						, CommUtil.getURL(CommUtil.INTEREST));
				return BitmapFactory.decodeStream(numberCon.getInputStream());
			}
			catch (IOException e) {
				if (isRetrialRequired())
					return retryDelayed(TAG);
				raisedException = e;
			}
			catch (Exception e) {
				raisedException = e;
			}
			finally {
				numberCon.disconnect();
			}
			Log.e(TAG, "An error occurred while getting the CAPTCHA.", raisedException);
			return null;
		}
	}
	
	/**
	 * Class for submitting the course and handling the result.
	 * @author Gwangrae Kim
	 */
	@SuppressWarnings("serial")
	public static class CourseSubmitTask extends BaseAsyncTask <Boolean> {
		//Exceptions that may only occur during submission
		public static class WrongCaptchaException extends BaseException {
			public WrongCaptchaException (String TAG, String reason) {
				super(TAG, reason);
			}
		}
		public static class OtherHandleableException extends BaseException {
			public OtherHandleableException (String TAG, String reason) {
				super(TAG,reason);
			}
		}
		public static class NotPeriodException extends BaseException {
			public NotPeriodException (String TAG, String reason) {
				super(TAG,reason);
			}
		}
		public static class NotEligibleException extends BaseException {
			public NotEligibleException (String TAG, String reason) {
				super(TAG,reason);
			}
		}
		public static class AlreadySubmittedException extends BaseException {
			public AlreadySubmittedException (String TAG, String reason) {		
				super(TAG,reason);
			}
		}
		public static class ReEnrollException extends BaseException {
			public ReEnrollException (String TAG, String reason) {		
				super(TAG,reason);
			}
		}
		public static class ReEnrollNotEligibleException extends BaseException {
			public ReEnrollNotEligibleException (String TAG, String reason) {		
				super(TAG,reason);
			}
		}
		
		public CourseSubmitTask (Context context) {
			super(context);
		}
		@Override 
		protected Boolean backgroundTask() {
			return null;
		}
		
		@Override
		protected void onSuccess(Boolean result) {
			
		}
		
		@Override
		protected void onFailure(Exception exceptionInstance) {
		}
	}
	
	public static class CourseAdapter extends BaseExpandableListAdapter {
		@Override
		public int getGroupCount() {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public Object getGroup(int groupPosition) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
