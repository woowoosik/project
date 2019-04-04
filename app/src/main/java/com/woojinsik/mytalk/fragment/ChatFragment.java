package com.woojinsik.mytalk.fragment;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;


public class ChatFragment extends Fragment {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm"); // 알아볼 수 있게 바꾼다.

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat,container,false);

        RecyclerView recyclerView  = (RecyclerView) view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));

        return view;
    }


    class ChatRecyclerViewAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private List<ChatModel> chatModels = new ArrayList<>();
        private List<String> keys = new ArrayList<>();
        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();

        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();  // uid 가져오기

            // 채팅방중에서 내가 속한 방만
            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid)
                    .equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    chatModels.clear(); // 중복될 경우를 대비하여 제거
                    for (DataSnapshot item :dataSnapshot.getChildren()){
                        chatModels.add(item.getValue(ChatModel.class));
                        keys.add(item.getKey()); // 단체방 키
                    }
                    notifyDataSetChanged();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

        }



        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat,parent,false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            final CustomViewHolder customViewHolder = (CustomViewHolder)holder;
            String destinationUid = null;

            // 일일 챗방에 있는 유저를 체크
            for(String user: chatModels.get(position).users.keySet()){
                if(!user.equals(uid)){ // 내가 아닌 다른 사람 가져오기
                    destinationUid = user;
                    destinationUsers.add(destinationUid); // 대화하는 사람 추가
                }
            }

            // 상대방이 누군지 데이터를 가져온다.
            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                    UserModel userModel =  dataSnapshot.getValue(UserModel.class);
                    // 이미지 뷰에 이미지 넣기
                    Glide.with(customViewHolder.itemView.getContext())
                            .load(userModel.profileImageUrl)
                            .apply(new RequestOptions().circleCrop())
                            .into(customViewHolder.imageView);
                    customViewHolder.textView_title.setText(userModel.userName); // 타이틀을 상대방 이름으로
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            // 메시지를 내림 차순으로 정렬 후 마지막 메세지의 키값을 가져옴
            Map<String,ChatModel.Comment> commentMap = new TreeMap<>(Collections.reverseOrder());
            commentMap.putAll(chatModels.get(position).comments);
            if(commentMap.keySet().toArray().length > 0) { // 메시지가 있을때만 읽어오도록
                String lastMessageKey = (String) commentMap.keySet().toArray()[0];
                // 마지막 채팅을 넣어준다.
                customViewHolder.textView_last_message.setText(chatModels.get(position)
                        .comments.get(lastMessageKey).message);

                //TimeStamp
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // 지역설정
                long unixTime = (long) chatModels.get(position).comments.get(lastMessageKey)
                        .timestamp; // 마지막 말 시간 가져오기
                Date date = new Date(unixTime); // Date 에 넣어서 보내주기
                customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date));
            }

            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = null;
                    // 2명이면 개인으로 3명이상이면 단체 채팅방으로
                    if(chatModels.get(position).users.size() > 2){
                        // 누르면 단체 채팅방으로 이동
                        intent = new Intent(view.getContext(), GroupMessageActivity.class);
                        intent.putExtra("destinationRoom",keys.get(position)); // 단체방 키
                    }else{
                        // 누르면 채팅방으로 이동
                        intent = new Intent(view.getContext(), MessageActivity.class);
                        // 누구랑 대화하는지를 넘겨준다
                        intent.putExtra("destinationUid", destinationUsers.get(position));
                    }

                    // 화면이 밀리면서 이동하기
                    ActivityOptions activityOptions = null;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright, R.anim.toleft);
                        startActivity(intent, activityOptions.toBundle());
                    }

                }
            });



        }



        @Override
        public int getItemCount() {
            return chatModels.size();
        }



        private class CustomViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timestamp;

            public CustomViewHolder(View view) {
                super(view);

                imageView = (ImageView) view.findViewById(R.id.chatitem_imageview);
                textView_title = (TextView)view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = (TextView)view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = (TextView)view.findViewById(R.id.chatitem_textview_timestamp);

            }
        }
    }





}