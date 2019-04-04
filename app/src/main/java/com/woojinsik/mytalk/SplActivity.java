package com.woojinsik.mytalk;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class SplActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spl);

        // 최상단의 상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        linearLayout = (LinearLayout)findViewById(R.id.mainActivity_linearLayout);
        // 원격 구성 개체 인스턴스를 가져오고 캐시를 빈번하게 새로고칠 수 있도록 개발자 모드를 설정
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        // 디폴트 값
        mFirebaseRemoteConfig.setDefaults(R.xml.default_config);

        // 서버에 값이 있을때 값을 덮어씌운다.
        mFirebaseRemoteConfig.fetch(0)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                        }
                        displayMessage();
                    }
                });

    }


    void displayMessage(){
        String spl_background = mFirebaseRemoteConfig.getString("spl_background");
        boolean caps = mFirebaseRemoteConfig.getBoolean("spl_message_caps");
        String spl_message = mFirebaseRemoteConfig.getString("spl_message");

        linearLayout.setBackgroundColor(Color.parseColor(spl_background));

        if(caps){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(spl_message).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });

            builder.create().show();
        }else {
            // 로그인 페이지로 이동
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }

    }



}
