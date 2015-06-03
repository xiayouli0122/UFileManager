package com.yuri.ufm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 策划菜单
 * @author Yuri
 *
 */
public class LeftMenuFragment extends BaseFragment{
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_left_menu, null);
        return view;
    }
}
