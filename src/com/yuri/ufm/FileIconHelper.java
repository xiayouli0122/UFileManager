package com.yuri.ufm;


import java.util.HashMap;

import android.content.Context;
import android.widget.ImageView;

import com.yuri.ufm.FileCategoryHelper.FileCategory;
import com.yuri.ufm.FileIconLoader.IconLoadFinishListener;
import com.zhaoyan.common.utils.Log;

public class FileIconHelper implements IconLoadFinishListener {

    private static final String TAG = "FileIconHelper";
    private static HashMap<String, Integer> fileExtToIcons = new HashMap<String, Integer>();

    private FileIconLoader mIconLoader;

    static {
        addItem(FileCategoryHelper.AUDIO_EXTS, R.drawable.icon_audio);
        addItem(FileCategoryHelper.VIDEO_EXTS, R.drawable.icon_video);
        addItem(FileCategoryHelper.IMAGE_EXTS, R.drawable.icon_image);
        addItem(FileCategoryHelper.EBOOK_EXTS, R.drawable.icon_txt);
        addItem(FileCategoryHelper.WORD_EXTS, R.drawable.icon_doc);
        addItem(FileCategoryHelper.PPT_EXTS, R.drawable.icon_ppt);
        addItem(FileCategoryHelper.EXCEL_EXTS, R.drawable.icon_xls);
        addItem(FileCategoryHelper.APK_EXTS, R.drawable.icon_apk);
        addItem(FileCategoryHelper.ARCHIVE_EXTS, R.drawable.icon_rar);
        addItem(FileCategoryHelper.PDF_EXTS, R.drawable.icon_pdf);
    }

    public FileIconHelper(Context context) {
        mIconLoader = new FileIconLoader(context, this);
    }

	private static void addItem(String[] exts, int resId) {
		if (exts != null) {
			for (String ext : exts) {
				fileExtToIcons.put(ext.toLowerCase(), resId);
			}
		}
	}

    public static int getFileIcon(String ext) {
        Integer i = fileExtToIcons.get(ext.toLowerCase());
        if (i != null) {
            return i.intValue();
        } else {
            return R.drawable.icon_file;
        }

    }

    public void setIcon(FileInfo fileInfo, ImageView fileImage) {
        String filePath = fileInfo.filePath;
        String ext = FileManager.getExtFromFilename(fileInfo.fileName);
        FileCategory fc = FileCategoryHelper.getCategoryByName(fileInfo.fileName);
        boolean set = false;
        int id = getFileIcon(ext);
        fileImage.setImageResource(id);
        mIconLoader.cancelRequest(fileImage);
        switch (fc) {
            case Apk:
                set = mIconLoader.loadIcon(fileImage, filePath, fc);
                if (!set) {
                	fileImage.setImageResource(R.drawable.icon_apk);
                    set = true;
				}
                break;
            case Image:
            case Video:
                set = mIconLoader.loadIcon(fileImage, filePath, fc);
                if (!set){
                    fileImage.setImageResource(fc == FileCategory.Image ? R.drawable.icon_image
                            : R.drawable.icon_video);
                    set = true;
                }
                break;
            default:
                set = true;
                break;
        }
        if (!set)
            fileImage.setImageResource(R.drawable.icon_file);
    }

    @Override
    public void onIconLoadFinished(ImageView view) {
    	Log.d(TAG, "onIconLoadFinished");
    }
    
    public void stopLoader(){
    	mIconLoader.cancelAllRequest();
    }

}
