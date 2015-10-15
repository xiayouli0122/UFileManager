package com.yuri.ufm;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yuri.ufm.AppListActivity.MyAdapter.ViewHolder;
import com.yuri.ufm.util.Utils;
import com.zhaoyan.common.utils.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListActivity extends AppCompatActivity implements OnItemClickListener, OnClickListener, OnItemLongClickListener {
	private ListView mListView;
	private PackageManager pm;
	private List<AppInfo> mAppInfos = new ArrayList<AppInfo>();
	
	
	ProgressBar mLoadingBar;
	ImageButton mFabButton;
	
	Button mBackupButton;

	private MyAdapter myAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_package);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.id_toolbar);
		Log.d("toolbar:" + toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(Color.WHITE);

        setSupportActionBar(toolbar);

		mListView = (ListView) findViewById(R.id.package_listview);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);

		mLoadingBar = (ProgressBar) findViewById(R.id.progressBar);
		
		// Fab Button
        mFabButton = (ImageButton) findViewById(R.id.fab_button);
//        mFabButton.setImageDrawable(new IconicsDrawable(this, FontAwesome.Icon.faw_upload).color(Color.WHITE).actionBarSize());
        mFabButton.setOnClickListener(this);
        Utils.configureFab(mFabButton);
        
        mBackupButton = (Button) findViewById(R.id.backup_button);
        mBackupButton.setOnClickListener(this);
		
		pm = getPackageManager();
		
		myAdapter = new MyAdapter();
		
		AppListTask appListTask = new AppListTask();
		appListTask.execute("");
	}


	public class AppListTask extends AsyncTask<String, String, List<AppInfo>> {
	    
	    @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingBar.setVisibility(View.VISIBLE);
        }
	    
		@Override
		protected List<AppInfo> doInBackground(String... params) {
		    
		    //Query the applications
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            
            List<ResolveInfo> ril = pm.queryIntentActivities(mainIntent, 0);
            
            for (ResolveInfo ri : ril) {
                mAppInfos.add(new AppInfo(AppListActivity.this, ri));
            }
            Collections.sort(mAppInfos);
            
            for (AppInfo appInfo : mAppInfos) {
                //load icons before shown. so the list is smoother
                appInfo.getIcon();
                appInfo.getLabel();
            }

			// Done!
			return null;
		}

		@Override
		protected void onPostExecute(List<AppInfo> result) {
			super.onPostExecute(result);
			mListView.setAdapter(myAdapter);
			mLoadingBar.setVisibility(View.GONE);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
//			mHandler.sendEmptyMessage(0);
		}

	}

	/**
	 * Perform alphabetical comparison of application entry objects.
	 */
	public static final Comparator<AppInfo> ALPHA_COMPARATOR = new Comparator<AppInfo>() {
		private final Collator sCollator = Collator.getInstance();

		@Override
		public int compare(AppInfo object1, AppInfo object2) {
			return sCollator.compare(object1.getLabel(), object2.getLabel());
		}
	};

	class MyAdapter extends BaseAdapter {
		LayoutInflater inflater = null;
		
		private SparseBooleanArray mIsSelected = null;
		
		private List<AppInfo> list = new ArrayList<AppInfo>();

		MyAdapter() {
			inflater = LayoutInflater.from(AppListActivity.this);
		}
		
		public void setDataList(List<AppInfo> list){
		    this.list = list;
		    mIsSelected = new SparseBooleanArray(list.size());
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mAppInfos.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		public class ViewHolder {
			ImageView iconView;
			TextView lableView;
			TextView packageNameView;
			CheckBox checkBox;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;
			ViewHolder holder = null;
			if (null == convertView || null == convertView.getTag()) {
				view = inflater.inflate(R.layout.app_package_item, null);
				holder = new ViewHolder();
				holder.iconView = (ImageView) view.findViewById(R.id.app_icon_imageview);
				holder.lableView = (TextView) view.findViewById(R.id.app_label_textview);
				holder.packageNameView = (TextView) view.findViewById(R.id.app_package_textview);
				holder.checkBox = (CheckBox) view.findViewById(R.id.checkbox);
//				Log.d("view.id:" + view.getId() + ",view:" +view);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}

			holder.iconView.setImageDrawable(mAppInfos.get(position).getIcon());
			holder.lableView.setText(mAppInfos.get(position).getLabel());
			holder.packageNameView.setText(mAppInfos.get(position).getPackageName());

			return view;
		}
	}


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder holder = (ViewHolder) view.getTag();
        AppInfo appInfo = mAppInfos.get(position);
        animateActivity(appInfo, holder.iconView);
    }
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
//        mBackupButton.setVisibility(View.VISIBLE);
        return false;
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void animateActivity(AppInfo appInfo, View appIcon) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra("appInfo", appInfo.getComponentName());

        ActivityOptionsCompat transitionActivityOptions = 
                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                        Pair.create((View) mFabButton, "fab"), 
                        Pair.create(appIcon, "appIcon"));
        startActivity(i, transitionActivityOptions.toBundle());
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
        case R.id.fab_button:
            
            break;
        case R.id.backup_button:
            
            break;

        default:
            break;
        }
    }

}
