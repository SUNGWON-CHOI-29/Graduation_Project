package com.example.watsonz.graduationproject;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by watsonz on 2016-05-12.
 */
    /**
     * A placeholder fragment containing a simple view.
     */
    public class functionB extends Fragment implements View.OnClickListener{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private TextView result_tv,total_tv;
        String data = "";
        private Button start_listen_btn,stop_listen_btn,mute;
        private SpeechRecognizerManager mSpeechManager;
        private static final String ARG_SECTION_NUMBER = "section_number";
        private TextToSpeech tts;
        private EditText inputtext;
        private Button button;
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static functionB newInstance(int sectionNumber) {
            functionB fragment = new functionB();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public functionB() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_function_b, container, false);
            result_tv=(TextView)rootView.findViewById(R.id.result_tv);
            total_tv=(TextView)rootView.findViewById(R.id.test);
            start_listen_btn=(Button)rootView.findViewById(R.id.start_listen_btn);
            stop_listen_btn=(Button)rootView.findViewById(R.id.stop_listen_btn);
            mute=(Button)rootView.findViewById(R.id.mute);
            mute.setVisibility(mute.GONE);
            start_listen_btn.setOnClickListener(this);
            stop_listen_btn.setOnClickListener(this);
            stop_listen_btn.setVisibility(stop_listen_btn.GONE);
            mute.setOnClickListener(this);
            inputtext=(EditText)rootView.findViewById(R.id.editText);
            button=(Button)rootView.findViewById(R.id.button);

            Intent mCheckIntent=new Intent();
            mCheckIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(mCheckIntent, 1);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stop_listen_btn.performClick();
                    String text = inputtext.getText().toString();
                    Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
                    //http://stackoverflow.com/a/29777304
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ttsGreater21(text);
                    } else {
                        ttsUnder20(text);
                    }
                }
            });
            return rootView;
        }
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(requestCode==1)
            {
                switch (resultCode) {
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                        tts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if(status==TextToSpeech.SUCCESS) {
                                    tts.setLanguage(Locale.KOREAN);
                                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                        @Override
                                        public void onDone(String utteranceId) {
                                            // Log.d("MainActivity", "TTS finished");
                                            getActivity().runOnUiThread(new Runnable() {
                                                public void run() {
                                                    //Do something on UiThread
                                                    start_listen_btn.performClick();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(String utteranceId) {
                                        }

                                        @Override
                                        public void onStart(String utteranceId) {
                                        }
                                    });
                                }
                            }
                        });
                        //Log.v(TAG, "tts engine is instance");
                        break;
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
                        Intent mUpdateData=new Intent();
                        mUpdateData.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(mUpdateData);
                        break;
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
                        //Log.v(TAG, "TTS engine checked fail");
                        break;
                    default:
                        //Log.v(TAG, "Got a failure. TTS apparently not available");
                        break;
                }

            }

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
                            result_tv.setText(getString(R.string.you_may_speak));
                        }
                        else if(mSpeechManager!=null) {
                            result_tv.setText(getString(R.string.destroied));
                            mSpeechManager.destroy();
                            mSpeechManager = null;
                            start_listen_btn.setBackground(this.getResources().getDrawable(R.drawable.bt_start_stt));
                            result_tv.setText(getString(R.string.destroied));
                        }
                        break;
                    case R.id.stop_listen_btn:
                        if(mSpeechManager!=null) {
                            result_tv.setText(getString(R.string.destroied));
                            mSpeechManager.destroy();
                            mSpeechManager = null;
                        }
                        break;
                    case R.id.mute:
                        if(mSpeechManager!=null) {
                            if(mSpeechManager.isInMuteMode()) {
                                mute.setText(getString(R.string.mute));
                                mSpeechManager.mute(false);
                            }
                            else
                            {
                                mute.setText(getString(R.string.un_mute));
                                mSpeechManager.mute(true);
                            }
                        }
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
            if(tts !=null){
                tts.stop();
                tts.shutdown();
            }
            super.onPause();
        }
        @SuppressWarnings("deprecation")
        private void ttsUnder20(String text) {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void ttsGreater21(String text) {
            String utteranceId=this.hashCode() + "";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }
