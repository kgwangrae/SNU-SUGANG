package com.kgwangrae.snucrs.utils;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Basic form of AsyncTask<Void,Void,Boolean> used in this application. 
 * Provides operations to retry the job after an exception occurs.
 * Developers can save a reference to the exception raised while executing 
 * so that it can be used at the outside of this AsyncTask.
 * onPostExecute was replaced by onSuccess and onFailure.
 * @author Gwangrae Kim
 */
public abstract class BaseAsyncTask extends AsyncTask<Void, Void, Boolean> {
	protected Context context = null;
	protected Exception raisedException = null;
	
	protected BaseAsyncTask () {}
	protected BaseAsyncTask (Context context) {
		this.context = context;
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
	 * Retry the task.
	 * This method must not be called on the main thread!
	 * @return Whether this retrial was successful.
	 */
	protected final boolean retry() {
		return doInBackground((Void) null);
	}
	/**
	 * Use this method to use the retrial functionality of this class.
	 * @return This instance of {@link BaseAsyncTask}
	 */
	public final BaseAsyncTask execute () {
		super.execute((Void) null);
		initialTime = System.currentTimeMillis();
		return this;
	}
	
	@Override
	protected final Boolean doInBackground(Void... params) {
		trialCount++;
		return backgroundTask();
	}
	
	protected abstract boolean backgroundTask();
	
	@Override 
	protected final void onPostExecute (Boolean result) {
		if (result) onSuccess();
		else onFailure(raisedException);
	}
	
	/**
	 * This method is called on the UI thread after the job is finished without any error.
	 */
	protected abstract void onSuccess ();
	/**
	 * This method is called on the UI thread after the job is finished with some errors.
	 * However, you should manually enable logging the stack trace if you want to.
	 * @param exceptionInstance If no reference to the exception was saved during execution, this value will be null.
	 */
	protected abstract void onFailure (Exception exceptionInstance);
}