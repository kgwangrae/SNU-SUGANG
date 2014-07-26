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
 * Things relevant to credentials.
 * @author Gwangrae Kim
 */
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
		private static String TAG = "LoginTask";
		
		private String jSessionId = null;
		private long timeStamp = 0;
		
		private String studentId = null;
		private String plainPassword = null;
		private String base64Password = null;

		/**
		 * Use of this constructor is forbidden as it doesn't force to give necessary information
		 */
		private LoginTask () {
			super();
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
		
		@Override
		protected Boolean doInBackground(Void... params) {
			try {		
				String loginPageURL = "http://sugang.snu.ac.kr/sugang/j_login";
				HttpURLConnection loginCon 
					= CommUtil.getSugangConnection(loginPageURL, jSessionId, CommUtil.getURL(CommUtil.MAIN));
				loginCon.setInstanceFollowRedirects(false);  // To get Location header field, stop redirection.
				//Write Login Data, Note current time.
				timeStamp = System.currentTimeMillis();
				OutputStreamWriter writer = new OutputStreamWriter(loginCon.getOutputStream());
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
					if (cookie.toUpperCase(Locale.ENGLISH).contains("JSESSION")) 
						jSessionId = cookie;
				}
				if (jSessionId == null) {
					Log.e(TAG,"JSESSIONID is not found.");
					throw new PageChangedException(TAG);
				}
				loginCon.disconnect();
				
				/**
				 * Verify again with the given JSESSIONID by checking the main page 
				 * now have certain String that indicates the login was successful.
				 */
				HttpURLConnection checkCon 
					= CommUtil.getSugangConnection(CommUtil.getURL(CommUtil.MAIN), jSessionId, loginPageURL);
				BufferedReader br = new BufferedReader(new InputStreamReader(checkCon.getInputStream()));
				for (String line=br.readLine(); line!=null; line=br.readLine()) {
					if(line.contains("로그인전")) throw new IPChangedException(TAG);
					else if(line.contains("로그인후")) break;
				}
				return true;
			}
			//Catch exceptions, starting from the most common one. 
			catch (IOException e) {
				Log.e(TAG,"Please use reliable connection.",e);
				return false;
			}
			catch (WrongCredentialException e) {
				return false;
			}
			catch (IPChangedException e) {
				return false;
			}
			catch (PageChangedException e) {
				return false;
			}
			catch (Exception e) {
				//Catch unknown errors to avoid the app being killed by the system.
				return false;
			}
		}
		
		/**
		 * super.onPostExecute (Boolean result) must be called when overriding this method.
		 */
		@Override 
		protected void onPostExecute (Boolean result) {
			if (result) {
				Log.i(TAG, "Logged in at " + Long.valueOf(timeStamp).toString());
				onSuccess(jSessionId,timeStamp);
			}
			else onFailure();
		}
		
		protected abstract void onSuccess (String jSessionId, long timeStamp);
		protected abstract void onFailure ();
	}
}
