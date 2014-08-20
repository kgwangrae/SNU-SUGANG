package com.kgwangrae.snucrs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;
import android.util.Log;

/**
 * Utils for communicating with SNUCRS server.
 * @author Gwangrae Kim
 */
@SuppressWarnings("serial")
public class CommUtil {
	/**
	 * URL Strings
	 */
	private final static String BASE_URL = "http://sugang.snu.ac.kr/sugang/";
	public final static int INTEREST = 1;
	private final static String INTEREST_URL = "cc/cc210.action";
	public final static int MAIN = 2;
	private final static String MAIN_URL = "co/co010.action";
	public final static int LOGOUT = 3;
	private final static String LOGOUT_URL = "co/co003.action";
	public final static int PRE_SUBMISSION = 4;
	private final static String PRE_SUBMISSION_URL = "ca/ca201.action";
	public final static int SUBMISSION = 5;
	private final static String SUBMISSION_URL = "ca/ca101.action";
	public final static int RESULT = 6;
	private final static String RESULT_URL = "ca/ca110.action";
	public final static int PRE_RESULT = 7;
	private final static String PRE_RESULT_URL = "ca/ca210.action";
	public final static int CAPTCHA = 8;
	private final static String CAPTCHA_URL = "ca/number.action";
	
	/**
	 * @param page One of the pageIDs which is defined as constant in this class. 
	 * @return Actual URL of the page. If ID is invalid, null is returned.
	 */
	public static String getURL (int page) {
		switch (page) {
		case INTEREST : return BASE_URL+INTEREST_URL;
		case MAIN : return BASE_URL+MAIN_URL;
		case LOGOUT : return BASE_URL+LOGOUT_URL;
		case PRE_SUBMISSION : return BASE_URL+PRE_SUBMISSION_URL;
		case SUBMISSION : return BASE_URL+SUBMISSION_URL;
		case RESULT : return BASE_URL+RESULT_URL;
		case PRE_RESULT : return BASE_URL+PRE_RESULT_URL;
		case CAPTCHA : return BASE_URL+CAPTCHA_URL;
		default : return null;
		}
	}
	
	/**
	 * Returns a new {@link HttpURLConnection} object for the given URL
	 * with commonly used headers for communicating with the server.
	 * @param c Context
	 * @param url Get this by using {@link CommUtil}.getURL (int page).
	 * @param referer Get this by using {@link CommUtil}.getURL (int page). This cannot be null.
	 * @return A new {@link HttpURLConnection} object
	 * @throws IOException
	 * @throws MalformedURLException Thrown when url or referer is invalid
	 */
	public static HttpURLConnection getSugangConnection (Context c, String url, String referer) 
																throws MalformedURLException, IOException {
		String jSessionId = PrefUtil.getJSessionId(c);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setReadTimeout(3000); // 1. Limit waiting time
		con.setUseCaches(false); // 2. Disable cache
		con.setDoOutput(true); // 3. Enable writing data such as form data. 
		con.setInstanceFollowRedirects(false);  // To get exact header field, stop redirection.
		con.setRequestProperty("Host","sugang.snu.ac.kr"); // 4. Set Host header
		new URL (referer); 
		con.setRequestProperty("Referer", referer); //5. Test validity of the given referer and set Referer header.
		con.setRequestProperty("User-Agent", // 6. Set User-Agent header as Chrome
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36");
		if (jSessionId != null) {
			con.setRequestProperty("Cookie", jSessionId+"; enter=Y");
			//For every request using JSESSIONID, the life of JSESSIONID is prolonged!
			PrefUtil.renewTimestamp(c);
		}
		else con.setRequestProperty("Cookie", "enter=Y"); //7,8 : Set cookie properties
		con.setRequestMethod("POST");
		return con;
	}
	
	/**
	 * Convenient method for getting Jsoup Document for the page.
	 * This method MUST NOT be called on the UI thread.
	 * @param c
	 * @param url
	 * @param referer
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public static Document getJsoupDoc (Context c, String url, String referer) 
													throws IOException, MalformedURLException {
		HttpURLConnection con = null;
		BufferedReader br = null;
		try {
			con = getSugangConnection(c, url, referer);
			br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			
			StringBuffer strbuf = new StringBuffer();
			for (String tmp = br.readLine() ; tmp != null ; tmp = br.readLine()) {
				strbuf.append(tmp);
			}
			return Jsoup.parse(strbuf.toString());
		}
		finally {
			if (con != null) con.disconnect();
			if (br != null) br.close();
		}
	}
	
	/**
	 * Thrown when sturcture of the SNUCRS web page is changed so that this application can no longer operate.
	 * NOTE : It can be unexpectedly thrown when the web page is incompletely loaded 
	 * (usually due to lossy or slow connection) 
	 */
	public static class PageChangedException extends BaseException {
		public PageChangedException(String TAG) {
			super(TAG,"SNUCRS web page is changed. This app needs to be updated.");
		}
	}
}
