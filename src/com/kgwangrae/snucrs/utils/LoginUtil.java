package com.kgwangrae.snucrs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.kgwangrae.snucrs.utils.CommUtil.PageChangedException;

/**
 * Things relevant to signing in.
 * @author Gwangrae Kim
 */
@SuppressWarnings("serial")
public class LoginUtil {
	public final static long credentialLifeDuration = 590000;
	/**
	 * Returns whether currently saved credential is older than 10 minutes.
	 * @param c Please make sure this mContext is not null.
	 * @return
	 */
	public static boolean isCredentialOld () {
		return (System.currentTimeMillis() - PrefUtil.getTimeStamp() > credentialLifeDuration);
	}
	public static class RefreshHandler extends Handler {
		//TODO
		private final static String TAG = "RefreshHandler";
		public final static int jSessionIdMsg = 0;
		private WeakReference<Activity> mActivity = null;
		public RefreshHandler (Activity activity) {
			mActivity = new WeakReference<Activity>(activity);
		}
		@Override
		public void handleMessage (Message msg) {
			@SuppressWarnings("unused")
			Activity activity = mActivity.get();
			if (msg.what == jSessionIdMsg) {
				Log.i(TAG,"Refresh JSESSIONID request is handled!");
				this.sendMessageDelayed(Message.obtain(msg), 2000);
			}
		}
	}
	
	private static class WrongCredentialException extends BaseException {
		private WrongCredentialException (String TAG) {
			super(TAG,"Authentication failed due to wrong credential.");
		}
	}
	public static class LoggedOutException extends BaseException {
		public LoggedOutException (String TAG) {
			super(TAG,"Logged out due to IP address change or login attempt from other browser.");
		}
		public LoggedOutException (String TAG, String reason) {
			super(TAG,reason);
		}
	}
	
	public static abstract class LoginTask extends BaseAsyncTask <Boolean> {
		protected final static String TAG = "LoginTask";
		
		private String jSessionId = null;
		private long timeStamp = 0;
		
		private String studentId = null;
		private String plainPassword = null;
		private String base64Password = null;
		
		//Connection, Writer references that should be closed regardless of exceptions.
		private HttpURLConnection loginCon = null, checkCon = null;
		private OutputStreamWriter writer = null;
		
		public LoginTask (Context context, String studentId, String plainPassword) {
			super(context);
			this.studentId = studentId;
			this.plainPassword = plainPassword;
			this.base64Password = Base64.encodeToString(plainPassword.getBytes(), Base64.DEFAULT);
			//Cut the unexpected character at the end.
			if(base64Password.length()>0) 
				base64Password = base64Password.substring(0,base64Password.length()-1);
		}
		
		@Override
		protected final Boolean backgroundTask() {
			try {		
				String loginPageURL = "http://sugang.snu.ac.kr/sugang/j_login";
				loginCon 
					= CommUtil.getSugangConnection(loginPageURL, CommUtil.getURL(CommUtil.MAIN));
				//Write Login Data, Note current time.
				timeStamp = System.currentTimeMillis();
				loginCon.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				writer = new OutputStreamWriter(loginCon.getOutputStream());
				String data = "j_username="+URLEncoder.encode(studentId,"utf-8")
						+"&j_password="+URLEncoder.encode(base64Password,"utf-8")
						+"&v_password="+URLEncoder.encode(plainPassword,"utf-8")
						+"&t_password="+URLEncoder.encode("수강신청 비밀번호","utf-8");
				writer.write(data);
				writer.close();
				
				/**
				 * Get headers from the server as a Map object
				 * For example, {"Set-Cookie" : "JSESSIONID=~", "Location" : "http://sugang.snu.ac.kr/~~~"}
				 */
				Map<String,List<String>> headers = loginCon.getHeaderFields();
				if (headers == null) {
					Log.e("TAG","No headers were received from the server.");
					throw new PageChangedException(TAG);
				}
				
				//Verify whether the login process was successful. 
				String locationHeader = headers.get("Location").get(0);
				if (locationHeader == null) {
					Log.e(TAG,"Location Header is null.");
					throw new PageChangedException(TAG);
				}
				else if (locationHeader.contains("fail")) {
					throw new WrongCredentialException(TAG);	
				}
				
				//Get cookies
				List<String> cookies = headers.get("Set-Cookie");
				if (cookies == null) {
					Log.e(TAG,"Set-Cookie Header is null.");
					throw new PageChangedException(TAG);
				}
				for (int i=0; i<cookies.size();i++) {
					String cookie = cookies.get(i);
					//Look for JSESSIONID by doing case-insensitive search for each cookie.
					if (cookie.toUpperCase(Locale.ENGLISH).contains("JSESSION")) {
						jSessionId = cookie;
						break;
					}
				}
				if (jSessionId == null) {
					Log.e(TAG,"JSESSIONID is not found.");
					throw new PageChangedException(TAG);
				}
				//finally succeeded. Save the credential for further use.
				PrefUtil.setJSessionId(jSessionId, timeStamp);
				
				/**
				 * Verify again with the given JSESSIONID by checking the main page 
				 * now has some certain Strings that indicates the login was successful.
				 */
				checkCon 
					= CommUtil.getSugangConnection(CommUtil.getURL(CommUtil.MAIN), loginPageURL);
				BufferedReader br = new BufferedReader(new InputStreamReader(checkCon.getInputStream()));
				for (String line=br.readLine(); line!=null; line=br.readLine()) {
					if(line.contains("로그인전")) {
						throw new LoggedOutException(TAG);
					}
					else if(line.contains("로그인후")) break;
				}
				return true;
			}
			//Catch exceptions, starting from the most common one. 
			catch (IOException e) {
				raisedException = e;
				if (isRetrialRequired()) 
					return retryDelayed(TAG);
			}
			catch (WrongCredentialException e) {
				raisedException = e;
				//Retrying is meaningless for this case.
			}
			catch (LoggedOutException e) {
				raisedException = e;
				if (isRetrialRequired()) {
					//No interval is required for this case.
					return retry();
				}
			}
			catch (PageChangedException e) {
				raisedException = e;
				//See PageChangedException for detail why retrial is required for this exception.
				if (isRetrialRequired()) {
					return retryDelayed(TAG);
				}
			}
			catch (Exception e) {
				raisedException = e;
				//Catch unknown errors to avoid the app being killed by the system.
			}
			finally {
				if(loginCon!=null) loginCon.disconnect();
				if(checkCon!=null) checkCon.disconnect();
				if(writer!=null) {
					try { writer.close(); }
					catch (Exception e) {	e.printStackTrace(); }
				}
			}
			Log.e(TAG, "An error occurred during signing in.", raisedException);
			return null;
		}
	}
}
