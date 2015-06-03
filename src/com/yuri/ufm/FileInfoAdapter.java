package com.yuri.ufm;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhaoyan.common.actionmenu.ActionMenu;
import com.zhaoyan.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

public class FileInfoAdapter extends BaseAdapter {
	private static final String TAG = "FileInfoAdapter";
	private List<FileInfo> mList = new ArrayList<FileInfo>();
	private LayoutInflater mInflater = null;
	private SparseBooleanArray mIsSelected = null;
	
	private FileIconHelper iconHelper;

	public int mMode = ActionMenu.MODE_NORMAL;
	public int mLastMode = -1;
	
	private boolean mNeedShowMenuBar = true;
	
	private Context mContext;
	
	private MyOnCheckChangeListener mChangeListener = new MyOnCheckChangeListener();
	private MyOnClickListener mClickListener = new MyOnClickListener();
	
	private boolean mDestoryMenu = false;
	
	interface OnFileCheckChangeListener{
	    void onCheckChange();
	    void onCheckBoxClicked(int position);
	}
	
	private OnFileCheckChangeListener mFileCheckChangeListener;
	public void setOnFileCheckChangeListener(OnFileCheckChangeListener listener){
	    mFileCheckChangeListener = listener;
	}
	
	public FileInfoAdapter(Context context, FileIconHelper iconHelper) {
		mInflater = LayoutInflater.from(context);
		mIsSelected = new SparseBooleanArray();
		this.iconHelper = iconHelper;
		mContext = context;
	}

	public FileInfoAdapter(Context context, List<FileInfo> list, FileIconHelper iconHelper) {
		mInflater = LayoutInflater.from(context);
		this.mList = list;
		mIsSelected = new SparseBooleanArray();
		this.iconHelper = iconHelper;
		mContext = context;
	}
	
	public void setDataList(List<FileInfo> list){
		this.mList = list;
		notifyDataSetChanged();
	}

	/**
	 * Select All or not
	 * 
	 * @param isSelected
	 *            true or false
	 */
	public void selectAll(boolean isSelected) {
		int count = this.getCount();
		Log.d(TAG, "selectALl.count=" + count);
		for (int i = 0; i < count; i++) {
			setSelected(i, isSelected);
		}
	}

	/**
	 * set selected or not
	 * 
	 * @param position
	 *            the position that clicked
	 * @param isSelected
	 *            checked or not
	 */
	public void setSelected(int position, boolean isSelected) {
		mIsSelected.put(position, isSelected);
	}

	public void setSelected(int position) {
		mIsSelected.put(position, !isSelected(position));
	}

	public void clearSelected() {
	    Log.d();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				setSelected(i, false);
			}
		}
		mDestoryMenu = true;
		notifyDataSetChanged();
	}

	/**
	 * return current position checked or not
	 * 
	 * @param position
	 *            current position
	 * @return checked or not
	 */
	public boolean isSelected(int position) {
		return mIsSelected.get(position);
	}

	/**
	 * get how many item that has cheked
	 * 
	 * @return checked items num.
	 */
	public int getSelectedItems() {
		int count = 0;
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * get selected items fileinfo list
	 * @return
	 */
	public List<FileInfo> getSelectedFileInfos() {
		List<FileInfo> fileList = new ArrayList<FileInfo>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				fileList.add(mList.get(i));
			}
		}
		return fileList;
	}

	/**
	 * get selected items filepath list
	 * @return
	 */
	public List<String> getSelectedFilePaths() {
		List<String> pathList = new ArrayList<String>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				pathList.add(mList.get(i).filePath);
			}
		}

		return pathList;
	}

	/**
	 * get selected items position list
	 * @return
	 */
	public List<Integer> getSelectedItemsPos() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				list.add(mIsSelected.keyAt(i));
			}
		}
		return list;
	}
	
	/**
	 * check if there is dir selected in selected list
	 * @return
	 */
	public boolean hasDirSelected(){
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				if (mList.get(i).isDir) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * set scroll is idle or not
	 * 
	 * @param flag
	 */
	public void setFlag(boolean flag) {
//		this.mIdleFlag = flag;
	}
	
	public void setNeedShowMenuBar(boolean isNeed){
	    mNeedShowMenuBar = isNeed;
	    Log.d("mNeedShowMenuBar:" + mNeedShowMenuBar);
	}

	/**
	 * This method changes the display mode of adapter between MODE_NORMAL,
	 * MODE_EDIT
	 * 
	 * @param mode
	 *            the mode which will be changed to be.
	 */
	public void changeMode(int mode) {
		mMode = mode;
		Log.d("mLastMode:" + mLastMode + ",mode:" + mode);
		if (ActionMenu.MODE_NORMAL == mode && mLastMode == ActionMenu.MODE_EDIT) {
            setNeedShowMenuBar(false);
        }
		mLastMode = mode;
	}

	/**
	 * This method checks that current mode equals to certain mode, or not.
	 * 
	 * @param mode
	 *            the display mode of adapter
	 * @return true for equal, and false for not equal
	 */
	public boolean isMode(int mode) {
		return mMode == mode;
	}

	public List<FileInfo> getList() {
		return mList;
	}

	public void setList(List<FileInfo> fileList) {
		mList = fileList;
	}

	/**
	 * This method gets index of certain fileInfo(item) in fileInfoList
	 * 
	 * @param fileInfo
	 *            the fileInfo which wants to be located.
	 * @return the index of the item in the listView.
	 */
	public int getPosition(FileInfo fileInfo) {
		Log.d(TAG, "getPosition:" + fileInfo.filePath);
		for (int i = 0; i < mList.size(); i++) {
			if (fileInfo.filePath.equals(mList.get(i).filePath)) {
				return i;
			}
		}
		return mList.indexOf(fileInfo);
	}
	
	public boolean remove(FileInfo fileInfo){
	    boolean ret = false;
	    try {
	        ret = mList.remove(fileInfo);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(e.toString());
        }
	    return ret;
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public FileInfo getItem(int position) {
		if (mList.size() <= 0) {
			return null;
		}
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public class ViewHolder {
		public ImageView iconView;
		CheckBox checkBox;
		TextView nameView;
		TextView countView;
		TextView dateAndSizeView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;

		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.file_item, parent, false);
			holder.iconView = (ImageView) view
					.findViewById(R.id.file_icon_imageview);
			holder.checkBox = (CheckBox) view.findViewById(R.id.checkbox);
			holder.checkBox.setOnCheckedChangeListener(mChangeListener);
			holder.checkBox.setOnClickListener(mClickListener);
//			holder.nameView = (TextView) view
//					.findViewById(R.id.tv_filename);
//			holder.countView = (TextView) view.findViewById(R.id.tv_filecount);
//			holder.dateAndSizeView = (TextView) view
//					.findViewById(R.id.tv_fileinfo);
			view.setTag(holder);
		} else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}

		FileInfo fileInfo = mList.get(position);
		
		holder.checkBox.setTag(new MsgData(position));
		
		//20131128 yuri:use new way to load file icon
		FileListItem.setupFileListItemInfo(mContext, view, fileInfo, iconHelper);
		

//		if (isMode(ActionMenu.MODE_EDIT) || isMode(ActionMenu.MODE_COPY)) {
//			updateListViewBackground(position, view, R.color.holo_blue_light);
//		} else if (isMode(ActionMenu.MODE_CUT)) {
//			updateListViewBackground(position, view, R.color.holo_blue_light_transparent);
//		}else {
//			holder.checkBox.setChecked(false);
//			view.setBackgroundResource(Color.TRANSPARENT);
//		}
		if (isSelected(position)) {
//		    Log.d("setChecked(true)");
		    holder.checkBox.setChecked(true);
        } else {
//            Log.d("setChecked(false)");
            holder.checkBox.setChecked(false);
        }
		
//		if (!isMode(ActionMenu.MODE_NORMAL)) {
//            mFileCheckChangeListener.onCheckChange();
//        }

		return view;
	}
	
	private class MyOnClickListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            MsgData msgData = (MsgData) v.getTag();
            int position = msgData.position;
            Log.d("position" + position);
            mFileCheckChangeListener.onCheckBoxClicked(position);
        }
	    
	}
	
	private class MyOnCheckChangeListener implements OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d();
