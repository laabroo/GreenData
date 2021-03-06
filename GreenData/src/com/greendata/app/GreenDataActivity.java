package com.greendata.app;

import com.greendata.configuration.RequestConfiguration;
import com.greendata.data.DataParser;
import com.greendata.data.DataQuery;
import com.greendata.data.DataResults;
import com.greendata.data.service.DataService;
import com.greendata.data.worker.DataWorker;
import com.greendata.requestmanager.GreenDataRequestManager;
import com.greendata.requestmanager.GreenDataRequestManager.OnRequestFinishedListener;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;

public class GreenDataActivity extends Activity implements
		OnRequestFinishedListener {

	private GreenDataRequestManager mRequestManager;
	private OnRequestDataListener mOnRequestDataListener;
	protected int mRequestId;
	private DataQuery mCurrentQuery;
	private DataResults<? extends Parcelable> mLastResults;
	
	public interface OnRequestDataListener {
		public void onGetData();

		public void onGetDataCompleted(DataResults<? extends Parcelable> results);

		public void getDataComplete();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRequestManager = GreenDataRequestManager.from(this);
	}

	public OnRequestDataListener getOnRequestDataListener() {
		return mOnRequestDataListener;
	}

	public void setOnRequestDataListener(
			OnRequestDataListener onRequestDataListener) {
		mOnRequestDataListener = onRequestDataListener;
	}

	@Override
	public void onRequestFinished(int requestId, int resultCode, Bundle payload) {
		if (requestId == mRequestId) {
			mRequestId = -1;
			mRequestManager.removeOnRequestFinishedListener(this);
			if (resultCode == DataService.ERROR_CODE) {
				if (payload != null) {
					final int errorType = payload.getInt(
							GreenDataRequestManager.RECEIVER_EXTRA_ERROR_TYPE,
							-1);
					if (errorType == GreenDataRequestManager.RECEIVER_EXTRA_VALUE_ERROR_TYPE_DATA) {
						// mErrorDialogTitle =
						// getString(R.string.dialog_error_data_error_title);
						// mErrorDialogMessage =
						// getString(R.string.dialog_error_data_error_message);
						// showDialog(DialogConfig.DIALOG_ERROR);
					} else {
						// showDialog(DialogConfig.DIALOG_CONNEXION_ERROR);
					}
				} else {
					// showDialog(DialogConfig.DIALOG_CONNEXION_ERROR);
				}
			} else {
				mLastResults = (DataResults<? extends Parcelable>) payload.getParcelable("results");
				mOnRequestDataListener
						.onGetDataCompleted(mLastResults);
			}
		}
	}

	public DataResults<? extends Parcelable> getLastResults() {
		return mLastResults;
	}
	
	public void setCurrentQuery(DataQuery query) {
		mCurrentQuery = query;
	}
	
	public DataQuery getCurrentQuery() {
		return mCurrentQuery;
	}
	
	protected void doGetData(DataWorker worker, DataQuery query) {
		mRequestManager.addOnRequestFinishedListener(this);
		if (mOnRequestDataListener != null) {
			mOnRequestDataListener.onGetData();
		}
		final RequestConfiguration configuration = new RequestConfiguration(worker, query);
		mRequestId = mRequestManager.getData(configuration);
	}
}
