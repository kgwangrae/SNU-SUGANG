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
	 * @param c 
	 * @return
	 */
	public static boolean isCredentialOld (Context c) {
		return (System.currentTimeMillis() - PrefUtil.getTimeStamp(c) > credentialLifeDuration);
	}
	public static class RefreshHandler extends Handler {
		private final static String TAG = "RefreshHandler";
		public final static int jSessionIdMsg = 0;
		private WeakReference<Activity> mActivity = null;
		public RefreshHandler (Activity activity) {
			mActivity = new WeakReference<Activity>(activity);
		}
		@Override
		public void handleMessage (Message msg) {
			Activity activity = mActivity.get();
			if (msg.what == jSessionIdMsg) {
				Log.i(TAG,"Refresh JSESSIONID request is handled!");
				this.sendMessageDelayed(Message.obtain(msg), 2000);
				//TESTING lifecycle of the looper in the main thread.
				//if(activity!=null) activity.finish();
			}
		}
	}
	
	private static class WrongCredentialException extends Exception {
		private WrongCredentialException (String TAG) {
			Log.e(TAG,"Authentication failed due to wrong credential.");
		}
	}
	public static class IPChangedException extends Exception {
		private IPChangedException (String TAG) {
			Log.e(TAG,"Logged out due to IP address change. Please use stable connection such as Wi-Fi.");
		}
	}
	
	public static abstract class LoginTask extends BaseAsyncTask {
		protected final static String TAG = "LoginTask";
		
		private String jSessionId = null;
		private long timeStamp = 0;
		
		private String studentId = null;
		private String plainPassword = null;
		private String base64Password = null;
		
		//Connection, Writer references that should be closed regardless of exceptions.
		private HttpURLConnection loginCon = null, checkCon = null;
		private OutputStreamWriter writer = null;
		
		public LoginTask (Context c, String studentId, String plainPassword) {
			super(c);
			this.studentId = studentId;
			this.plainPassword = plainPassword;
			this.base64Password = Base64.encodeToString(plainPassword.getBytes(), Base64.DEFAULT);
			//Cut the unexpected character at the end.
			if(base64Password.length()>0) 
				base64Password = base64Password.substring(0,base64Password.length()-1);
		}
		
		@Override
		protected final boolean backgroundTask() {
			try {		
				String loginPageURL = "http://sugang.snu.ac.kr/sugang/j_login";
				loginCon 
					= CommUtil.getSugangConnection(context, loginPageURL, CommUtil.getURL(CommUtil.MAIN));
				//Write Login Data, Note current time.
				timeStamp = System.currentTimeMillis();
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
				PrefUtil.setJSessionId(context, jSessionId, timeStamp);
				
				/**
				 * Verify again with the given JSESSIONID by checking the main page 
				 * now has some certain Strings that indicates the login was successful.
				 */
				checkCon 
					= CommUtil.getSugangConnection(context, CommUtil.getURL(CommUtil.MAIN), loginPageURL);
				BufferedReader br = new BufferedReader(new InputStreamReader(checkCon.getInputStream()));
				for (String line=br.readLine(); line!=null; line=br.readLine()) {
					if(line.contains("로그인전")) {
						throw new IPChangedException(TAG);
					}
					else if(line.contains("로그인후")) break;
				}
				return true;
			}
			//Catch exceptions, starting from the most common one. 
			catch (IOException e) {
				raisedException = e;
				if (isRetrialRequired()) {
					try { Thread.sleep(IO_RETRIAL_INTERVAL); }
					catch (InterruptedException ie) { Log.e(TAG,ie.getMessage(),ie); }
					return retry();
				}
				else return false;
			}
			catch (WrongCredentialException e) {
				raisedException = e;
				//Retrying is meaningless for this case.
				return false;
			}
			catch (IPChangedException e) {
				raisedException = e;
				if (isRetrialRequired()) {
					//No interval is required for this case.
					return retry();
				}
				else return false;
			}
			catch (PageChangedException e) {
				raisedException = e;
				//See PageChangedException for detail why retrial is required for this exception.
				if (isRetrialRequired()) {
					try { Thread.sleep(IO_RETRIAL_INTERVAL); }
					catch (InterruptedException ie) { Log.e(TAG,ie.getMessage(),ie); }
					return retry();
				}
				else return false;
			}
			catch (Exception e) {
				raisedException = e;
				//Catch unknown errors to avoid the app being killed by the system.
				return false;
			}
			finally {
				if(loginCon!=null) loginCon.disconnect();
				if(checkCon!=null) checkCon.disconnect();
				if(writer!=null) {
					try { writer.close(); }
					catch (Exception e) {	e.printStackTrace(); }
				}
				if(raisedException!=null) Log.e(TAG, "An error occurred during signing in.", raisedException);
			}
		}
	}
}
