package com.kgwangrae.snucrs.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Basic form of AsyncTask used in this application. 
 * Provides operations to retry the job after an exception occurs.
 * Developers can save a reference of the raised exception to 'raisedException'
 * so that it can be used at the outside of this AsyncTask.
 * onPostExecute was replaced by onSuccess and onFailure for convenience.
 * NOTE : This class must not be used for long-awaiting tasks because it has a strong reference to the caller mContext
 * , which may disturb garbage collection of that and may cause memory leak.
 * @author Gwangrae Kim
 */
public abstract class BaseAsyncTask <Result> extends AsyncTask<Void, Void, Result> {
	protected Context mContext = null;
	protected Exception raisedException = null;
	
	@SuppressWarnings("unused")
	private BaseAsyncTask () {}
	protected BaseAsyncTask (Context context) {
		super();
		this.mContext = context;
	}
	
	//Parameters needed for retrying this AsyncTask
	protected final static long IO_RETRIAL_INTERVAL = 1000;
	protected final static int MAX_TRIAL_COUNT = 20;
	protected final static long TIMEOUT = IO_RETRIAL_INTERVAL*MAX_TRIAL_COUNT;
	protected long initialTime = 0;
	protected int trialCount = 0;

	protected final boolean isRetrialRequired() {
		return (System.currentTimeMillis() - initialTime <= TIMEOUT && trialCount < MAX_TRIAL_COUNT);
	}
	/**
	 * Retry the task without checking limit params.
	 * This method must not be called on the main thread!
	 * @return Result of this retrial
	 */
	protected final Result retry() {
		return doInBackground((Void) null);
	}
	/**
	 * Retry the task after the predefined delay period without checking limit params.
	 * This method must not be called on the main thread!
	 * @param TAG
	 * @return	Result of this retrial
	 */
	protected final Result retryDelayed(String TAG) {
		try { 
			Thread.sleep(IO_RETRIAL_INTERVAL); 
		}
		catch (InterruptedException ie) { 
			Log.e(TAG,ie.getMessage(),ie); 
		}
		return doInBackground((Void) null);
	}
	/**
	 * Use this method to make use of the retrial functionality of this class.
	 * @return This instance of {@link BaseAsyncTask}
	 */
	public final BaseAsyncTask<Result> execute () {
		super.execute((Void) null);
		initialTime = System.currentTimeMillis();
		return this;
	}
	
	@Override
	protected final Result doInBackground(Void... params) {
		trialCount++;
		return backgroundTask();
	}
	
	/**
	 * @return When the job succeeds then it must return the result object
	 * , otherwise it must return null or false.
	 */
	protected abstract Result backgroundTask();
	
	@Override 
	protected final void onPostExecute (Result result) {
		if (result != null) {
			if (result instanceof Boolean) {
				if ((Boolean) result) onSuccess(result);
				else onFailure(raisedException);
			}
			else onSuccess(result);
		}
		else onFailure(raisedException);
	}
	
	/**
	 * This method is called on the UI thread after the job is finished without any error.
	 */
	protected abstract void onSuccess (Result result);
	/**
	 * This method is called on the UI thread after the job is finished with some errors.
	 * However, you should manually enable logging the stack trace if you want to.
	 * @param exceptionInstance If no reference to the exception was saved during execution, this value will be null.
	 */
	protected abstract void onFailure (Exception exceptionInstance);
}