//            Log.d("mDestoryMenu:" + mDestoryMenu);
//            
//            switch (mMode) {
//            case ActionMenu.MODE_NORMAL:
//                Log.d("mMode.MODE_NORMAL");
//                break;
//            case ActionMenu.MODE_EDIT:
//                Log.d("mMode.MODE_EDIT");
//                break;
//            case ActionMenu.MODE_COPY:
//                Log.d("mMode.MODE_COPY");
//                break;
//            case ActionMenu.MODE_CUT:
//                Log.d("mMode.MODE_CUT");
//                break;
//            default:
//                break;
//            }
//            
////            if (mDestoryMenu) {
////                mDestoryMenu = false;
////                return;
////            }
//            
//            if (isMode(ActionMenu.MODE_NORMAL)) {
//                Log.d("进入编辑模式");
//                MsgData msgData = (MsgData) buttonView.getTag();
//                int position = msgData.position;
//                Log.d("position" + position + ",isChecked:" + isChecked);
//                setSelected(position, isChecked);
//                mFileCheckChangeListener.onCheckChange();
//                return;
//            }
//            
//            MsgData msgData = (MsgData) buttonView.getTag();
//            int position = msgData.position;
//            Log.d("position" + position + ",isChecked:" + isChecked);
//            setSelected(position, isChecked);
//            
//            
//            switch (mLastMode) {
//            case ActionMenu.MODE_NORMAL:
//                Log.d("mLastMode.MODE_NORMAL");
//                break;
//            case ActionMenu.MODE_EDIT:
//                Log.d("mLastMode.MODE_EDIT");
//                break;
//            case ActionMenu.MODE_COPY:
//                Log.d("mLastMode.MODE_COPY");
//                break;
//            case ActionMenu.MODE_CUT:
//                Log.d("mLastMode.MODE_CUT");
//                break;
//            default:
//                Log.d("mLastMode is default");
//                break;
//            }
//            
//            if (mMode == ActionMenu.MODE_EDIT) {
//                if (!mNeedShowMenuBar) {
//                    setNeedShowMenuBar(true);
//                    Log.d("可以显示菜单了");
//                    return;
//                }
//            }
//            
//            mFileCheckChangeListener.onCheckChange();
        }
	    
	}
	
	class MsgData{
	    int position;
	    
	    public MsgData(int position) {
	        this.position = position;
        }
	}

	private void updateListViewBackground(int position, View view, int colorId) {
		if (isSelected(position)) {
			view.setBackgroundResource(colorId);
		} else {
			view.setBackgroundResource(Color.TRANSPARENT);
		}
	}
}
