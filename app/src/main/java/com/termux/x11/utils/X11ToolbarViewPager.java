package com.termux.x11.utils;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;

import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.x11.MainActivity;
import com.termux.x11.R;

public class X11ToolbarViewPager {
    public static class PageAdapter extends PagerAdapter {

        final MainActivity mActivity;
        private final View.OnKeyListener mEventListener;
        public TermuxX11ExtraKeys mExtraKeys; //由于现在有两个pager，不能再使用activity中的唯一一个extraKey变量
        /** 标明是左侧还是右侧。0为左侧1为右侧. 分别对应pref的key是extra_keys_config 和 extra_keys_config2 */
        public int side = 0;
        public PageAdapter(MainActivity activity, View.OnKeyListener listen) {
            this.mActivity = activity;
            this.mEventListener = listen;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View layout;
            if (position == 0) {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                ExtraKeysView extraKeysView = (ExtraKeysView) layout;
                //从activity属性改为自身属性。并设置左右侧
                mExtraKeys = new TermuxX11ExtraKeys(mEventListener, mActivity, extraKeysView);
                mExtraKeys.side = this.side;
                int mTerminalToolbarDefaultHeight = mActivity.getTerminalToolbarViewPager().getLayoutParams().height;
                int height = mTerminalToolbarDefaultHeight *
                        ((mExtraKeys.getExtraKeysInfo() == null) ? 0 : mExtraKeys.getExtraKeysInfo().getMatrix().length);
                extraKeysView.reload(mExtraKeys.getExtraKeysInfo(), height);
                //按钮在reload里被设置为黑色背景。虽然下面处理点击事件时会将其设置为透明背景，
                // 但初次添加后未点击过的按钮还是黑色。所以手动设置一遍
                for(int i=0; i<extraKeysView.getChildCount(); i++)
                    if(extraKeysView.getChildAt(i) instanceof Button)
                        extraKeysView.getChildAt(i).setBackgroundColor(0x00000000);
                extraKeysView.setExtraKeysViewClient(mExtraKeys);
                extraKeysView.setOnHoverListener((v, e) -> true);
                extraKeysView.setOnGenericMotionListener((v, e) -> true);
            } else {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);
                final Button back = layout.findViewById(R.id.terminal_toolbar_back_button);

                editText.setOnEditorActionListener((v, actionId, event) -> {
                    String textToSend = editText.getText().toString();
                    if (textToSend.length() == 0) textToSend = "\r";
                    KeyEvent e = new KeyEvent(0, textToSend, KeyCharacterMap.VIRTUAL_KEYBOARD, 0);
                    mEventListener.onKey(mActivity.getLorieView(), 0, e);

                    editText.setText("");
                    return true;
                });

                editText.setOnCapturedPointerListener((v2, e2) -> {
                    v2.releasePointerCapture();
                    return false;
                });

                back.setOnClickListener(v -> mActivity.getTerminalToolbarViewPager().setCurrentItem(0, true));
                back.setTextColor(0xFFFFFFFF);
                back.setPadding(0, 0, 0, 0);
                back.setBackground(new ColorDrawable(Color.BLACK) {
                    public boolean isStateful() {
                        return true;
                    }
                    public boolean hasFocusStateSpecified() {
                        return true;
                    }
                });
                back.setOnTouchListener((view, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            view.setBackgroundColor(0xFF7F7F7F);
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            view.setBackgroundColor(0x00000000);
                            break;
                    }
                    return false;
                });
            }
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

    }

    public static class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        final MainActivity act;
        final ViewPager mTerminalToolbarViewPager;

        public OnPageChangeListener(MainActivity activity, ViewPager viewPager) {
            this.act = activity;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                act.getLorieView().requestFocus();
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }
    }
}
