package com.example.watsonz.graduationproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by watsonz on 2016-05-12.
 */
    /**
     * A placeholder fragment containing a simple view.
     */
    public class functionA extends Fragment implements View.OnClickListener{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        SoundPoolPlayer sound;
        int frequency = 8000;
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        // 우리의 FFT 객체는 transformer고, 이 FFT 객체를 통해 AudioRecord 객체에서 한 번에 256가지 샘플을 다룬다. 사용하는 샘플의 수는 FFT 객체를 통해
        // 샘플들을 실행하고 가져올 주파수의 수와 일치한다. 다른 크기를 마음대로 지정해도 되지만, 메모리와 성능 측면을 반드시 고려해야 한다.
        // 적용될 수학적 계산이 프로세서의 성능과 밀접한 관계를 보이기 때문이다.
        private RealDoubleFFT transformer;
        int blockSize = 256;
        Button startStopButton;
        boolean started = false;
        boolean finish = false;
        boolean set = false;
        double average, count;
        // RecordAudio는 여기에서 정의되는 내부 클래스로서 AsyncTask를 확장한다.
        RecordAudio recordTask;

        Vibrator vb;

        // Bitmap 이미지를 표시하기 위해 ImageView를 사용한다. 이 이미지는 현재 오디오 스트림에서 주파수들의 레벨을 나타낸다.
        // 이 레벨들을 그리려면 Bitmap에서 구성한 Canvas 객체와 Paint객체가 필요하다.
        ImageView imageView;
        Bitmap bitmap;
        Canvas canvas;
        Paint paint;
        TextView tv_hz;
        TextView tv_current_db;
        TextView tv_threshold;
        SeekBar sb_threshold;
        SeekBar bar_content;

        LinearLayout bar_db;

        String tmp;
        int avr_count;
        double avr_value;;
        double decibel;
        double hz;
        double min_hz;
        double max_hz;
        int threshold;

        int match_count;
        int[] match_index;
        int maxIndex = 0;
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static functionA newInstance(int sectionNumber) {
            functionA fragment = new functionA();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public functionA() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_function_a, container, false);
            startStopButton = (Button)rootView.findViewById(R.id.StartStopButton);
            startStopButton.setOnClickListener(this);

            sound = new SoundPoolPlayer(rootView.getContext());

            // RealDoubleFFT 클래스 컨스트럭터는 한번에 처리할 샘플들의 수를 받는다. 그리고 출력될 주파수 범위들의 수를 나타낸다.
            transformer = new RealDoubleFFT(blockSize);

            // ImageView 및 관련 객체 설정 부분
            imageView = (ImageView)rootView.findViewById(R.id.ImageView01);
            bitmap = Bitmap.createBitmap((int)1024, (int)400, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            paint = new Paint();
            paint.setColor(Color.BLACK);
            imageView.setImageBitmap(bitmap);
            /*Button bt_clear = (Button)rootView.findViewById(R.id.bt_clear);
            bt_clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish = !finish;
                    Toast.makeText(getContext(), "state :"+finish, Toast.LENGTH_SHORT).show();
                }
            });*/
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.RECORD_AUDIO)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            1);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
            bar_db = (LinearLayout)rootView.findViewById(R.id.bar_db);
            bar_db.setVisibility(bar_db.GONE);
            tv_hz = (TextView)rootView.findViewById(R.id.tv_hz);
            tv_hz.setText("");
            tv_hz.setVisibility(tv_hz.GONE);
            tv_current_db = (TextView)rootView.findViewById(R.id.tv_current_db);
            tv_current_db.setVisibility(tv_current_db.GONE);
            tv_threshold = (TextView)rootView.findViewById(R.id.tv_threshold);
            tv_threshold.setVisibility(tv_threshold.GONE);
            sb_threshold = (SeekBar)rootView.findViewById(R.id.set_threshold);
            sb_threshold.setMax(100);
            sb_threshold.setProgress(40);
            sb_threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    threshold = progress;
                    tv_threshold.setText("current threshold: " + threshold);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            bar_content = (SeekBar)rootView.findViewById(R.id.bar_content);
            bar_content.setVisibility(bar_content.INVISIBLE);
            tmp = "";
            avr_count = 0;
            avr_value = 0;
            match_count = 1000;
            match_index = new int[blockSize /2];
            threshold = 40;
            tv_threshold.setText("current threshold: "+ 40);

            vb = (Vibrator)getActivity().getSystemService(getContext().VIBRATOR_SERVICE);
            return rootView;
        }
        private class RecordAudio extends AsyncTask<Void, double[], Void> {
            @Override
            protected Void doInBackground(Void... params) {
                try{
                    // AudioRecord를 설정하고 사용한다.
                    //InputStream in = getActivity().getResources().openRawResource(R.raw.siren);
                    //ObjectOutputStream output =  new ObjectOutputStream(new FileOutputStream("gilad-OutPut.bin"));


                    int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

                    AudioRecord audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

                    // short로 이뤄진 배열인 buffer는 원시 PCM 샘플을 AudioRecord 객체에서 받는다.
                    // double로 이뤄진 배열인 toTransform은 같은 데이터를 담지만 double 타입인데, FFT 클래스에서는 double타입이 필요해서이다.
                     short[] buffer = new short[blockSize];
                     double[] toTransform = new double[blockSize];
                    //double[] SirenResult = new double[blockSize];

                    audioRecord.startRecording();

                    while(started){
                        //int bufferReadResult = audioRecord.read(buffer, 0, blockSize);
                        int bufferReadResult = audioRecord.read(buffer, 0, blockSize);
                        // AudioRecord 객체에서 데이터를 읽은 다음에는 short 타입의 변수들을 double 타입으로 바꾸는 루프를 처리한다.
                        // 직접 타입 변환(casting)으로 이 작업을 처리할 수 없다. 값들이 전체 범위가 아니라 -1.0에서 1.0 사이라서 그렇다
                        // short를 32,768.0(Short.MAX_VALUE) 으로 나누면 double로 타입이 바뀌는데, 이 값이 short의 최대값이기 때문이다.
                        average = 0; count = blockSize;
                        for(int i = 0; i < blockSize && i < bufferReadResult; i++){
                            if(buffer[i] > 0){
                                average += Math.abs(buffer[i]);
                            }
                            else{
                               count--;
                            }
                            toTransform[i] = (double)buffer[i] / Short.MAX_VALUE; // 부호 있는 16비트
                        }
                        transformer.ft(toTransform);
                        double magnitude[] = new double[blockSize / 2];

                        for (int i = 0; i < magnitude.length; i++) {
                            double R = toTransform[2 * i] * toTransform[2 * i];
                            double I = toTransform[2 * i + 1]
                                      * toTransform[2 * i * 1];

                            magnitude[i] = Math.sqrt(I + R);
                        }

                        double max = magnitude[0];
                        for (int i = 1; i < magnitude.length; i++) {
                            if (magnitude[i] > max) {
                                max = magnitude[i];
                                maxIndex = i;
                            }
                        }
                        double[] data = new double[blockSize];
                        System.arraycopy(toTransform, 0, data, 0, blockSize);
                        // dominant frequency
                        hz = maxIndex * frequency / (double) blockSize;

                        double x = average / count;
                        if(x != 0)
                        {
                            decibel = 20 * Math.log10((x / 51805.5336) / 0.00002);
                            publishProgress(data);
                        }

                    }

                    audioRecord.stop();
                }catch(Throwable t){
                    Log.e("AudioRecord", "Recording Failed");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(double[]... toTransform) {
                bar_content.setLayoutParams(new LinearLayout.LayoutParams((int)decibel*10,bar_content.getLayoutParams().height));
                canvas.drawColor(Color.WHITE);
                for(int i = 0; i < toTransform[0].length; i++){
                    int x = i*4;
                    int downy = (int) (400 - (toTransform[0][i])*40);
                    int upy = 400;
                    if(i == maxIndex){
                        paint.setColor(Color.RED);
                    }
                    else{
                        paint.setColor(Color.BLUE);
                    }
                    canvas.drawLine(x, downy, x, upy, paint);
                    canvas.drawLine(x+1, downy, x+1, upy, paint);
                    canvas.drawLine(x+2, downy, x+2, upy, paint);
                    canvas.drawLine(x+3, downy, x+3, upy, paint);
                }
                //Log.d("threshold value:", "" + threshold);
                tv_current_db.setText("DB : "+(int)decibel);
                if(decibel > threshold && set == false)
                {
                    match_count = 100;
                    avr_count = 0;
                    avr_value = 0;
                    min_hz = 0;
                    max_hz = 0;
                    for(int i = 0; i < match_index.length; i++)
                    {
                        match_index[i] = 0;
                    }
                    set = true;
                    finish = false;
                    vb.vibrate(100);
                    //Toast.makeText(getContext(),"소리가 감지되었습니다..",Toast.LENGTH_SHORT).show();
                    //큰소리가 들어왔을 때 분석시작.
                }
                if(decibel > threshold && finish == false && set == true){
                    match_count--;
                    match_index[maxIndex] += 1;
                }
                if(finish == false && set == true && match_count == 0){
                    double temp;
                    double bw;
                    for(int i = 0; i < match_index.length; i++)
                    {
                        if(match_index[i] > 5) {
                            temp = i * frequency / (double) blockSize;
                            tmp = tmp.concat("Hz :  " + Double.valueOf(temp) + "hz\t");
                            tmp = tmp.concat("match :  " + match_index[i] + "개\n");
                            avr_count += match_index[i];
                            avr_value += temp * match_index[i];
                            if(min_hz == 0)
                                min_hz = temp;
                            max_hz = temp;
                        }
                    }
                    tmp = tmp.concat("----------------------------------------\n");
                    avr_value /= avr_count;
                    bw = max_hz - min_hz;
                    tmp = tmp.concat("min hz :  " + min_hz + "hz\t");
                    tmp = tmp.concat("max hz :  " + max_hz + "hz\n");
                    tmp = tmp.concat("bandWidth :  " + bw + "hz\n");
                    tmp = tmp.concat("avr hz :  " + avr_value + "hz\n");
                    tmp = tmp.concat("----------------------------------------\n");
                    tv_hz.setText(tmp);
                    if(avr_value > 800 && avr_value < 850 && bw < 300 && bw > 200){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"아기 울음 소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    if(avr_value > 900 && avr_value < 950 && bw < 190 && bw > 180){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"자동차 엔진소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    if(avr_value > 600 && avr_value < 780 && bw < 160 && bw > 155){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"초인종 소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    if(avr_value > 1100 && avr_value < 1600 && bw < 450 && bw > 200){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"사이렌 소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    if(avr_value > 900 && avr_value < 980 && bw < 450 && bw > 200){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"사이렌 소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    if(avr_value > 1000 && avr_value < 1200 && bw < 500 && bw > 300){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"자동차 경적 소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    if(avr_value > 100 && avr_value < 300 && bw < 200){
                        vb.vibrate(200);
                        Toast.makeText(getContext(),"남자 목소리입니다.",Toast.LENGTH_SHORT).show();
                    }
                    set = false;
                    finish = true;
                }
                imageView.invalidate();
            }
        }

        @Override
        public void onClick(View arg0) {
            if(started){
                started = false;
                //startStopButton.setText("Start");
                bar_db.setVisibility(bar_db.GONE);
                startStopButton.setBackground(this.getResources().getDrawable(R.drawable.bt_start));
                recordTask.cancel(true);
            }else{
                started = true;
                bar_content.setMax(0);
                bar_db.setVisibility(bar_db.VISIBLE);
                //startStopButton.setText("Stop");
                startStopButton.setBackground(this.getResources().getDrawable(R.drawable.bt_stop));
                recordTask = new RecordAudio();
                recordTask.execute();
            }
        }
    }
