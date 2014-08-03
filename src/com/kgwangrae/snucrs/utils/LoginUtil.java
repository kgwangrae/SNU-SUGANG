package com.kgwangrae.snucrs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.kgwangrae.snucrs.utils.CommUtil.PageChangedException;

/**
 * Things relevant to signing in.
 * @author Gwangrae Kim
 */
@SuppressWarnings("serial")
public class LoginUtil {
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
	
	public static abstract class LoginTask extends AsyncTask<Void, Void, Boolean> {
		private final static String TAG = "LoginTask";
		
		private String jSessionId = null;
		private long timeStamp = 0;
		
		private String studentId = null;
		private String plainPassword = null;
		private String base64Password = null;
		
		//Connection, Writer references that should be closed regardless of exceptions.
		private HttpURLConnection loginCon = null, checkCon = null;
		private OutputStreamWriter writer = null;
		
		//Retrial parameters
		private final static long IO_RETRIAL_INTERVAL = 1000;
		private final static int MAX_TRIAL_COUNT = 50;
		private final static long TIMEOUT = IO_RETRIAL_INTERVAL*MAX_TRIAL_COUNT;
		private long initialTime = 0;
		private int trialCount = 0;
		private Exception raisedException = null;

		private final boolean isRetrialRequired() {
			return (System.currentTimeMillis() - initialTime <= TIMEOUT && trialCount < MAX_TRIAL_COUNT);
		}
		
		public LoginTask (String studentId, String plainPassword) {
			super();
			this.studentId = studentId;
			this.plainPassword = plainPassword;
			this.base64Password = Base64.encodeToString(plainPassword.getBytes(), Base64.DEFAULT);
			//Cut the unexpected character at the end.
			if(base64Password.length()>0) 
				base64Password = base64Password.substring(0,base64Password.length()-1);
		}
		//Note : Final methods can't be overriden just like final constants.
		public final LoginTask execute () {
			super.execute((Void) null);
			initialTime = System.currentTimeMillis();
			return this;
		}
		
		@Override
		protected final Boolean doInBackground(Void... params) {
			trialCount ++;
			try {		
				String loginPageURL = "http://sugang.snu.ac.kr/sugang/j_login";
				loginCon 
					= CommUtil.getSugangConnection(loginPageURL, jSessionId, CommUtil.getURL(CommUtil.MAIN));
				loginCon.setInstanceFollowRedirects(false);  // To get Location header field, stop redirection.
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
					raisedException = new PageChangedException(TAG);
					throw raisedException;
				}
				
				//Verify whether the login process was successful. 
				String locationHeader = headers.get("Location").get(0);
				if (locationHeader == null) {
					Log.e(TAG,"Location Header is null.");
					raisedException = new PageChangedException(TAG);
					throw raisedException;
				}
				else if (locationHeader.contains("fail")) {
					raisedException = new WrongCredentialException(TAG);	
					throw raisedException;
				}
				
				//Get cookies
				List<String> cookies = headers.get("Set-Cookie");
				if (cookies == null) {
					Log.e(TAG,"Set-Cookie Header is null.");
					raisedException = new PageChangedException(TAG);
					throw raisedException;
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
					raisedException = new PageChangedException(TAG);
					throw raisedException;
				}
				
				/**
				 * Verify again with the given JSESSIONID by checking the main page 
				 * now have certain String that indicates the login was successful.
				 */
				checkCon 
					= CommUtil.getSugangConnection(CommUtil.getURL(CommUtil.MAIN), jSessionId, loginPageURL);
				BufferedReader br = new BufferedReader(new InputStreamReader(checkCon.getInputStream()));
				for (String line=br.readLine(); line!=null; line=br.readLine()) {
					if(line.contains("로그인전")) {
						raisedException = new IPChangedException(TAG);
						throw raisedException;
					}
					else if(line.contains("로그인후")) break;
				}
				return true;
			}
			//Catch exceptions, starting from the most common one. 
			catch (IOException e) {
				Log.e(TAG,"Please use reliable connection.",e);
				if (isRetrialRequired()) {
					try { Thread.sleep(IO_RETRIAL_INTERVAL); }
					catch (InterruptedException ie) { Log.e(TAG,ie.getMessage(),ie); }
					return doInBackground((Void) null);
				}
				else return false;
			}
			catch (WrongCredentialException e) {
				//Retrying is meaningless for this case.
				return false;
			}
			catch (IPChangedException e) {
				if (isRetrialRequired()) {
					//No interval is required for this case.
					return doInBackground((Void) null);
				}
				else return false;
			}
			catch (PageChangedException e) {
				//See PageChangedException for detail why retrial is required for this exception.
				if (isRetrialRequired()) {
					try { Thread.sleep(IO_RETRIAL_INTERVAL); }
					catch (InterruptedException ie) { Log.e(TAG,ie.getMessage(),ie); }
					return doInBackground((Void) null);
				}
				else return false;
			}
			catch (Exception e) {
				//Catch unknown errors to avoid the app being killed by the system.
				return false;
			}
			finally {
				if(loginCon!=null) loginCon.disconnect();
				if(checkCon!=null) checkCon.disconnect();
				if(writer!=null) {
					try { writer.close(); }
					catch (Exception e) { e.printStackTrace(); }
				}
			}
		}
		
		@Override 
		protected final void onPostExecute (Boolean result) {
			if (result) {
				Log.i(TAG, "Logged in at " + Long.valueOf(timeStamp).toString());
				onSuccess(jSessionId,timeStamp);
			}
			else {
				Log.e(TAG, "Login failed.",raisedException);
				onFailure(raisedException);
			}
		}
		protected abstract void onSuccess (String jSessionId, long timeStamp);
		protected abstract void onFailure (Exception exceptionInstance);
	}
}
