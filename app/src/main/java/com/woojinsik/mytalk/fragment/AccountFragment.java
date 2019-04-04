package com.woojinsik.mytalk.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.woojinsik.mytalk.LoginActivity;
import com.woojinsik.mytalk.MainActivity;
import com.woojinsik.mytalk.R;
import com.woojinsik.mytalk.SignupActivity;
import com.woojinsik.mytalk.model.UserModel;

import org.w3c.dom.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class AccountFragment extends Fragment{

    private FirebaseAuth firebaseAuth;

    private ImageView profile;
    private Uri imageUri;
    private TextView comment;


    private TextView name;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account,container,false); // xml 불러오기

        Button button = (Button) view.findViewById(R.id.accountFragment_button_comment);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(view.getContext());
            }
        });

        Button button_signout = (Button) view.findViewById(R.id.acountFragment_button_signout);
        button_signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.getInstance().signOut();
                getActivity().finish();

            }
        });


        comment = (TextView) view.findViewById(R.id.accountFragment_TextView_comment);


        name = (TextView) view.findViewById(R.id.accountFragment_TextView_name);
        profile = (ImageView) view.findViewById(R.id.accountFragment_imageview_profile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent,1);
            }
        });



        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(uid)
        .addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                    UserModel userModels = dataSnapshot.getValue(UserModel.class);

                    comment.setText(userModels.comment);
                    name.setText(userModels.userName);
                    imageUri=Uri.parse(userModels.profileImageUrl);
                    profile.setImageURI(Uri.parse(userModels.profileImageUrl));

                    Glide.with
                            (profile.getContext())
                            .load(imageUri) //주소
                            .apply(new RequestOptions().circleCrop()) // 어떻게 이미지를 줄건지
                            .into(profile); // 이미지 넣기
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        return view;
    }


    void showDialog(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.dialog_comment,null);
        final EditText editText = (EditText) view.findViewById(R.id.commentDialog_edittext); // 상태메시지를 받아옴

        // 빌더에 setView를 해준다., 확인에 대한 인터페이스를 해준다.
        builder.setView(view).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Map<String,Object> stringObjectMap = new HashMap<>();
                // uid 받아온다
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                // edit 으로 받아온걸 보내준다.
                stringObjectMap.put("comment",editText.getText().toString());
                // 서버에 comment를 만든다.
                FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(stringObjectMap);

            }

        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        imageUri = data.getData();// 이미지 경로 원본

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseStorage.getInstance().getReference().child("userImages").child(uid).putFile(imageUri)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        @SuppressWarnings("VisibleForTests")
                        String imageUrl = task.getResult().getDownloadUrl().toString();

                        Map<String,Object> profileImageMap = new HashMap<>();
                        // uid 받아온다
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        profileImageMap.put("profileImageUrl",imageUrl);

                       FirebaseDatabase.getInstance().getReference().child("users")
                               .child(uid).updateChildren(profileImageMap);

                    }
                });


    }





}
