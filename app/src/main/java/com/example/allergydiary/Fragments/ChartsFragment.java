package com.example.allergydiary.Fragments;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.allergydiary.AllergyDiaryDatabase.AllergicSymptom;
import com.example.allergydiary.AllergyDiaryDatabase.AllergicSymptomViewModel;
import com.example.allergydiary.BuildConfig;
import com.example.allergydiary.DateAxisValueFormatter;
import com.example.allergydiary.R;
import com.example.allergydiary.Widgets.InlineCalendarPickerWidget;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class ChartsFragment extends Fragment {
    //TODO add support for landscape view
    //TODO fix splash screen(back btn)
    private long referenceTimestamp = Long.MAX_VALUE;
    private BarChart barChart;
    private InlineCalendarPickerWidget calendarPicker;
    private ArrayList<BarEntry> Values = new ArrayList<>();
    private AllergicSymptomViewModel symptomViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_charts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        symptomViewModel = ViewModelProviders.of(getActivity()).get(AllergicSymptomViewModel.class);

        barChart = view.findViewById(R.id.BarChart);


        calendarPicker = view.findViewById(R.id.inlineCalendar);
        calendarPicker.setListener(new InlineCalendarPickerWidget.MyOnClickListener() {
            @Override
            public void onClickListener() {
                Calendar cal = calendarPicker.getCalendar();

                cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
                cal.clear(Calendar.MINUTE);
                cal.clear(Calendar.SECOND);
                cal.clear(Calendar.MILLISECOND);

                cal.set(Calendar.DAY_OF_MONTH, 1);
                long startDate = cal.getTimeInMillis();

                cal.add(Calendar.MONTH, 1);
                cal.add(Calendar.MILLISECOND, -1);
                long endDate = cal.getTimeInMillis();
                makeAndDisplayGraph(barChart, true, startDate, endDate);
            }
        });

        getCurrMonth();

        Button button = getActivity().findViewById(R.id.generateReport);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveGraphToPDF();
            }
        });

    }

    private void saveGraphToPDF(){
        if (!isExternalStorageReadable()) {
            return;
        }

        PdfDocument document = new PdfDocument();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(2250, 1400, 1).create();

        PdfDocument.Page page = document.startPage(pageInfo);

        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.pdf_layout, null);

        int measureWidth = View.MeasureSpec.makeMeasureSpec(page.getCanvas().getWidth(), View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(page.getCanvas().getHeight(), View.MeasureSpec.EXACTLY);

        content.measure(measureWidth, measuredHeight);
        content.layout(0, 0, page.getCanvas().getWidth(), page.getCanvas().getHeight());

        BarChart barChartToPDF = content.findViewById(R.id.barChartToPDF);

//        makeAndDisplayGraph(barChartToPDF, false, fromDate, toDate);

        content.draw(page.getCanvas());

//         finish the page
        document.finishPage(page);

        String targetPdf = getActivity().getExternalFilesDir(null).getPath() + File.separator + "allergy_report.pdf";
        try {
            //make sure you have asked for storage permission before this
            File f = new File(targetPdf);
            document.writeTo(new FileOutputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileWithinMyDir = new File(targetPdf);


        if(fileWithinMyDir.exists()) {
            Uri uri = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", fileWithinMyDir);
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            intentShareFile
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .setType("application/pdf")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION )
                    .putExtra(Intent.EXTRA_SUBJECT, "Allergy Symptoms report")
                    .putExtra(Intent.EXTRA_TEXT, "Allergy Symptoms report");

            startActivity(intentShareFile);
        }
    }


    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }


    private void getCurrMonth() {
        Calendar cal = calendarPicker.getCalendar();

        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        long startDate = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long endDate = cal.getTimeInMillis();

        makeAndDisplayGraph(barChart, true, startDate, endDate);
    }

    private void makeAndDisplayGraph(BarChart barChart, boolean animate, long fromDate, long toDate) {
        Values.clear();
        getDataInRange(fromDate, toDate);

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);

        barChart.getDescription().setEnabled(false);

        if (animate)    barChart.animateY(1500);

        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);


        ValueFormatter xAxisFormatter = new DateAxisValueFormatter(referenceTimestamp);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(xAxisFormatter);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setLabelCount(10, false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(10f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        rightAxis.setGranularity(1f);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(10f);

        BarDataSet barDataSet = new BarDataSet(Values, "AllergicSymptom");
        int startColor = ContextCompat.getColor(getActivity(), R.color.bright_green);
        int endColor = ContextCompat.getColor(getActivity(), R.color.bright_blue);
        barDataSet.setGradientColor(startColor, endColor);

        BarData barData = new BarData(barDataSet);
        barData.setDrawValues(false);

        barChart.setData(barData);
        barChart.invalidate();
    }

    private void getDataInRange(long fromDate, long toDate) {
        List<AllergicSymptom> symptoms = symptomViewModel.getDataBaseContents(fromDate, toDate);
        for (AllergicSymptom symptom : symptoms) {
            long date = symptom.getDate();
            date = TimeUnit.MILLISECONDS.toDays(date) + 1; // +1 because TimeUnit rounds down
            int feeling = symptom.getFeeling();
            boolean medicine = symptom.isMedicine();
            referenceTimestamp = Math.min(referenceTimestamp, date);
            if (medicine) {
                Drawable icon;
                if (android.os.Build.VERSION.SDK_INT >= 21)
                    icon = getResources().getDrawable(R.drawable.ic_pill, null);
                else
                    icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_pill);
                Values.add(new BarEntry(date, feeling, icon));
            }
            else
                Values.add(new BarEntry(date, feeling));
        }

        for (int i = 0; i < Values.size(); i++) {
            float tmp = Values.get(i).getX() - referenceTimestamp;
            Values.get(i).setX(tmp);
        }
    }
}
