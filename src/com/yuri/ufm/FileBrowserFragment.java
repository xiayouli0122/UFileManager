package com.yuri.ufm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yuri.ufm.FileInfoAdapter.OnFileCheckChangeListener;
import com.yuri.ufm.FileInfoManager.NavigationRecord;
import com.yuri.ufm.FileOperationHelper.OnOperationListener;
import com.yuri.ufm.Constants.Extras;
import com.yuri.ufm.common.CopyMoveDialog;
import com.yuri.ufm.common.ZyDeleteDialog;
import com.yuri.ufm.common.ZyEditDialog;
import com.yuri.ufm.common.ZyStorageManager;
import com.yuri.ufm.views.SlowHorizontalScrollView;
import com.yuri.ufm.views.ZyPopupMenu;
import com.yuri.ufm.views.ZyPopupMenu.PopupViewClickListener;
import com.zhaoyan.common.actionmenu.ActionMenu;
import com.zhaoyan.common.actionmenu.ActionMenu.ActionMenuItem;
import com.zhaoyan.common.actionmenu.MenuBarInterface;
import com.zhaoyan.common.dialog.ZyDialogBuilder.onZyDialogClickListener;
import com.zhaoyan.common.file.FileDeleteHelper;
import com.zhaoyan.common.file.FileDeleteHelper.OnDeleteListener;
import com.zhaoyan.common.file.FileUtils;
import com.zhaoyan.common.utils.IntentBuilder;
import com.zhaoyan.common.utils.Log;
import com.zhaoyan.common.utils.SharedPreferencesManager;

public class FileBrowserFragment extends BaseFragment implements OnClickListener, OnItemClickListener, OnScrollListener,
		OnItemLongClickListener, OnOperationListener, MenuBarInterface, OnDeleteListener {
	private static final String TAG = "FileBrowserFragment";

	// File path navigation bar
	private SlowHorizontalScrollView mNavigationBar = null;

	private ListView mListView = null;
	private TextView mListViewTip;
	private ProgressBar mLoadingBar;
	private LinearLayout mNavBarLayout;

	//fast to go to home view
	private View mHomeView;

	private TabManager mTabManager;
	private View rootView = null;
	private FileInfo mSelectedFileInfo = null;
	private int mTop = -1;

//	private FileHomeAdapter mHomeAdapter = null;
	private FileInfoManager mFileInfoManager = null;
	
	private FileInfoAdapter mFileInfoAdapter;
	private FileIconHelper mIconHelper;

	// save all files
	private List<FileInfo> mAllLists = new ArrayList<FileInfo>();
	// save folders
	private List<FileInfo> mFolderLists = new ArrayList<FileInfo>();
	// save files
	private List<FileInfo> mFileLists = new ArrayList<FileInfo>();
	
//	private List<FileHomeInfo> mHomeInfoList = new ArrayList<FileHomeInfo>();
	
	//copy or cut file path list
//	private List<FileInfo> mCopyList = new ArrayList<FileInfo>();
	
	//delete item positions
	private List<Integer> mDeletePosList = new ArrayList<Integer>();

	public static final int INTERNAL = ZyStorageManager.INTERNAL;
	public static final int SDCARD = ZyStorageManager.SDCARD;
	private static final int STATUS_FILE = 0;
	private static final int STATUS_HOME = 1;
	private int mStatus = STATUS_HOME;

	private Context mApplicationContext;

	/**
	 * current dir path
	 */
	private String mCurrentPath;

	// context menu
	// save current sdcard type
	private int storge_type = -1;
	// save current root path
	private String mRootPath;

	private Comparator<FileInfo> NAME_COMPARATOR = FileInfo.getNameComparator();
	
	private FileOperationHelper mFileOperationHelper;
	private FileDeleteHelper mDeleteHelper;

	private static final int MSG_UPDATE_UI = 0;
	private static final int MSG_UPDATE_LIST = 2;
	private static final int MSG_UPDATE_HOME = 3;
	private static final int MSG_UPDATE_FILE = 4;
	private static final int MSG_REFRESH = 5;
	private static final int MSG_OPERATION_OVER = 6;
	private static final int MSG_OPERATION_NOTIFY = 7;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
//				updateTitleNum(-1);
				break;
			case MSG_UPDATE_FILE:
				mFileInfoAdapter.notifyDataSetChanged();
				break;
			case MSG_UPDATE_LIST:
				mNotice.showToast(R.string.operator_over);
				List<FileInfo> fileList = mFileInfoAdapter.getList();
				
				List<Integer> poslist = new ArrayList<Integer>();
				Bundle bundle = msg.getData();
				if (null != bundle) {
					poslist = bundle.getIntegerArrayList("position");
//					Log.d(TAG, "poslist.size=" + poslist);
					int removePosition;
					for(int i = 0; i < poslist.size() ; i++){
						//remove from the last item to the first item
						removePosition = poslist.get(poslist.size() - (i + 1));
//						Log.d(TAG, "removePosition:" + removePosition);
						fileList.remove(removePosition);
						mFileInfoAdapter.notifyDataSetChanged();
					}
					updateUI(fileList.size());
				}else {
					Log.e(TAG, "bundle is null");
				}
				break;
			case MSG_UPDATE_HOME:
//				mHomeAdapter.notifyDataSetChanged();
				break;
			case MSG_REFRESH:
				refreshUI();
				break;
			case MSG_OPERATION_OVER:
				destroyMenuBar();
//				browserTo(new File(mCurrentPath));
				doScanFiles(mCurrentPath);
				break;
			case MSG_OPERATION_NOTIFY:
				mNotice.showToast(msg.obj.toString());
				break;
			default:
				break;
			}
		};
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFileOperationHelper = new FileOperationHelper(getActivity().getApplicationContext());
		mFileOperationHelper.setOnOperationListener(this);
		
		mDeleteHelper = new FileDeleteHelper(getActivity());
		mDeleteHelper.setOnDeleteListener(this);
		
		Log.d(TAG, "onCreate.mStatus=" + mStatus);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.file_main, container, false);
		mApplicationContext = getActivity().getApplicationContext();
		mListView = (ListView) rootView.findViewById(R.id.lv_file);
		mListView.setOnItemClickListener(this);
		mListView.setOnScrollListener(this);
		mListView.setOnItemLongClickListener(this);

