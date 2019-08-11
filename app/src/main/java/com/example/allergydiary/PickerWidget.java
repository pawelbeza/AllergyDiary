package com.example.allergydiary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

abstract public class PickerWidget extends LinearLayout {
    private static final String TAG = "InlineCalendar";
    protected Button btnPrev;
    protected Button btnNext;
    protected MyOnClickListener myOnClickListener;

    public PickerWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(MyOnClickListener myOnClickListener) {
        this.myOnClickListener = myOnClickListener;
    }

    abstract protected void assignUiElements();

    protected void initInterface() {
        myOnClickListener = new MyOnClickListener() {
            @Override
            public void onClickListener() {
            }
        };
    }

    protected void assignClickHandlers() {
        assignOnClickListener(btnPrev, -1);
        assignOnClickListener(btnNext, 1);
    }

    protected void assignOnClickListener(final Button btn, final int addToPicker) {
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePicker(addToPicker);
                myOnClickListener.onClickListener();
                arrowVisibility();
            }
        });
    }

    protected void arrowVisibility(){};

    abstract protected void initControl(Context context);

    abstract public void updatePicker(int addToPicker);

    public interface MyOnClickListener {
        void onClickListener();
    }
}
