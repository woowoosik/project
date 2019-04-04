package com.woojinsik.mytalk.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.woojinsik.mytalk.R;
import com.woojinsik.mytalk.chat.GroupMessageActivity;
import com.woojinsik.mytalk.chat.MessageActivity;
import com.woojinsik.mytalk.model.ChatModel;
import com.woojinsik.mytalk.model.UserModel;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendActivity extends AppCompatActivity {

    ChatModel chatModel = new ChatModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        // 리사이클이랑 어댑터 연결
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.selectFriendActivity_recyclerview);
        recyclerView.setAdapter(new SelectFriendRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Button button = (Button) findViewById(R.id.selectFriendActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                chatModel.users.put(myUid,true); // 자신도 단체 채팅방에 넣어준다.

                FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel);

            }
        });

    }

    class SelectFriendRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<UserModel> userModels;

        public SelectFriendRecyclerViewAdapter() {
            userModels = new ArrayList<>();

            //DB검색
            FirebaseDatabase.getInstance().getReference().child("users")
                    .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    userModels.clear(); //중복된 데이터 제거
                    // 내 uid가 있을경우 list에 안넣음
                    final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        // usermodel에 id가 내꺼일경우
                        if(userModel.uid.equals(myUid)){
                            continue;
                        }
                        // 데이터 가져와서 넣기
                        userModels.add(userModel);
                    }
                    notifyDataSetChanged(); //새로고침하고 추가
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friends_select,parent,false);

            return new CustomViewHolder(view);
        }



        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            Glide.with
                    (holder.itemView.getContext())
                    .load(userModels.get(position).profileImageUrl) //주소
                    .apply(new RequestOptions().circleCrop()) // 어떻게 이미지를 줄건지
                    .into(((CustomViewHolder)holder).imageView); // 이미지 넣기
            ((CustomViewHolder)holder).textView.setText(userModels.get(position).userName); // text 넣기

            holder.itemView.setOnClickListener(new View.OnClickListener() {

                @Override

                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), MessageActivity.class);
                    intent.putExtra("destinationUid",userModels.get(position).uid); // 상대방 uid 받아오기
                    ActivityOptions activityOptions = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {  // 젤리빈 이상부터 가능
                            // 들어오는 화면, 나가는 화면
                        activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright,R.anim.toleft);
                        startActivity(intent,activityOptions.toBundle()); // 옵션 넘기기
                    }
                }
            });
            // 상태메시지가 있으면 상태메시지를 보여준다.
            if(userModels.get(position).comment != null){
                ((CustomViewHolder) holder).textView_comment.setText(userModels.get(position).comment);
                ((CustomViewHolder) holder).textView_comment.setVisibility(View.VISIBLE);
            }
            ((CustomViewHolder) holder).checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b){  // b 값이 true일 경우 체크된 상태
                        // 체크된 사람 uid 넣어줌
                        chatModel.users.put(userModels.get(position).uid,true);
                    }else{ //체크 취소 상태
                        chatModel.users.remove(userModels.get(position));
                    }
                }
            });

        }


        @Override
        public int getItemCount() {
            return userModels.size();
        }



        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_comment;
            public CheckBox checkBox;

           // public LinearLayout textView_comment_linearlayout;

            public CustomViewHolder(View view) {
                super(view);
                imageView = (ImageView) view.findViewById(R.id.frienditem_imageview);
                textView = (TextView) view.findViewById(R.id.frienditem_textview);
                textView_comment = (TextView)view.findViewById(R.id.frienditem_textview_comment);
                checkBox = (CheckBox)view.findViewById(R.id.friendItem_checkbox);


            }
        }
    }

}
