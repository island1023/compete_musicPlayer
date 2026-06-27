package com.example.musicplayer;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CountrySelectFragment extends BottomSheetDialogFragment {

    private String[] countryData = {"中国 +86", "美国 +1", "中国香港 +852", "日本 +81", "韩国 +82", "英国 +44", "加拿大 +1"};
    private OnCountrySelectListener listener;

    // 定义一个回调接口，把选择的区号传回给登录页
    public interface OnCountrySelectListener {
        void onCountrySelected(String countryCode);
    }

    public void setOnCountrySelectListener(OnCountrySelectListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // 监听弹窗显示，动态设置高度为屏幕的 3/4
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                int targetHeight = (int) (screenHeight * 0.75); // 计算 3/4 高度

                bottomSheetInternal.getLayoutParams().height = targetHeight;
                behavior.setPeekHeight(targetHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_country_select, container, false);

        ImageView ivClose = view.findViewById(R.id.iv_close);
        ListView lvCountry = view.findViewById(R.id.lv_country);

        // 点击退出按钮，关闭弹窗
        ivClose.setOnClickListener(v -> dismiss());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, countryData);
        lvCountry.setAdapter(adapter);

        // 点击列表项
        lvCountry.setOnItemClickListener((parent, v, position, id) -> {
            String selected = countryData[position];
            String code = selected.substring(selected.indexOf("+"));
            if (listener != null) {
                listener.onCountrySelected(code); // 把区号传回给 LoginActivity
            }
            dismiss(); // 关闭弹窗
        });

        return view;
    }
}