package com.kgwangrae.snucrs.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.kgwangrae.snucrs.R;
import com.kgwangrae.snucrs.utils.CommUtil.PageChangedException;
import com.kgwangrae.snucrs.utils.LoginUtil.LoggedOutException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gwangrae Kim
 */
public class CourseUtil {
	/**
	 * Class for representing a course
   * TODO : too inflexible...
   * 2015-04-30 : SNU added one attribute, then this app crashed...
   * <tr>
   <td rowspan="3"><input type="checkbox" title="선택" name="check" value="0">
   <input type="hidden" name="openSchyy" value="2015">
   <input type="hidden" name="openShtmFg" value="U000200001">
   <input type="hidden" name="openDetaShtmFg" value="U000300002">
   <input type="hidden" name="sbjtCd" value="041.003">
   <input type="hidden" name="ltNo" value="001">
   </td>
   <td rowspan="3">교양</td>
   <td rowspan="3" class="ta_left">인문대학</td>
   <td rowspan="3" class="ta_left">국어국문학과</td> ---> TODO this one is added !
   <td rowspan="3">1학년</td>
   <td rowspan="3">041.003</td>
   <td rowspan="3">001</td>
   <td rowspan="3" class="ta_left"><a href="javascript:fnDetailSubject(0)" class="deco_under">문학과 대중문화</a></td>
   <td rowspan="3">3-3-0</td>
   <td class="blue_st">월(15:00~17:50)</td>
   <td class="blue_st">이론</td>
   <td class="blue_st">001-207</td>
   <td rowspan="3">최정아</td>
   <td rowspan="3">40 (40)</td>
   <td rowspan="3">0</td>
   <td rowspan="3" class="line_no">*&nbsp;(권장과목)</td>
   </tr>
   TODO just let human-readable information displayed in arbitrary order.
	 */
	public static class Course {
		//Human-readable attributes for each course
		public static final Integer TYPE = 0;
		public static final Integer OPEN_FROM = 1;
		public static final Integer YEAR = 2;
		public static final Integer CODE = 3; //sbjtCd
		public static final Integer NUMBER = 4; //ltNo (Warning : not itNo! ltNo (ListNumber))
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
			StringBuffer sb = new StringBuffer();
      Iterator<String> courseIterator = mCourseData.values().iterator();
      while (courseIterator.hasNext()) {
        sb.append(courseIterator.next() + " / ");
      }
			return sb.toString();
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
	 *  TYPE, OPEN_FROM, YEAR, CODE, NUMBER, NAME, CREDIT, 
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

    // TODO avoid this kind of too long method body and regex.
		@Override 
		protected LinkedList<Course> backgroundTask() {
			try {				
				Document coursesDoc = CommUtil.getJsoupDoc(CommUtil.getURL(CommUtil.INTEREST),
																				CommUtil.getURL(CommUtil.MAIN));
				Element coursesTable = null;
				try {
					coursesTable = coursesDoc.select("tbody").get(0);
				}
				catch (IndexOutOfBoundsException e) {
					throw new LoggedOutException(TAG);
					// Note : LoadInterest cannot handle LoggedOutException by itself
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
						//Look for codes which are NECESSARY for submitting a course.
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
									currCourse.putData(Course.NUMBER, value);
									foundAttrsCount++;
								}
							}
							else throw new PageChangedException(TAG);	
						}
						if (foundAttrsCount != 2) throw new PageChangedException(TAG);
            // Strict validation is required only for the codes above.