//		initTitle(rootView.findViewById(R.id.rl_file_browser_main), R.string.all_file);

		mListViewTip = (TextView) rootView.findViewById(R.id.tv_file_listview_tip);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_loading_file);
		mNavBarLayout = (LinearLayout) rootView.findViewById(R.id.navigation_bar);
		mNavigationBar = (SlowHorizontalScrollView) rootView.findViewById(R.id.navigation_bar_view);
		if (mNavigationBar != null) {
			mNavigationBar.setVerticalScrollBarEnabled(false);
			mNavigationBar.setHorizontalScrollBarEnabled(false);
			mTabManager = new TabManager();
		}
		mHomeView = rootView.findViewById(R.id.ll_home);
		mHomeView.setOnClickListener(this);

		mMenuBarView = rootView.findViewById(R.id.bottom);
		
		initMenuBar(rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);


		mFileInfoManager = new FileInfoManager();

		this.registerForContextMenu(mListView);
//		mHomeInfoList.clear();
		
//		FileHomeInfo homeInfo = null;
		
		// init
//		ZyStorageManager zsm = ZyStorageManager.getInstance(mApplicationContext);
//		String[] volumnPaths = zsm.getVolumePaths();
//		if (volumnPaths == null) {
//			Log.e(TAG, "No storage.");
//			//do nothing
//		} 
//		
//		if (volumnPaths.length != 0) {
//			String internalPath = volumnPaths[0];
//			Log.d(TAG, "internal path:" + internalPath);
//			homeInfo = new FileHomeInfo();
//			homeInfo.setStorageId(INTERNAL);
//			homeInfo.setRootPath(internalPath);
//			
//			homeInfo.setAvailableSize(ZyStorageManager.getAvailableBlockSize(internalPath));
//			homeInfo.setTotalSize(ZyStorageManager.getTotalBlockSize(internalPath));
//			
//			mHomeInfoList.add(homeInfo);
//		} 
//		
//		if (volumnPaths.length >= 2) {
//			//have internal & external
//			String externalPath = volumnPaths[1];
//			Log.d(TAG, "internal path:" + volumnPaths[0]);
//			Log.d(TAG, "external path:" + externalPath);
//			homeInfo = new FileHomeInfo();
//			homeInfo.setStorageId(SDCARD);
//			homeInfo.setRootPath(externalPath);
//			
//			homeInfo.setAvailableSize(ZyStorageManager.getAvailableBlockSize(externalPath));
//			homeInfo.setTotalSize(ZyStorageManager.getTotalBlockSize(externalPath));
//			
//			mHomeInfoList.add(homeInfo);
//		}
//
//		mHomeAdapter = new FileHomeAdapter(mApplicationContext, mHomeInfoList);
		mIconHelper = new FileIconHelper(mApplicationContext);
//		mFileInfoAdapter = new FileInfoAdapter(mApplicationContext, mAllLists, mIconHelper);
		mFileInfoAdapter = new FileInfoAdapter(mApplicationContext, mIconHelper);
		mFileInfoAdapter.setOnFileCheckChangeListener(new OnFileCheckChangeListener() {
            @Override
            public void onCheckChange() {
                Log.d();
                if (mFileInfoAdapter.isMode(ActionMenu.MODE_EDIT)) {
                    updateMenuBar();
                    mMenuBarManager.refreshMenus(mActionMenu);  
                } else {
                    mFileInfoAdapter.changeMode(ActionMenu.MODE_EDIT);
                
//                    mActionMenu = new ActionMenu(mApplicationContext);
                    if (mActionMenu == null) {
                        mActionMenu = new ActionMenu(mApplicationContext);
                    }
                    getActionMenuInflater().inflate(R.menu.allfile_menu, mActionMenu);
                    Log.d("startMenuBar");
                    startMenuBar(mMenuBarView);
                }
            }

            @Override
            public void onCheckBoxClicked(int position) {
                // TODO Auto-generated method stub
                Log.d();
                if (mFileInfoAdapter.isMode(ActionMenu.MODE_EDIT)) {
                    mFileInfoAdapter.setSelected(position);
                    
                    updateMenuBar();
                    mMenuBarManager.refreshMenus(mActionMenu);  
                } else if (mFileInfoAdapter.isMode(ActionMenu.MODE_COPY) ||
                        mFileInfoAdapter.isMode(ActionMenu.MODE_CUT)) {
                    mFileInfoAdapter.setSelected(position);
                    
                    mFileOperationHelper.copy(mFileInfoAdapter.getSelectedFileInfos());
                }else {
                    mFileInfoAdapter.changeMode(ActionMenu.MODE_EDIT);
                
//                    mActionMenu = new ActionMenu(mApplicationContext);
                    boolean isSelected = mFileInfoAdapter.isSelected(position);
                    mFileInfoAdapter.setSelected(position, !isSelected);
//                    mFileInfoAdapter.notifyDataSetChanged();
                    
                    if (mActionMenu == null) {
                        mActionMenu = new ActionMenu(mApplicationContext);
                    }
                    getActionMenuInflater().inflate(R.menu.allfile_menu, mActionMenu);
                    Log.d("startMenuBar");
                    startMenuBar(mMenuBarView);
                }
            }
        });
		Log.d();
		Log.d("onCreatEnd");
		
		setAdapter(mAllLists);
////      browserTo(new File(mRootPath));
//
		if (mRootPath == null) {
            mRootPath = SharedPreferencesManager.get(mApplicationContext, Extras.KEY_DEFAULT_ROOT_PATH, Constants.DEFAULT_SDCARD);
        }
		
        doScanFiles(mRootPath);

