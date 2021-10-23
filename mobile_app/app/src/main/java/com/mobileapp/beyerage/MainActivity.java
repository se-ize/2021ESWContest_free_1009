package com.mobileapp.beyerage;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mobileapp.beyerage.shop.ShopService;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{
    private BackPressCloseHandler backPressCloseHandler;
    //컨트롤러
    private static final AppConfig appConfig = new AppConfig();
    //음성 서비스
    private static final ShopService shopService = appConfig.shopService();
    //TTS 변수 선언
    private TextToSpeech tts;
    //음성인식 결과를 담는 변수
    private String userVoice = "";
    //퍼미션 체크를 위한 변수
    private final int PERMISSION = 1;
    //STT를 사용할 intent 와 SpeechRecognizer 초기화
    private SpeechRecognizer sRecognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        backPressCloseHandler = new BackPressCloseHandler(this);

        //TTS 환경설정
        setTTS();

        Button startServiceButton = (Button) findViewById(R.id.startServiceButton);
        Button vocButton = (Button) findViewById(R.id.vocButton);

        /**
         * 메인서비스 시작
         */
        //버튼 클릭시 음료 안내 서비스 호출
        startServiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SubActivity.class);
            startActivity(intent);

            shopService.voiceGuidance(tts, "현재 위치를 파악중입니다.");
        });

        /**
         * VOC
         */
        vocButton.setOnClickListener(v -> {
            shopService.vocQuestion(tts); //일단 뭘 전송할지 물어보고
            new Handler().postDelayed(() -> {
                //음성인식 시작
                Intent intent = setSTTPermission();
                sRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                sRecognizer.setRecognitionListener(listener);
                sRecognizer.startListening(intent);
            }, 500);
            // 0.5초 딜레이 첨부


        });
    }

    private RecognitionListener listener = new RecognitionListener() {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(),"음성인식을 시작합니다.",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃 에러";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음 에러";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER BUSY 에러";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버에 문제";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류";
                    break;
            }
            //사용자에게 오류 안내
            if(error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
                shopService.voiceGuidance(tts, message + "가 발생하였습니다. 액세스 허용을 해주세요.");
            } else {
                shopService.voiceGuidance(tts, message + "가 발생하였습니다.");
            }

            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            userVoice = "";

            ArrayList<String> matches = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (String match : matches) {
                userVoice += match;
            }
            Log.i("태그","######################################"+matches);
            AlertDialog.Builder oDialog = new AlertDialog.Builder(MainActivity.this);

            //팝업창에 텍스트 셋팅
            oDialog.setMessage(userVoice)
                    .setTitle("고객의 소리")
                    .setPositiveButton("취소", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        { }
                    })
                    .setNeutralButton("전송", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            //전송
                            //3초 후 자동 전송은 없을까
                        }
                    })
                    .setCancelable(true)
                    .show();
        }
        @Override
        public void onPartialResults(Bundle partialResults) {}
        @Override
        public void onEvent(int eventType, Bundle params) {}
    };

    //음성인식 환경설정
    private Intent setSTTPermission() {
        //STT 퍼미션 체크
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }
        // STT intent 설정
        Intent intent = setIntentForVoiceRec();
        return intent;
    }
    private Intent setIntentForVoiceRec() {
        //사용자에게 음성을 요구하고 음성 인식기를 통해 전송하는 활동 시작
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        //음석을 번역할 언어 설정
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        return intent;
    }

    private void setTTS(){
        //TTS를 생성하고 OnInitListener로 초기화
        tts = new TextToSpeech(this, status -> {
            if(status == TextToSpeech.SUCCESS) {
                //언어 선택
                tts.setLanguage(Locale.KOREAN);
                shopService.defaultGuidance(tts);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to shutdown!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onBackPressed() { //'뒤로' 두번누르면 종료
        backPressCloseHandler.onBackPressed();
    }
}