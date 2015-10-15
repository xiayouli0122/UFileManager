package com.yuri.ufm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhaoyan.common.fragment.LibBaseFragment;
import com.zhaoyan.common.utils.SharedPreferenceUtil;

public class BaseFragment extends LibBaseFragment{
	protected boolean mIsSelectAll = false;
	
	//title
	protected TextView mTitleNameView,mTitleNumView;
	private ViewGroup mViewGroup;
	
	/**
	 * current fragment file size
	 */
	protected int count = 0;
	
	//视图模式
//	protected int mViewType = Extra.VIEW_TYPE_DEFAULT;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//get View Type
		SharedPreferences sp = SharedPreferenceUtil
				.getSharedPreference(getActivity().getApplicationContext());
//		mViewType = sp.getInt(Extra.View_TYPE, Extra.VIEW_TYPE_DEFAULT);
				
	}
	
//	protected void initTitle(View view, int title_resId){
//		mViewGroup = (ViewGroup) view;
//		mTitleNameView = (TextView) view.findViewById(R.id.tv_title_name);
//		mTitleNameView.setText(title_resId);
//		mTitleNumView = (TextView) view.findViewById(R.id.tv_title_num);
//		mTitleNumView.setVisibility(View.VISIBLE);
//	}
	
	/**
	 * Show transport animation.
	 * 
	 * @param startViews The transport item image view.
	 */
	public void showTransportAnimation(ImageView... startViews) {
//		TransportAnimationView transportAnimationView = new TransportAnimationView(
//				mContext);
//		transportAnimationView.startTransportAnimation(mViewGroup,
//				mTitleNameView, startViews);
	}
	
	/**
	 * start activity by class name
	 * @param pClass
	 */
	protected void openActivity(Class<?> pClass){
		openActivity(pClass, null);
	}
	
	/**
	 * start activity by class name & include data
	 * @param pClass
	 * @param bundle
	 */
	protected void openActivity(Class<?> pClass, Bundle bundle){
		Intent intent = new Intent(getActivity(), pClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		if (bundle != null) {
			intent.putExtras(bundle);
		}
		startActivity(intent);
//		getActivity().overridePendingTransition(R.anim.activity_right_in, 0);
	}
	
	public void showHideFile(boolean hide){
        
    }

	/**
	 * when user pressed back key
	 */
	public boolean onBackPressed(){
		return false;
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
	
//	protected boolean isListView(){
//		return Extra.VIEW_TYPE_LIST == mViewType;
//	}

}
