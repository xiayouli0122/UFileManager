package com.yuri.ufm;

import com.zhaoyan.common.utils.Utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class FileListItem {
    public static void setupFileListItemInfo(Context context, View view,
            FileInfo fileInfo, FileIconHelper fileIcon) {

        setText(view, R.id.tv_filename, fileInfo.fileName);
        setText(view, R.id.tv_filecount, fileInfo.isDir ? "(" + fileInfo.count + ")" : "");
    	String size = Utils.getFormatSize(fileInfo.fileSize);
		String date = Utils.getFormatDate(fileInfo.fileDate);
        setText(view, R.id.tv_fileinfo, fileInfo.isDir ? date : date + " | " + size);

        ImageView imageView = (ImageView) view.findViewById(R.id.file_icon_imageview);

        if (fileInfo.isDir) {
            imageView.setImageResource(R.drawable.icon_folder);
        } else {
            fileIcon.setIcon(fileInfo, imageView);
        }
    }
    
    private static boolean setText(View view, int resId, String text){
    	 TextView textView = (TextView) view.findViewById(resId);
         if (textView == null)
             return false;

         textView.setText(text);
         return true;
    }
}