//		if (mHomeInfoList.size() <= 0) {
//			mNavBarLayout.setVisibility(View.GONE);
//			mListViewTip.setVisibility(View.VISIBLE);
//			mListViewTip.setText(R.string.no_sdcard);
//		} else {
//			goToHome();
//		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_home:
			destroyMenuBar();
//			goToHome();
			break;
		default:
			mIconHelper.stopLoader();
			mTabManager.updateNavigationBar(v.getId(), storge_type);
			break;
		}
	}

	private int restoreSelectedPosition() {
		if (mSelectedFileInfo == null) {
			Log.d(TAG, "restoreSelectedPosition.mSelectedFileInfo is null");
			return -1;
		} else {
			int curSelectedItemPosition = mFileInfoAdapter.getPosition(mSelectedFileInfo);
			Log.d(TAG, "restoreSelectedPosition.curSelectedItemPosition=" + curSelectedItemPosition);
			mSelectedFileInfo = null;
			return curSelectedItemPosition;
		}
	}

	public void goToMain(int storge_type, String rootPath){
	    Log.d();
		this.storge_type = storge_type;
		mRootPath = rootPath;

		mStatus = STATUS_FILE;

//		setAdapter(mAllLists);
//			browserTo(new File(mRootPath));

//		doScanFiles(mRootPath);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//		if (STATUS_HOME == mStatus) {
//			storge_type = mHomeInfoList.get(position).getStorageId();
//			mNavBarLayout.setVisibility(View.VISIBLE);
//			mStatus = STATUS_FILE;
//			mRootPath = mHomeInfoList.get(position).getRootPath();
//			
//			setAdapter(mAllLists);
////			browserTo(new File(mRootPath));
//			
//			doScanFiles(mRootPath);
//		} else {
			if (mFileInfoAdapter.isMode(ActionMenu.MODE_EDIT)) {
				mFileInfoAdapter.setSelected(position);
				mFileInfoAdapter.notifyDataSetChanged();

//				int selectedCount = mFileInfoAdapter.getSelectedItems();
//				updateTitleNum(selectedCount);
				updateMenuBar();
				mMenuBarManager.refreshMenus(mActionMenu);
			} else {
				FileInfo selectedFileInfo = mFileInfoAdapter.getItem(position);
				if (selectedFileInfo.isDir) {
					int top = view.getTop();
					addToNavigationList(mCurrentPath, top, selectedFileInfo);
//					browserTo(new File(selectedFileInfo.filePath));
					doScanFiles(selectedFileInfo.filePath);
				} else {
					// open file
					IntentBuilder.viewFile(getActivity(), selectedFileInfo.filePath);
				}
			}
//		}
	}
	
	private void doScanFiles(File file){
		if (mGetFileTask != null && mGetFileTask.getStatus() == AsyncTask.Status.RUNNING) {
			mGetFileTask.cancel(true);
		} 
		mGetFileTask = new GetFileTask();
		mGetFileTask.execute(file);
	}
	
	private void doScanFiles(String path){
	    Log.d(path);
		File file = new File(path);
		doScanFiles(file);
	}

	private void setAdapter(List<FileInfo> list) {
		updateUI(list.size());
		mFileInfoAdapter.setList(list);
		mListView.setAdapter(mFileInfoAdapter);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, final View view, final int position, long arg3) {
	    mTop = view.getTop();
	    
		if (STATUS_HOME == mStatus) {
			return false;
		}

		if (mFileInfoAdapter.isMode(ActionMenu.MODE_EDIT)) {
			//do nothing
			//doCheckAll();
			return true;
		} else if (mFileInfoAdapter.isMode(ActionMenu.MODE_COPY)
				|| mFileInfoAdapter.isMode(ActionMenu.MODE_CUT)) {
			return true;
		}

//		else {
//			mFileInfoAdapter.changeMode(ActionMenu.MODE_EDIT);
////			updateTitleNum(1);
//		}
//
//		boolean isSelected = mFileInfoAdapter.isSelected(position);
//		mFileInfoAdapter.setSelected(position, !isSelected);
//		mFileInfoAdapter.notifyDataSetChanged();
//
//		if (mActionMenu == null) {
//		    mActionMenu = new ActionMenu(mApplicationContext);
//        }
//		getActionMenuInflater().inflate(R.menu.allfile_menu, mActionMenu);
//
//		startMenuBar(mMenuBarView);
		//如果有注册ContextMenu, 如果你想在onItemLongClick中实现弹出ContextMenu，那么就return false，否则return true.
		return false;
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo adapterContextMenuInfo = (AdapterContextMenuInfo) menuInfo;
		//获取弹出菜单时，用户选择的ListView的位置
		int position = adapterContextMenuInfo.position;
		FileInfo fileInfo = mFileInfoAdapter.getItem(position);
		if (fileInfo.isDir) {
		    getActivity().getMenuInflater().inflate(R.menu.menu_folder, menu);
        } else {
            getActivity().getMenuInflater().inflate(R.menu.menu_file, menu);
        }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
	    int position = menuInfo.position;
	    Log.d("position:" + position);
	    FileInfo fileInfo = mFileInfoAdapter.getItem(position);
	    List<FileInfo> fileInfos = new ArrayList<FileInfo>();
	    fileInfos.add(fileInfo);
	    switch (item.getItemId()) {
        case R.id.menu_open:
            if (fileInfo.isDir) {
                addToNavigationList(mCurrentPath, mTop, fileInfo);
                doScanFiles(fileInfo.filePath);
            } else {
                // open file
                IntentBuilder.viewFile(getActivity(), fileInfo.filePath);
            }
            break;
        case R.id.menu_copy:
            mFileOperationHelper.copy(fileInfos);
            mFileInfoAdapter.setSelected(position);
            mFileInfoAdapter.changeMode(ActionMenu.MODE_COPY);
            mFileInfoAdapter.notifyDataSetChanged();
            startSinglePasteMenu(fileInfos);
            break;
        case R.id.menu_cut:
            mFileOperationHelper.copy(fileInfos);
            
            mFileInfoAdapter.setSelected(position);
            mFileInfoAdapter.changeMode(ActionMenu.MODE_CUT);
            mFileInfoAdapter.notifyDataSetChanged();
            startSinglePasteMenu(fileInfos);
            break;
        case R.id.menu_delete:
            showDeleteSingleDialog(fileInfo);
            break;
        case R.id.menu_rename:
            mFileInfoManager.showRenameDialog(getActivity(), fileInfos);
            break;
        case R.id.menu_info:
            mFileInfoManager.showInfoDialog(getActivity(), fileInfos);
            break;
        case R.id.menu_share:
            File file = new File(fileInfo.filePath);
            
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            startActivity(intent);
            break;

        default:
            break;
        }
	    return super.onContextItemSelected(item);
	}

	public void browserTo(File file) {
		Log.d(TAG, "browserTo.status=" + mStatus);
		if (file.isDirectory()) {
			mCurrentPath = file.getAbsolutePath();

			clearList();

			fillList(file.listFiles());

			// sort
			Collections.sort(mFolderLists, NAME_COMPARATOR);
			Collections.sort(mFileLists, NAME_COMPARATOR);

			mAllLists.addAll(mFolderLists);
			mAllLists.addAll(mFileLists);

			mFileInfoAdapter.setDataList(mAllLists);
//			mFileInfoAdapter.notifyDataSetChanged();
			int seletedItemPosition = restoreSelectedPosition();
			// Log.d(TAG, "seletedItemPosition:" + seletedItemPosition +
			// ",mTop=" + mTop);
			if (seletedItemPosition == -1) {
				mListView.setSelectionAfterHeaderView();
			} else if (seletedItemPosition >= 0 && seletedItemPosition < mFileInfoAdapter.getCount()) {
				if (mTop == -1) {
					mListView.setSelection(seletedItemPosition);
				} else {
					mListView.setSelectionFromTop(seletedItemPosition, mTop);
					mTop = -1;
				}
			}

			mFileInfoAdapter.selectAll(false);
			updateUI(mAllLists.size());
			mTabManager.refreshTab(mCurrentPath, storge_type);
		} else {
			Log.e(TAG, "It is a file");
		}
	}
	
	public void refreshUI(){
		Log.d(TAG, "refreshUI:" + mCurrentPath);
		List<FileInfo> list = new ArrayList<FileInfo>();
		int firstVisbilePos = mListView.getFirstVisiblePosition();
//		Log.d(TAG, "refreshUi.firstVisbilePos="  + firstVisbilePos);
		clearList();

		File file = new File(mCurrentPath);
		fillList(file.listFiles());

		// sort
		Collections.sort(mFolderLists, NAME_COMPARATOR);
		Collections.sort(mFileLists, NAME_COMPARATOR);

		list.addAll(mFolderLists);
		list.addAll(mFileLists);

		mFileInfoAdapter.setDataList(list);
		
		mListView.setSelection(firstVisbilePos);
	}

	private void clearList() {
//		mAllLists.clear();
		mFolderLists.clear();
		mFileLists.clear();
	}

	/** fill current folder's files into list */
	private void fillList(File[] file) {
		for (File currentFile : file) {
			FileInfo fileInfo = null;

			fileInfo = new FileInfo(currentFile.getName());
			fileInfo.fileDate = currentFile.lastModified();
			fileInfo.filePath = currentFile.getAbsolutePath();
			if (currentFile.isDirectory()) {
				fileInfo.isDir = true;
				fileInfo.fileSize = 0;
				fileInfo.type = FileManager.UNKNOW;
				//do not count hidden files
				File[] files = currentFile.listFiles();
				if (null == files) {
					fileInfo.count = 0;
				}else {
					int count = files.length;
					for(File f : files){
						if (f.isHidden()) {
							count --;
						}
					}
					fileInfo.count = count;
				}
				
				if (currentFile.isHidden()) {
					// do nothing
				} else {
					mFolderLists.add(fileInfo);
				}
			} else {
				fileInfo.isDir = false;
				fileInfo.fileSize = currentFile.length();
				fileInfo.type = FileManager.getFileType(mApplicationContext, currentFile);
				if (currentFile.isHidden()) {
					// do nothing
				} else {
					mFileLists.add(fileInfo);
				}
			}
		}
	}

	/**
	 * show delete confrim dialog
	 */
	public void showDeleteDialog(final List<Integer> posList) {
		// get name list
		List<FileInfo> fileList = mFileInfoAdapter.getList();
		
		ZyDeleteDialog deleteDialog = new ZyDeleteDialog(getActivity());
		deleteDialog.setDialogTitle(R.string.delete_file);
		String msg = "";
		if (posList.size() == 1) {
			msg = getString(R.string.delete_file_confirm_msg, fileList.get(posList.get(0)).fileName);
		}else {
			msg = getString(R.string.delete_file_confirm_msg_file, posList.size());
		}
		deleteDialog.setMessage(msg);
		deleteDialog.setPositiveButton(R.string.menu_delete, new onZyDialogClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				mDeletePosList = mFileInfoAdapter.getSelectedItemsPos();
				
				mDeleteHelper.setDeletePathList(mFileInfoAdapter.getSelectedFilePaths());
				mDeleteHelper.doDelete();
				
				dialog.dismiss();
				destroyMenuBar();
			}
		});
		deleteDialog.setNegativeButton(R.string.cancel, null);
		deleteDialog.show();
	}
	
	/**
     * show delete single confrim dialog
     */
    public void showDeleteSingleDialog(final FileInfo fileInfo) {
        ZyDeleteDialog deleteDialog = new ZyDeleteDialog(getActivity());
        deleteDialog.setDialogTitle(R.string.delete_file);
        String msg = getString(R.string.delete_file_confirm_msg, fileInfo.fileName);
        deleteDialog.setMessage(msg);
        deleteDialog.setPositiveButton(R.string.menu_delete, new onZyDialogClickListener() {
            @Override
            public void onClick(Dialog dialog) {
                List<String> deleteList = new ArrayList<String>();
                deleteList.add(fileInfo.filePath);
                
                mDeleteHelper.setDeletePathList(deleteList);
                mDeleteHelper.doDelete();
                
                mFileInfoAdapter.remove(fileInfo);
                mFileInfoAdapter.notifyDataSetChanged();
                
                dialog.dismiss();
            }
        });
        deleteDialog.setNegativeButton(R.string.cancel, null);
        deleteDialog.show();
    }

	public void addToNavigationList(String currentPath, int top, FileInfo selectFile) {
		mFileInfoManager.addToNavigationList(new NavigationRecord(currentPath, top, selectFile));
	}

	/** file path tab manager */
	protected class TabManager {
		private List<String> mTabNameList = new ArrayList<String>();
		protected LinearLayout mTabsHolder = null;
		private String curFilePath = null;
		private Button mBlankTab;

		public TabManager() {
			mTabsHolder = (LinearLayout) rootView.findViewById(R.id.tabs_holder);
			// 添加一个空的button，为了UI更美观
			mBlankTab = new Button(getActivity());
			mBlankTab.setBackgroundResource(R.drawable.fm_blank_tab);
			LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

			mlp.setMargins((int) getResources().getDimension(R.dimen.tab_margin_left), 0,
					(int) getResources().getDimension(R.dimen.tab_margin_right), 0);
			mBlankTab.setLayoutParams(mlp);
			mTabsHolder.addView(mBlankTab);
		}

		protected void updateHomeButton(int type) {
			Button homeBtn = (Button) mTabsHolder.getChildAt(0);
			if (homeBtn == null) {
				Log.e(TAG, "HomeBtm is null,return.");
				return;
			}
			Log.d(TAG, "updateHomeButton.type:" + type + ",button:" + homeBtn);
			Resources resources = getResources();
			homeBtn.setBackgroundResource(R.drawable.custom_home_ninepatch_tab);
			homeBtn.setPadding((int) resources.getDimension(R.dimen.home_btn_padding), 0,
					(int) resources.getDimension(R.dimen.home_btn_padding), 0);
			homeBtn.setTextColor(Color.BLACK);
			switch (type) {
			case INTERNAL:
				homeBtn.setText(R.string.internal_sdcard);
				break;
			case SDCARD:
				homeBtn.setText(R.string.sdcard);
				break;
			default:
				break;
			}
		}

		public void refreshTab(String initFileInfo, int type) {
			Log.d(TAG, "refreshTab.initFileInfo:" + initFileInfo);
			int count = mTabsHolder.getChildCount();
			Log.d(TAG, "refreshTab.count:" + count);
			mTabsHolder.removeViews(0, count);
			mTabNameList.clear();

			curFilePath = initFileInfo;

			if (curFilePath != null) {
				String[] result = ZyStorageManager.getShowPath(mRootPath, curFilePath).split(ZyStorageManager.SEPERATOR);
				Log.d(TAG, "refreshTab.result:" + result);
				for (String string : result) {
					// add string to tab
					addTab(string);
				}
				startActionBarScroll();
			}

			updateHomeButton(type);
		}

		private void startActionBarScroll() {
			// scroll to right with slow-slide animation
			// To pass the Launch performance test, avoid the scroll
			// animation when launch.
			int tabHostCount = mTabsHolder.getChildCount();
			int navigationBarCount = mNavigationBar.getChildCount();
			if ((tabHostCount > 2) && (navigationBarCount >= 1)) {
				int width = mNavigationBar.getChildAt(navigationBarCount - 1).getRight();
				mNavigationBar.startHorizontalScroll(mNavigationBar.getScrollX(), width - mNavigationBar.getScrollX());
			}
		}

		/**
		 * This method updates the navigation view to the previous view when
		 * back button is pressed
		 * 
		 * @param newPath
		 *            the previous showed directory in the navigation history
		 */
		private void showPrevNavigationView(String newPath) {
			// refreshTab(newPath, storge_type);
//			browserTo(new File(newPath));
			Log.d(TAG, "showPrevNavigationView:" + newPath);
			doScanFiles(newPath);
		}

		/**
		 * This method creates tabs on the navigation bar
		 * 
		 * @param text
		 *            the name of the tab
		 */
		protected void addTab(String text) {
			Log.d(TAG, "addTab.text:" + text);
			LinearLayout.LayoutParams mlp = null;

			mTabsHolder.removeView(mBlankTab);
			View btn = null;
			if (mTabNameList.isEmpty()) {
				btn = new Button(mApplicationContext);
				mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.MATCH_PARENT));
				mlp.setMargins(0, 0, 0, 0);
				btn.setLayoutParams(mlp);
			} else {
				btn = new Button(mApplicationContext);

				((Button) btn).setTextColor(getResources().getColor(R.drawable.path_selector));
				btn.setBackgroundResource(R.drawable.custom_tab);
				if (text.length() <= 10) {
					((Button) btn).setText(text);
				} else {
					String tabItemText = text.substring(0, 10 - 3) + "...";
					((Button) btn).setText(tabItemText);
				}
				mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.MATCH_PARENT));
				mlp.setMargins((int) getResources().getDimension(R.dimen.tab_margin_left), 0, 0, 0);
				btn.setLayoutParams(mlp);
			}
			btn.setOnClickListener(FileBrowserFragment.this);
			btn.setId(mTabNameList.size());
			mTabsHolder.addView(btn);
			mTabNameList.add(text);

			// add blank tab to the tab holder
			mTabsHolder.addView(mBlankTab);
		}

		/**
		 * The method updates the navigation bar
		 * 
		 * @param id
		 *            the tab id that was clicked
		 */
		protected void updateNavigationBar(int id, int type) {
			Log.d(TAG, "updateNavigationBar,id = " + id + ",mTabNameList.size:" + mTabNameList.size());
			// click current button do not response
			if (id < mTabNameList.size() - 1) {
				if (mFileInfoAdapter.isMode(ActionMenu.MODE_EDIT)) {
					destroyMenuBar();
				}
				//remove 
				int count = mTabNameList.size() - (id + 1);
				mTabsHolder.removeViews(id + 1, count);

				for (int i = 1; i < count; i++) {
					// update mTabNameList
					mTabNameList.remove(mTabNameList.size() - 1);
				}
				// mTabsHolder.addView(mBlankTab);

				if (id == 0) {
					curFilePath = mRootPath;
				} else {
					String[] result = ZyStorageManager.getShowPath(mRootPath, curFilePath).split(ZyStorageManager.SEPERATOR);
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i <= id; i++) {
						sb.append(ZyStorageManager.SEPERATOR);
						sb.append(result[i]);
					}
					curFilePath = mRootPath + sb.toString();
				}

				int top = -1;
				FileInfo selectedFileInfo = null;
				if (mListView.getCount() > 0) {
					View view = mListView.getChildAt(0);
					selectedFileInfo = mFileInfoAdapter.getItem(mListView.getPositionForView(view));
					top = view.getTop();
				}
				addToNavigationList(mCurrentPath, top, selectedFileInfo);
//				browserTo(new File(curFilePath));
				doScanFiles(curFilePath);
			} else {
				// Refresh current page
				refreshUI();
			}
		}
		// end tab manager
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (STATUS_HOME == mStatus) {
			return;
		}

		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
