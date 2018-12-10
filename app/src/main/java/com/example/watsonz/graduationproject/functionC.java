package com.example.watsonz.graduationproject;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by watsonz on 2016-11-24.
 */
public class functionC extends Fragment implements View.OnClickListener{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private TextView result_tv,total_tv;
    String data = "";
    private Button start_listen_btn,stop_listen_btn,mute;
    private SpeechRecognizerManager mSpeechManager;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private EditText inputtext;
    private Button bt_save;
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static functionC newInstance(int sectionNumber) {
        functionC fragment = new functionC();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public functionC() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_function_c, container, false);
        result_tv=(TextView)rootView.findViewById(R.id.result_tv);
        total_tv=(TextView)rootView.findViewById(R.id.test);
        start_listen_btn=(Button)rootView.findViewById(R.id.start_listen_btn);
        start_listen_btn.setOnClickListener(this);
        inputtext=(EditText)rootView.findViewById(R.id.editText);
        bt_save=(Button)rootView.findViewById(R.id.save_btn);
        bt_save.setOnClickListener(this);

        Intent mCheckIntent=new Intent();
        mCheckIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(mCheckIntent, 1);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to write the permission.
                Toast.makeText(getContext(),"Read/Write external storage", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);

            // MY_PERMISSION_REQUEST_STORAGE is an
            // app-defined int constant

        }
        return rootView;
    }
    @Override
    public void onClick(View v) {
        if(PermissionHandler.checkPermission(getActivity(),PermissionHandler.RECORD_AUDIO)) {

            switch (v.getId()) {
                case R.id.start_listen_btn:
                    if(mSpeechManager==null)
                    {
                        SetSpeechListener();
                        start_listen_btn.setBackground(this.getResources().getDrawable(R.drawable.bt_stop));
                        result_tv.setText(getString(R.string.record_helper));
                    }
                    else if(mSpeechManager!=null) {
                        result_tv.setText(getString(R.string.destroied));
                        mSpeechManager.destroy();
                        mSpeechManager = null;
                        start_listen_btn.setBackground(this.getResources().getDrawable(R.drawable.bt_start_stt));
                        result_tv.setText(getString(R.string.destroied));
                    }
                    break;
                case R.id.save_btn:
                    if(total_tv.getText().toString().equals("")){
                        return;
                    }
                    long now = System.currentTimeMillis();
                    Date date = new Date(now);
                    SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String strNow = sdfNow.format(date);
                    String strData = total_tv.getText().toString().trim();
                    saveAsText(strData, "myRecord", strNow);
                    break;
            }
        }
        else
        {
            PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO,getActivity());
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode)
        {
            case PermissionHandler.RECORD_AUDIO:
                if(grantResults.length>0) {
                    if(grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                        start_listen_btn.performClick();
                    }
                }
                break;

        }
    }

    private void SetSpeechListener(){
        mSpeechManager=new SpeechRecognizerManager(getContext(), new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {

                if(results!=null && results.size()>0)
                {

                    if(results.size()==1)
                    {
                        mSpeechManager.destroy();
                        mSpeechManager = null;
                        result_tv.setText(results.get(0));
                    }
                    else {
                        StringBuilder sb = new StringBuilder();
                        if (results.size() > 5) {
                            results = (ArrayList<String>) results.subList(0, 5);
                        }
                        for (String result : results) {
                            sb.append(result).append("\n");
                        }
                        //result_tv.setText(sb.toString());
                        data = data.concat(results.get(0)+"\n");
                        total_tv.setText(data);
                    }
                }
                else
                    result_tv.setText(getString(R.string.no_results_found));
            }
        });
    }
    @Override
    public void onPause() {
        if(mSpeechManager!=null) {
            mSpeechManager.destroy();
            mSpeechManager=null;
        }
        super.onPause();
    }
    public void saveAsText(String data, String folder, String date){
        String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Get Absolute Path in External Sdcard
        String foler_name = "/"+folder+"/";
        String file_name = date+".txt";
        String string_path = ex_storage+foler_name;
        //String file_name = name+".jpg";
        File file_path;
        try{
            file_path = new File(string_path);
            if(!file_path.isDirectory()){
                if(file_path.mkdirs())
                {
                    Log.d("file created", "");
                }
                else
                {
                    Log.d("file creation error","");
                }
            }
            File file_txt = new File(string_path+file_name);
            FileOutputStream fos = new FileOutputStream(file_txt);
            BufferedWriter buw = new BufferedWriter(new OutputStreamWriter(fos, "UTF8"));
            buw.write(data);
            buw.close();
            fos.close();
            total_tv.setText("");
            Toast.makeText(getContext(),"myRecord 폴더에 저장되었습니다.",Toast.LENGTH_SHORT).show();
            Log.d("file location", string_path);
        }catch(FileNotFoundException exception){
            Log.e("FileNotFoundException", exception.getMessage());
        }catch(IOException exception){
            Log.e("IOException", exception.getMessage());
        }
    }
}
