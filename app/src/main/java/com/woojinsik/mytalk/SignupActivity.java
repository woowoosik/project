package com.woojinsik.mytalk;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.woojinsik.mytalk.model.UserModel;

public class SignupActivity extends AppCompatActivity {

    private static final int PICK_FROM_ALBUM = 1;
    private EditText email;
    private EditText name;
    private EditText password;
    private Button signup;
    private String spl_background;
    private ImageView profile;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 앱 최상단의 상태바 색 적용 ( 원격 )
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        spl_background = mFirebaseRemoteConfig.getString(getString(R.string.rc_color));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //롤리팝부터 적용가능
            // 앱 최상단의 상태바 배경 원격으로 바꾸기
            getWindow().setStatusBarColor(Color.parseColor(spl_background));
        }

        // 사진을 클릭시 앨범 오픈
        profile = (ImageView)findViewById(R.id.signupActivity_imageview_profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent,PICK_FROM_ALBUM);
            }
        });



        email = (EditText)findViewById(R.id.signupActivity_edittext_email);
        name = (EditText)findViewById(R.id.signupActivity_edittext_name);
        password = (EditText)findViewById(R.id.signupActivity_edittext_password);
        signup = (Button)findViewById(R.id.signupActivity_button_signup);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 값이 없는경우
                if(email.getText().length() == 0 || name.getText().length() == 0 || password.getText().length() == 0){
                    Toast.makeText(SignupActivity.this, "정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }else if(imageUri == null) {
                    // 이미지를 추가하지 않았을 경우 기본 이미지로
                    imageUri = Uri.parse("android.resource://" + getPackageName() +"/" + R.drawable.default_image);
                }

                // 이메일 주소와 비밀번호를 createUserWithEmailAndPassword에 전달하여 신규 계정을 생성
                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                         // 작업이 완료될 때 호출된다.
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                final String uid = task.getResult().getUser().getUid();

                                FirebaseStorage.getInstance().getReference().child("userImages").child(uid).putFile(imageUri)
                                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                        @SuppressWarnings("VisibleForTests")

                                        String imageUrl = task.getResult().getDownloadUrl().toString();




                                        //System.out.println("wwwwwwwwwwwwwwwwwwwwwwwwwwww"+imageUrl);
                                        UserModel userModel = new UserModel();
                                        userModel.userName = name.getText().toString();
                                        userModel.profileImageUrl = imageUrl;
                                        userModel.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();


                                        // 데이터베이스에 넣고, 성공하면 닫기
                                        FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(userModel)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override   // 회원가입 완료시 종료
                                            public void onSuccess(Void aVoid) {
                                                SignupActivity.this.finish();
                                            }
                                        });


                                        System.out.println("wwwwwwwwwwwwwwwwwwwwwwwwwwww"+ FirebaseDatabase.getInstance().getReference().child("users").child(uid));

                                    }
                                });
                            }
                        });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_FROM_ALBUM && resultCode ==RESULT_OK){
            profile.setImageURI(data.getData()); // 가운데 뷰를 바꿈
            imageUri = data.getData();// 이미지 경로 원본
        }
    }

}