//			Log.d(TAG, "SCROLL_STATE_FLING");
			mFileInfoAdapter.setFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
//			Log.d(TAG, "SCROLL_STATE_IDLE");
			mFileInfoAdapter.setFlag(true);
			mFileInfoAdapter.notifyDataSetChanged();
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
//			Log.d(TAG, "SCROLL_STATE_TOUCH_SCROLL");
			mFileInfoAdapter.setFlag(false);
			break;
		default:
			break;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	public void updateUI(int num) {
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}

//	public void goToHome() {
//		Log.d(TAG, "goToHome");
//		mAllLists.clear();
//		mNavBarLayout.setVisibility(View.GONE);
//
//		mStatus = STATUS_HOME;
//		updateUI(mHomeInfoList.size());
//		mListView.setAdapter(mHomeAdapter);
//		mHomeAdapter.notifyDataSetChanged();
//	}

	/**
	 * back key callback
	 */
	@Override
	public boolean onBackPressed() {
		Log.d(TAG, "onBackPressed.mStatus=" + mStatus);
		mIconHelper.stopLoader();
		if (mFileInfoAdapter.isMode(ActionMenu.MODE_EDIT) ||
		        mFileInfoAdapter.isMode(ActionMenu.MODE_COPY) ||
		        mFileInfoAdapter.isMode(ActionMenu.MODE_CUT)) {
			destroyMenuBar();
			return true;
		}

		switch (mStatus) {
		case STATUS_HOME:
			return super.onBackPressed();
		case STATUS_FILE:
			// if is root path,back to Home view
			if (isRootPath()) {
//				goToHome();
			    return super.onBackPressed();
			} else {
				NavigationRecord navRecord = mFileInfoManager.getPrevNavigation();
				String prevPath = null;
				if (null != navRecord) {
					prevPath = navRecord.getRecordPath();
					mSelectedFileInfo = navRecord.getSelectedFile();
					mTop = navRecord.getTop();
					if (null != prevPath) {
						mTabManager.showPrevNavigationView(prevPath);
						Log.d(TAG, "onBackPressed.prevPath=" + prevPath);
					}
				}
			}
			return true;
		default:
//			goToHome();
			return true;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onMenuItemClick(ActionMenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			List<Integer> posList = mFileInfoAdapter.getSelectedItemsPos();
			Log.d("delete.size=" + posList.size());
			for (int i = 0; i < posList.size(); i++) {
                Log.d("delete[" + i + "=" + posList.get(i));
            }
			showDeleteDialog(posList);
			break;
		case R.id.menu_select:
			doCheckAll();
			break;
		case R.id.menu_copy:
			mFileOperationHelper.copy(mFileInfoAdapter.getSelectedFileInfos());
			mFileInfoAdapter.changeMode(ActionMenu.MODE_COPY);
			startPasteMenu();
			break;
		case R.id.menu_cut:
			mFileOperationHelper.copy(mFileInfoAdapter.getSelectedFileInfos());
			
			mFileInfoAdapter.changeMode(ActionMenu.MODE_CUT);
			mFileInfoAdapter.notifyDataSetChanged();
			startPasteMenu();
			break;
		case R.id.menu_paste:
			if (mFileInfoAdapter.isMode(ActionMenu.MODE_COPY)) {
				onOperationPaste();
			}else if (mFileInfoAdapter.isMode(ActionMenu.MODE_CUT)) {
				onOperationCut();
			}else {
				Log.e(TAG, "ACTION_MENU_PASTE.Error");
			}
			break;
		case R.id.menu_cancel:
			mFileOperationHelper.clear();
			destroyMenuBar();
			break;
		case R.id.menu_more:
			ActionMenu actionMenu = new ActionMenu(mApplicationContext);
			actionMenu.addItem(ActionMenu.ACTION_MENU_RENAME, R.drawable.ic_action_rename_enable, R.string.rename);
			actionMenu.addItem(ActionMenu.ACTION_MENU_INFO, R.drawable.ic_action_info_enable, R.string.menu_info);
			
			final List<FileInfo> fileInfos = mFileInfoAdapter.getSelectedFileInfos();
			if (fileInfos.size() == 1) {
				FileInfo fileInfo = fileInfos.get(0);
				if (!fileInfo.isDir) {
					actionMenu.addItem(ActionMenu.ACTION_MENU_SHARE, R.drawable.ic_action_share, R.string.menu_share);
				}
			} 
			
			ZyPopupMenu popupMenu = new ZyPopupMenu(getActivity(), actionMenu);
			popupMenu.showAsLoaction(mMenuBarView, Gravity.RIGHT | Gravity.BOTTOM, 5, (int) mApplicationContext.getResources().getDimension(R.dimen.menubar_height));
			popupMenu.setOnPopupViewListener(new PopupViewClickListener() {
				@Override
				public void onActionMenuItemClick(ActionMenuItem item) {
					switch (item.getItemId()) {
					case ActionMenu.ACTION_MENU_RENAME:
						List<FileInfo> renameList = mFileInfoAdapter.getSelectedFileInfos();
						mFileInfoManager.showRenameDialog(mContext, renameList);
						mFileInfoAdapter.notifyDataSetChanged();
						
						destroyMenuBar();
						break;
					case ActionMenu.ACTION_MENU_INFO:
						List<FileInfo> list = mFileInfoAdapter.getSelectedFileInfos();
						mFileInfoManager.showInfoDialog(getActivity(), list);
						break;
					case ActionMenu.ACTION_MENU_SHARE:
						FileInfo fileInfo = fileInfos.get(0);
						File file = new File(fileInfo.filePath);
						
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_SEND);
						intent.setType("*/*");
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
						startActivity(intent);
						break;

					default:
						break;
					}
				}
			});
			break;
		case R.id.menu_create_folder:
			final ZyEditDialog editDialog = new ZyEditDialog(getActivity());
			editDialog.setTitle(R.string.create_folder);
			editDialog.setEditDialogMsg(mApplicationContext.getString(R.string.folder_input));
			editDialog.setEditStr(mApplicationContext.getString(R.string.new_folder));
			editDialog.selectAll();
			editDialog.showIME(true);
			editDialog.setPositiveButton(R.string.ok, new onZyDialogClickListener() {
				
				@Override
				public void onClick(Dialog dialog) {
					String folderName = editDialog.getEditTextStr();
					//verify name's format
					String ret = FileUtils.FileNameFormatVerify(mContext, folderName);
					if (null != ret) {
						editDialog.showTipMessage(true, ret);
						return;
					}else {
						editDialog.showTipMessage(false, null);
					}
					
					String newPath = mCurrentPath + File.separator + folderName;
					File file = new File(newPath);
					if (file.exists()) {
						mNotice.showToast(R.string.folder_exist);
					}else {
						if (!file.mkdir()) {
							mNotice.showToast(R.string.folder_create_failed);
						}else {
							FileInfo fileInfo = new FileInfo(newPath);
							fileInfo.fileDate = file.lastModified();
							fileInfo.filePath = newPath;
							fileInfo.isDir = true;
							fileInfo.fileSize = 0;
							fileInfo.type = FileManager.UNKNOW;
							fileInfo.count = 0;
							addToNavigationList(mCurrentPath, 0, fileInfo);
//							browserTo(file);
							doScanFiles(file);
						}
					}
					dialog.dismiss();
				}
			});
			editDialog.setNegativeButton(R.string.cancel, null);
			editDialog.show();
			break;
		default:
			break;
		}
	}

	public void destroyMenuBar() {
		super.destroyMenuBar(mMenuBarView);
		
//		updateTitleNum(-1);
		
		mFileInfoAdapter.changeMode(ActionMenu.MODE_NORMAL);
		mFileInfoAdapter.clearSelected();
		
//		mCopyList.clear();
	}

	@Override
	public void updateMenuBar() {
		int selectCount = mFileInfoAdapter.getSelectedItems();
//		updateTitleNum(selectCount);
		
		ActionMenuItem selectItem = mActionMenu.findItem(R.id.menu_select);
		if (mFileInfoAdapter.getCount() == selectCount) {
			selectItem.setTitle(R.string.unselect_all);
			selectItem.setEnableIcon(R.drawable.ic_aciton_unselect);
		} else {
			selectItem.setTitle(R.string.select_all);
			selectItem.setEnableIcon(R.drawable.ic_aciton_select);
		}

		if (0 == selectCount) {
			mActionMenu.findItem(R.id.menu_copy).setEnable(false);
			mActionMenu.findItem(R.id.menu_cut).setEnable(false);
			mActionMenu.findItem(R.id.menu_delete).setEnable(false);
			mActionMenu.findItem(R.id.menu_more).setEnable(false);
		} else if (mFileInfoAdapter.hasDirSelected()) {
			mActionMenu.findItem(R.id.menu_copy).setEnable(true);
			mActionMenu.findItem(R.id.menu_cut).setEnable(true);
			mActionMenu.findItem(R.id.menu_delete).setEnable(true);
			mActionMenu.findItem(R.id.menu_more).setEnable(true);
		}else {
			mActionMenu.findItem(R.id.menu_copy).setEnable(true);
			mActionMenu.findItem(R.id.menu_cut).setEnable(true);
			mActionMenu.findItem(R.id.menu_delete).setEnable(true);
			mActionMenu.findItem(R.id.menu_more).setEnable(true);
		}
	}

	@Override
	public void doCheckAll() {
		int selectedCount = mFileInfoAdapter.getSelectedItems();
		if (mFileInfoAdapter.getCount() != selectedCount) {
			mFileInfoAdapter.selectAll(true);
		} else {
			mFileInfoAdapter.selectAll(false);
		}
//		updateMenuBar();
//		mMenuBarManager.refreshMenus(mActionMenu);
		mFileInfoAdapter.notifyDataSetChanged();
	}
	
	public void startPasteMenu(){
//		mCopyList = mFileInfoAdapter.getSelectedFileInfos();
		//update new menu
//		mActionMenu = new ActionMenu(mApplicationContext);
		if (mActionMenu == null) {
            mActionMenu = new ActionMenu(mApplicationContext);
        }
		getActionMenuInflater().inflate(R.menu.allfile_menu_paste, mActionMenu);
		mMenuBarManager.refreshMenus(mActionMenu);
	}
	
	public void startSinglePasteMenu(List<FileInfo> list){
//	    mCopyList = list;
	    if (mActionMenu == null) {
            mActionMenu = new ActionMenu(mApplicationContext);
        }
	    getActionMenuInflater().inflate(R.menu.allfile_menu_paste, mActionMenu);
	    startMenuBar(mMenuBarView);
	}
	
	private void onOperationPaste(){
		showCopyDialog(mApplicationContext.getString(R.string.filecopy));
		if (!mFileOperationHelper.doCopy(mCurrentPath)) {
			onFinished();
		}
	}
	
	private void onOperationCut(){
		Log.d(TAG, "onOperationCut()");
		showCopyDialog(mApplicationContext.getString(R.string.filecut));
		if (!mFileOperationHelper.doCut(mCurrentPath)) {
			onFinished();
		}
	}
	
	private ProgressDialog progressDialog = null;
	public void showProgressDialog(String msg){
		progressDialog = new ProgressDialog(getActivity());
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(true);
		progressDialog.setMessage(msg);
		progressDialog.setIndeterminate(true);
		progressDialog.show();
	}
	
	private CopyMoveDialog mCopyDialog = null;
	public void showCopyDialog(String title){
		mCopyDialog = new CopyMoveDialog(getActivity());
		mCopyDialog.setDialogTitle(title);
		mCopyDialog.setDesPath(mCurrentPath);
		mCopyDialog.setCancelable(false);
		mCopyDialog.setNegativeButton(R.string.cancel, new onZyDialogClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				mFileOperationHelper.stopCopy();
				dialog.dismiss();
				mHandler.sendMessage(mHandler.obtainMessage(MSG_OPERATION_OVER));
			}
		});
		mCopyDialog.show();
	}

	@Override
	public void onFinished() {
		Log.d(TAG, "onFinished");
		if (null != progressDialog) {
			progressDialog.dismiss();
			progressDialog = null;
		}
		
		if (null != mCopyDialog) {
			mCopyDialog.dismiss();
			mCopyDialog = null;
		}
		mHandler.sendMessage(mHandler.obtainMessage(MSG_OPERATION_OVER));
	}
	
	@Override
	public void onNotify(int msg) {
		switch (msg) {
		case FileOperationHelper.MSG_COPY_CUT_TO_CHILD:
			Message message = mHandler.obtainMessage();
			message.obj = mApplicationContext.getString(R.string.copy_cut_fail);
			message.what = MSG_OPERATION_NOTIFY;
			message.sendToTarget();
			break;
		default:
			break;
		}
	}

	@Override
	public void onRefreshFiles(String fileName, int count, long filesize, long copysize) {
		if (null == fileName && count != -1) {
			mCopyDialog.setTotalCount(count);
		}else if(null != fileName && copysize == -1){
			mCopyDialog.updateCountProgress(fileName, count, filesize);
		}else if (null == fileName && copysize != -1) {
			mCopyDialog.updateSingleFileProgress(copysize);
		}
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (null != mFileOperationHelper) {
			mFileOperationHelper.cancelOperationListener();
			mFileOperationHelper = null;
		}
		
		if (null != mDeleteHelper) {
			mDeleteHelper.cancelDeleteListener();
			mDeleteHelper = null;
		}
	}

	@Override
	public void onDeleteFinished() {
		Log.d(TAG, "onDeleteFinished");
		//when delete over,send message to update ui
		Message message = mHandler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putIntegerArrayList("position", (ArrayList<Integer>)mDeletePosList);
		message.setData(bundle);
		message.what = MSG_UPDATE_LIST;
		message.sendToTarget();
	}
	
	private boolean isRootPath(){
		return mRootPath.equals(mCurrentPath);
	}
	
	private GetFileTask mGetFileTask = null;
	private class GetFileTask extends AsyncTask<File, Void, Void>{
		
		private List<FileInfo> mFileInfos;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mLoadingBar.setVisibility(View.VISIBLE);
			
			mFileInfos = new ArrayList<FileInfo>();
		}

		@Override
		protected Void doInBackground(File... params) {
			// TODO Auto-generated method stub
//			browserTo(file)
			File file = params[0];
			Log.d(TAG, "GetFileTask.status=" + mStatus);
			if (file.isDirectory()) {
				mCurrentPath = file.getAbsolutePath();

				// clearList();
				mFolderLists.clear();
				mFileLists.clear();

				fillList(file.listFiles());

				// sort
				Collections.sort(mFolderLists, NAME_COMPARATOR);
				Collections.sort(mFileLists, NAME_COMPARATOR);

				mFileInfos.addAll(mFolderLists);
				mFileInfos.addAll(mFileLists);

				// System.out.println("mFileInfos:" + mFileInfos.size());
				// mAllLists = mFileInfos;
				// System.out.println("mAllLists:" + mAllLists.size());
				// mFileInfoAdapter.selectAll(false);
			} else {
				Log.e(TAG, "It is a file");
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mLoadingBar.setVisibility(View.GONE);
			
			mFileInfoAdapter.setDataList(mFileInfos);
			mFileInfoAdapter.selectAll(false);
			
//			mFileInfoAdapter.notifyDataSetChanged();
			int seletedItemPosition = restoreSelectedPosition();
			// Log.d(TAG, "seletedItemPosition:" + seletedItemPosition +
			// ",mTop=" + mTop);
			if (seletedItemPosition == -1) {
				mListView.setSelectionAfterHeaderView();
			} else if (seletedItemPosition >= 0 && seletedItemPosition < mFileInfoAdapter.getCount()) {
				if (mTop == -1) {
					mListView.setSelection(seletedItemPosition);
				} else {
					mListView.setSelectionFromTop(seletedItemPosition, mTop);
					mTop = -1;
				}
			}

			
			updateUI(mAllLists.size());
			mTabManager.refreshTab(mCurrentPath, storge_type);
//			mTabManager.updateHomeButton(storge_type);
		}
		
	}
}
