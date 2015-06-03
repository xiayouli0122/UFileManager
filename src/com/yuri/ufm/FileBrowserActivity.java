package com.yuri.ufm;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yuri.ufm.SystemBarTintManager.SystemBarConfig;
import com.yuri.ufm.Constants.Extras;
import com.yuri.ufm.common.ZyStorageManager;
import com.zhaoyan.common.utils.Log;
import com.zhaoyan.common.utils.SharedPreferencesManager;

public class FileBrowserActivity extends AppCompatActivity implements OnMenuItemClickListener{
	private FileBrowserFragment mBrowserFragment;
	private LeftMenuFragment mLeftMenuFragment;
	
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mActionBarDrawerToggle;
	
	private ListView mDrawerListView;
	private String[] lvs = {"List Item 01", "List Item 02", "List Item 03", "List Item 04"};
	private ArrayAdapter arrayAdapter;

	private FileHomeAdapter mHomeAdapter = null;
	private List<FileHomeInfo> mHomeInfoList = new ArrayList<FileHomeInfo>();
	public static final int INTERNAL = ZyStorageManager.INTERNAL;
	public static final int SDCARD = ZyStorageManager.SDCARD;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		initSystemBar();
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.id_toolbar);
		toolbar.setTitle(R.string.app_name);
		toolbar.setTitleTextColor(Color.WHITE);
		
		setSupportActionBar(toolbar);
		
		toolbar.setOnMenuItemClickListener(this);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_left);
		mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
		mActionBarDrawerToggle.syncState();
		mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);
		
		mDrawerListView = (ListView) findViewById(R.id.lv_left_menu);
		
		//设置菜单列表
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, lvs);
        mDrawerListView.setAdapter(arrayAdapter);
		
		mBrowserFragment= new FileBrowserFragment();
		mLeftMenuFragment = new LeftMenuFragment();
		
		FragmentManager fm = getFragmentManager();
//		fm.beginTransaction().add(R.id.fragment_main, mBrowserFragment).commit();;
//		fm.beginTransaction().add(R.id.left_menu_container, mLeftMenuFragment);
		
		//隐藏别的Fragment，如果存在的话
//        List<Fragment> fragments = fm.beginTransaction().
		
		fm.beginTransaction().replace(R.id.fragment_main, mBrowserFragment).commit();

		FileHomeInfo homeInfo = null;

		// init
		ZyStorageManager zsm = ZyStorageManager.getInstance(getApplicationContext());
		String[] volumnPaths = zsm.getVolumePaths();
		if (volumnPaths == null) {
			Log.d("no storage");
			//do nothing
		}

		if (volumnPaths.length != 0) {
			String internalPath = volumnPaths[0];
			Log.d("internal path:" + internalPath);
			homeInfo = new FileHomeInfo();
			homeInfo.setStorageId(INTERNAL);
			homeInfo.setRootPath(internalPath);

			homeInfo.setAvailableSize(ZyStorageManager.getAvailableBlockSize(internalPath));
			homeInfo.setTotalSize(ZyStorageManager.getTotalBlockSize(internalPath));

			mHomeInfoList.add(homeInfo);
		}

		if (volumnPaths.length >= 2) {
			//have internal & external
			String externalPath = volumnPaths[1];
			Log.d("internal path:" + volumnPaths[0]);
			Log.d("external path:" + externalPath);
			homeInfo = new FileHomeInfo();
			homeInfo.setStorageId(SDCARD);
			homeInfo.setRootPath(externalPath);

			homeInfo.setAvailableSize(ZyStorageManager.getAvailableBlockSize(externalPath));
			homeInfo.setTotalSize(ZyStorageManager.getTotalBlockSize(externalPath));

			mHomeInfoList.add(homeInfo);
		}

		mHomeAdapter = new FileHomeAdapter(getApplicationContext(), mHomeInfoList);
		mDrawerListView.setAdapter(mHomeAdapter);
		
		String defalutRootPath = mHomeInfoList.get(0).getRootPath();
		Log.d("defaultRootPath:" + defalutRootPath);
		SharedPreferencesManager.put(getApplicationContext(), Extras.KEY_DEFAULT_ROOT_PATH, defalutRootPath);

		mBrowserFragment.goToMain(mHomeInfoList.get(0).getStorageId(), defalutRootPath);
	}
	
	private void initSystemBar() {
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
	        //开启状态栏透明
	        //已经在values-v19中设定，这里就不必要了
//	        setTranslucentStatus(true);
	        //使用SystemBarTintManager设定状态栏颜色
	        SystemBarTintManager tintManager = new SystemBarTintManager(this);
	        tintManager.setStatusBarTintEnabled(true);
	        tintManager.setStatusBarTintResource(R.color.colorPrimaryDark);
	        //同时还需要在layout中设定
	        //android:fitsSystemWindows="true"
	    }
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }
	
	@Override
	public void onBackPressed() {
	    boolean ret = mBrowserFragment.onBackPressed();
	    if (!ret) {
	        moveTaskToBack(false);
        } 
	}
	
	@Override
	protected void onDestroy() {
		if (null != mBrowserFragment) {
			mBrowserFragment = null;
		}
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.main, menu);
	    return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
        case R.id.action_exit:
            FileBrowserActivity.this.finish();
            break;

        default:
            break;
        }
        return false;
    }
}
