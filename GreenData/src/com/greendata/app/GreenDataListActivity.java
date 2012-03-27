package com.greendata.app;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.greendata.R;
import com.greendata.data.DataParser;
import com.greendata.data.DataQuery;
import com.greendata.data.DataResults;
import com.greendata.data.GreenRequest;
import com.greendata.data.service.DataService;
import com.greendata.data.worker.DataWorker;
import com.greendata.requestmanager.GreenDataRequestManager;
import com.greendata.requestmanager.GreenDataRequestManager.OnRequestFinishedListener;
import com.openwidget.listview.RefreshAndLoadMoreListView;
import com.openwidget.listview.RefreshAndLoadMoreListView.OnLoadMoreListener;
import com.openwidget.listview.RefreshAndLoadMoreListView.OnRefreshListener;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.greendata.app.GreenDataActivity.OnRequestDataListener;

public class GreenDataListActivity<E extends Parcelable> extends
		GreenDataActivity implements OnRequestDataListener {
	private static final int DEFAULT_THRESHOLD = 5;
	private RefreshAndLoadMoreListView mListView;
	private ArrayAdapter<E> mAdapter;

	private int mThreshold = DEFAULT_THRESHOLD;
	private boolean mIsFirstLoad = true;
	private DataParser mParser;
	private DataQuery mQuery;
	private DataWorker mWorker;
	private View mEmptyView;
	private Handler mHandler = new Handler();
	private boolean mFinishedStart = false;

	private Runnable mRequestFocus = new Runnable() {
		public void run() {
			mListView.focusableViewAvailable(mListView);
		}
	};

	/**
	 * This method will be called when an item in the list is selected.
	 * Subclasses should override. Subclasses can call
	 * getListView().getItemAtPosition(position) if they need to access the data
	 * associated with the selected item.
	 * 
	 * @param l
	 *            The ListView where the click happened
	 * @param v
	 *            The view that was clicked within the ListView
	 * @param position
	 *            The position of the view in the list
	 * @param id
	 *            The row id of the item that was clicked
	 */
	protected void onListItemClick(ListView l, View v, int position, long id) {
	}

	/**
	 * Ensures the list view has been created before Activity restores all of
	 * the view states.
	 * 
	 * @see Activity#onRestoreInstanceState(Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		ensureList();
		super.onRestoreInstanceState(state);
	}

	/**
	 * @see Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(mRequestFocus);
		super.onDestroy();
	}

	/**
	 * Updates the screen state (current list and other views) when the content
	 * changes.
	 * 
	 * @see Activity#onContentChanged()
	 */
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mEmptyView = findViewById(android.R.id.empty);
		mListView = (RefreshAndLoadMoreListView) findViewById(R.id.green_list_view);
		if (mListView == null) {
			throw new RuntimeException(
					"Your content must have a ListView whose id attribute is "
							+ "'android.R.id.list'");
		}
		if (mEmptyView != null) {
			mListView.setEmptyView(mEmptyView);
		}
		mListView.setOnItemClickListener(mOnItemClickListener);
		mListView.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				doGetList(new DataQuery(new TreeMap<String, String>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = -5957690356117127223L;

					{
						put("v", "2");
						put("alt", "jsonc");
						put("start-index", "1");
						put("maxResuls", String.valueOf(mThreshold));
					}
				}));
			}
		});

		mListView.setOnLoadMoreListener(new OnLoadMoreListener() {

			@Override
			public void onLoadMore() {
				// final int itemCount = mMediaAdapter.getCount();
				// if (mMediaAdapter.getMaximumItemCount() ==
				// MediaAdapter.UNLIMITED
				// || itemCount < mMediaAdapter.getMaximumItemCount()) {
				// getVideoList(itemCount + 1, mThreshold);
				// } else {
				// mVideoListView.completeLoadMore();
				// }
			}
		});
		if (mFinishedStart) {
			setListAdapter(mAdapter);
		}
		mHandler.post(mRequestFocus);
		mFinishedStart = true;
	}

	public int getThreshold() {
		return mThreshold;
	}

	public void setThreshold(int threshold) {
		mThreshold = threshold;
	}

	public ArrayAdapter getListAdapter() {
		return mAdapter;
	}

	public void setListAdapter(ArrayAdapter adapter) {
		synchronized (this) {
			ensureList();
			mAdapter = adapter;
			mListView.setAdapter(adapter);
		}
	}

	private void ensureList() {
		if (mListView != null) {
			return;
		}
		setContentView(R.layout.green_list_activity);
	}

	public DataParser getParser() {
		return mParser;
	}

	public void setParser(DataParser parser) {
		mParser = parser;
	}

	public void setWorker(DataWorker worker) {
		mWorker = worker;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setOnRequestDataListener(this);
	}

	@Override
	public void onGetData() {
		// TODO Auto-generated method stub
		// doGetList();
		// Show loader progress bar
	}

	@Override
	public void onGetDataCompleted(DataResults results) {
		List<E> items = results.getList();
		final boolean isRefreshing = mListView.getIsRefreshing();
		int index = 0;
		for (E e : items) {
			if (isRefreshing) {
				mAdapter.insert(e, index++);
			} else {
				mAdapter.add(e);
			}
		}
		if (isRefreshing) {
			mListView.completeRefreshing();
		} else {
			mListView.completeLoadMore();
		}
		if (mIsFirstLoad) {
			mIsFirstLoad = false;
		} 
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void getDataComplete() {
		// Remove loader
	}

	protected void doGetList(DataQuery dataQuery) {
		super.doGetData(mWorker, dataQuery);
	}

	protected void doGetList(GreenRequest request) {
		DataQuery query = new DataQuery(
				(TreeMap<String, String>) request.getParams());
		if (mWorker == null) {
			mWorker = new DataWorker(request.getRestUrl(), request.getParser());
		}
		doGetList(query);
	}

	private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			onListItemClick((ListView) parent, v, position, id);
		}
	};
}