            // Other human-readable information which is not necessary for submission.
						for (int idx = 0; currCourseIter.hasNext(); idx++) {
							String rawData = currCourseIter.next().toString();
							Pattern infoPattern = Pattern.compile("<td .*\">([^<]*).*</td>");
							Matcher infoMatcher = infoPattern.matcher(rawData.toString());
							if (infoMatcher.find()) {
								String data = infoMatcher.group(1);
								String existingData = currCourse.getData(idx);
								if (existingData != null) {
                  // Do nothing for existing data.
								}
								else {
									currCourse.putData(idx, data);
									foundAttrsCount++;
								}
							}
							else throw new PageChangedException(TAG);
						}

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
								else {
                  //Do nothing
								}
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
			catch (LoggedOutException e) {
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
				numberCon = CommUtil.getSugangConnection(CommUtil.getURL(CommUtil.CAPTCHA)
																						, CommUtil.getURL(CommUtil.INTEREST));
				if (numberCon.getHeaderField("Location") == null) {
					//Normal case. Do nothing
				}
				else if (numberCon.getHeaderField("Location").contains(CommUtil.getURL(CommUtil.LOGOUT))) {
					throw new LoggedOutException(TAG);
				}
				Bitmap result = BitmapFactory.decodeStream(numberCon.getInputStream());
				return result;
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
	public static abstract class CourseUpdateTask extends BaseAsyncTask <Boolean> {
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
		
		private static final String TAG = "CourseUpdateTask";
		public static final int WORK_INPUT = 0;
		public static final int WORK_DELETE = 1;

		private boolean isPreSubmit = false;
		protected Course mCourse = null;
		private String mCaptcha = null;
		private String workType = null;
		
		public CourseUpdateTask (Context context, boolean isPreSubmit, Course course, 
										String captcha, int workType) {
			super(context);
			this.isPreSubmit = isPreSubmit; 
			this.mCourse = course;
			this.mCaptcha = captcha;
			switch (workType) {
			case WORK_INPUT :
				this.workType = "I";
				break;
			case WORK_DELETE:
				this.workType = "D";
				break;
			default:
				//Let this.workType be null.
				break;
			}
		}
		
		
		@Override 
		protected Boolean backgroundTask() {
			HttpURLConnection submissionCon = null;
			try {
				if (isPreSubmit)
					submissionCon = CommUtil.getSugangConnection(CommUtil.getURL(CommUtil.PRE_SUBMISSION),
            CommUtil.getURL(CommUtil.INTEREST));
				else 
					submissionCon = CommUtil.getSugangConnection(CommUtil.getURL(CommUtil.SUBMISSION),
            CommUtil.getURL(CommUtil.INTEREST));
				submissionCon.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				submissionCon.setDoOutput(true);
				
				OutputStreamWriter applyFormData = new OutputStreamWriter(submissionCon.getOutputStream());
				StringBuffer strBuf = new StringBuffer();
				strBuf.append("workType=").append(workType)
						.append("&check=0")
						.append("&sbjtCd=").append(mCourse.getData(Course.CODE))
						.append("&ltNo=").append(mCourse.getData(Course.NUMBER))
						.append("&inputText=").append(mCaptcha);
				applyFormData.write(strBuf.toString());
				applyFormData.close();
				
				boolean isResultFound = false;
				Pattern alertPattern = Pattern.compile("alert[(]\"(.*)\"[)];");
				
				BufferedReader br = new BufferedReader (new InputStreamReader (submissionCon.getInputStream()));
				for(String input = br.readLine();input!=null;input=br.readLine()) {
					Matcher alertMatcher = alertPattern.matcher(input);
					if(alertMatcher.find()) {
						isResultFound = true;
						String result = alertMatcher.group(1);
						result = result.replace("\\n", " ");
						Log.i("The result found!",result);
						/**
						 * Idea of this part is provided by Alchan Kim, SNUCSE.
						 * TODO : needs verification. (재수강 / 이미 들은 과목 / 가득 찬 강좌 신청) 
						*/
						if(result.contains("확인문자")) {
							throw new WrongCaptchaException(TAG, result);
						}
						else if(result.contains("못했습니다")||result.contains("아닙니다")||result.contains("잘못")
								||result.contains("오류")||result.contains("없습니다")||result.contains("실패")
								||result.contains("겹칩니다")||result.contains("수업교시")) {
							//TODO : 강의 시간 겹침
							if (result.contains("시간")||result.contains("기간")) 
								throw new NotPeriodException(TAG, result);
							else if (result.contains("대상자")) 
								throw new NotEligibleException(TAG, result);
							else if(result.contains("이미")&&(!result.contains("가득"))) 
								throw new AlreadySubmittedException(TAG, result);
							else throw new OtherHandleableException(TAG, result);
						}
						else if(result.contains("이미")&&(!result.contains("가득"))) {
							throw new AlreadySubmittedException(TAG, result);
						}
						else if(result.contains("넘었습니다")&&(!result.contains("가득"))) {
							//Weirdly they says "5번 넘게 틀렸습니다. 로그아웃 됩니다."
							//But It's still okay to use the current JSESSIONID!
							throw new WrongCaptchaException(TAG, result);
							//throw new LoggedOutException(TAG, result);
						}
						else if(result.contains("로그 아웃")||result.contains("로그아웃")
              ||result.contains("로그인")||result.contains("중복 로그인")
              ||result.contains("10분")) {
							throw new LoggedOutException(TAG, result);
						}
						//Succeeded after filtering the states above.
					}
				}
				//If any result is not found after loop : consider this as logout event (TODO : examine more)
				if (!isResultFound) {
        //  Log.i("INFO",submissionCon.getHeaderFields().toString());
				// Check whether the session was killed by login from other browser
				//	if (submissionCon.getHeaderField("Location") == null) {
        //    throw new PageChangedException(TAG);
        //  }
				//	else if (submissionCon.getHeaderField("Location").contains(CommUtil.getURL(CommUtil.LOGOUT))) {
						throw new LoggedOutException(TAG,"수강신청 서버에 의해 로그아웃되었습니다.*");
				//	}
        //  else throw new PageChangedException(TAG);
				}
				return true;
				// TODO : weirdly set-cookie appears again sometimes
        // Instead of location header -> maybe sugang.snu.ac.kr updated.
			}
			catch (AlreadySubmittedException e) {
				raisedException = e;
				return false;
			}
			catch (NotEligibleException e) {
				raisedException = e;
				return false;
			}
			catch (NotPeriodException e) {
				raisedException = e;
				return false;
			}
			catch (WrongCaptchaException e) {
				raisedException = e;
				return false;
			}
			catch (OtherHandleableException e) {
				raisedException = e;
				return false;
			}
			catch (LoggedOutException e) {
				if (e.getReason().contains("중복")) {
					// Fail instantly
				}
				else if (isRetrialRequired())
					return retry();
				raisedException = e;
				return false;
			}
		/*	catch (PageChangedException e) {
				raisedException = e;
				Log.e(TAG, "Page structure is changed! This app needs an update!", e);
				return false;
			}*/
			catch (IOException e)	{
				if (isRetrialRequired()) 
					return retryDelayed(TAG);
				Log.e(TAG, "An IO error occurred.", e);
				return false;
			}
			catch (Exception e) {
				raisedException = e;
				Log.e(TAG, "Unknown Exception occurred.", e);
				return false;
			}
			finally {
				if(submissionCon != null) submissionCon.disconnect();
			}
		}
	}
	
	public static class CourseAdapter extends BaseExpandableListAdapter {
		private final static String TAG = "CourseAdapter";
		private LinkedList<Course> mCourses = null;
		private Map<Course, LinkedList<Course>> alternativeCourses = null;
		private Context mContext = null;

		public CourseAdapter (Context context, LinkedList<Course> courses) {
			this.mContext = context;
			this.mCourses = courses;
		}
		public LinkedList<Course> getCourses() {
			return mCourses;
		}
		
		@Override
		public int getGroupCount() {
			return mCourses.size();
		} 
		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return 1;
		}
		@Override
		public Object getGroup(int groupPosition) {
			return mCourses.get(groupPosition);
		}
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return new Object();
		}
		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public View getGroupView(final int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			final Course currCourse = mCourses.get(groupPosition);
			View resultView = null;
			if (convertView != null)
				resultView = convertView;
			else resultView = LayoutInflater.from(mContext).inflate(R.layout.list_courses_parent, null);
			if (groupPosition%4 == 3)
				resultView.setBackgroundColor(0x5022ffaa);
			else if (groupPosition%4 == 2)
				resultView.setBackgroundColor(0x3522ffaa);
			else if (groupPosition%4 == 1)
				resultView.setBackgroundColor(0x2022ffaa);
			else 
				resultView.setBackgroundColor(0x0522ffaa);
			//ConvertView still has its previous color. So you must repaint it!
			
			TextView groupText = (TextView) resultView.findViewById(R.id.textview_main_course);
			TextView upBtn = (TextView) resultView.findViewById(R.id.textview_main_course_up);
			TextView downBtn = (TextView) resultView.findViewById(R.id.textview_main_course_down);
			groupText.setText("** "+Integer.valueOf(groupPosition+1).toString()+"순위 **\n"
									+currCourse.toString());
			
			upBtn.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						v.setBackgroundColor(0x46c5c1ff);
						return true;
					}
					else if (event.getAction() == MotionEvent.ACTION_UP) {
						v.setBackgroundColor(0x00000000);
						if (groupPosition > 0) {
							mCourses.remove(groupPosition);
							mCourses.add(groupPosition-1, currCourse);
							notifyDataSetChanged();
						}
						return true;
					}
					else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
						v.setBackgroundColor(0x00000000);
						return true;
					}
					return false;
				}
			});
			downBtn.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						v.setBackgroundColor(0x46c5c1ff);
						return true;
					}
					else if (event.getAction() == MotionEvent.ACTION_UP) {
						v.setBackgroundColor(0x00000000);
						if (groupPosition < (mCourses.size() - 1)) {
							mCourses.remove(groupPosition);
							mCourses.add(groupPosition+1, currCourse);
							notifyDataSetChanged();
						}
						return true;
					}
					else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
						v.setBackgroundColor(0x00000000);
						return true;
					}
					return false;
				}
			});
			
			upBtn.setOnClickListener(null);
			downBtn.setOnClickListener(null);
			
			return resultView;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			View resultView = convertView;
			if (resultView == null) 
				resultView = LayoutInflater.from(mContext).inflate(R.layout.list_courses_child, null);
			return resultView;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
