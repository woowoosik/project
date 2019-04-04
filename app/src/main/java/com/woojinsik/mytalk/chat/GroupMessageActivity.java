package com.woojinsik.mytalk.chat;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.gson.Gson;
import com.woojinsik.mytalk.R;

import com.woojinsik.mytalk.model.ChatModel;
import com.woojinsik.mytalk.model.NotificationModel;
import com.woojinsik.mytalk.model.UserModel;

import com.google.firebase.database.DataSnapshot;

import com.google.firebase.database.DatabaseError;

import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMessageActivity extends AppCompatActivity {

    Map<String,UserModel> users = new HashMap<>(); // 다수의 유저를 담을 Map
    String destinationRoom;
    String uid;
    EditText editText;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
    //private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private RecyclerView recyclerView;

    List<ChatModel.Comment> comments = new ArrayList<>(); // ArrayList로 초기화

    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);

        destinationRoom = getIntent().getStringExtra("destinationRoom"); // 키값 가져오기
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        editText = (EditText)findViewById(R.id.groupMessageActivity_editText);

        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 유저들 넣기
                for(DataSnapshot item : dataSnapshot.getChildren()){
                    // key 와 value 값을 Map 에 저장 , userModel로 캐스팅해서 넣어준다.
                    users.put(item.getKey(),item.getValue(UserModel.class));
                }
                init();
                recyclerView = (RecyclerView) findViewById(R.id.groupMessageActivity_recyclerview);
                recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
                recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
            }

             @Override
             public void onCancelled(DatabaseError databaseError) {
             }
        });

    }


    void init(){
        Button button = (Button) findViewById(R.id.groupMessageActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel.Comment comment = new ChatModel.Comment();
                comment.uid = uid;
                comment.message = editText.getText().toString();
                comment.timestamp = ServerValue.TIMESTAMP;
                // 데이터베이스로 넘기기
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments")
                        .push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                                editText.setText("");

                    }
                });
            }
        });
    }


    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        // 어댑터가 실행되면 가장 먼저 실행
        public GroupMessageRecyclerViewAdapter(){
            getMessageList();
        }

        void getMessageList(){
            // 데이터베이스로 comments 접촉, addValueEvent리스너로 읽어드림
    /*        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
                    .addValueEventListener(new ValueEventListener() {*/
            databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments");

            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear(); // 대화내용 데이터 쌓이는거 제거
                    Map<String, Object> readUsersMap = new HashMap<>(); // 메시지 읽음 유무
                    for(DataSnapshot item : dataSnapshot.getChildren()){
                        String key = item.getKey();
                        ChatModel.Comment comment_origin = item.getValue(ChatModel.Comment.class);
                        ChatModel.Comment comment_motify = item.getValue(ChatModel.Comment.class);

                        comment_motify.readUsers.put(uid, true); // 읽은 유무 put
                        readUsersMap.put(key, comment_motify); // 읽은 내용 put
                        comments.add(comment_origin);

                    }

                    // comment에 리드유저가 없을경우, 있을경우는 그냥 읽기
                    if(comments.size() == 0){return;
                    }
                    else if (!comments.get(comments.size() - 1).readUsers.containsKey(uid)) {


                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("comments")
                                // 서버 업데이트 하고 했으면 반응하는 리스너
                                .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                notifyDataSetChanged(); // 리스트 갱신
                                // 맨 마지막 포지션으로 이동
                                recyclerView.scrollToPosition(comments.size() - 1);
                            }
                        });
                        //메세지가 갱신

                    }else {
                        notifyDataSetChanged();
                        recyclerView.scrollToPosition(comments.size() - 1);
                    }

                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);

            return new GroupMessageViewHodler(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            // 캐스팅('()'한거)으로 가져온다.
            GroupMessageViewHodler messageViewHolder = ((GroupMessageViewHodler) holder);

            // 내가 보낸 메시지
            if(comments.get(position).uid.equals(uid)){ // 앞의 uid는 내꺼 뒤에는 상대방 ( 같을경우 = 나인 경우 )

                messageViewHolder.textView_message.setText(comments.get(position).message); // 메시지 가져오기
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble); // 말풍선 가져오기
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE); // 내 사진, 이름 감추기
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT); // 오른쪽으로 고정
                setReadCounter(position,messageViewHolder.textView_readCounter_left); // 내가 보낸 경우 읽은 숫자가 왼쪽
                messageViewHolder.textView_readCounter_right.setVisibility(View.INVISIBLE);




                // 왼쪽 켜고 값 보여주기
            }else { // 상대방이 보낸 메시지
                Glide.with(holder.itemView.getContext()) // 상대방 이미지 가져오기
                        .load(users.get(comments.get(position).uid).profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textview_name.setText(users.get(comments.get(position).uid).userName); // 상대방 이름
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE); // 상대방 정보 보기
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble); // 말풍선 이미지
                messageViewHolder.textView_message.setText(comments.get(position).message); // 메시지
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT); // 왼쪽에 고정
                setReadCounter(position,messageViewHolder.textView_readCounter_right); // 상대방이 보낸 경우 읽은 숫자가 오른쪽
                messageViewHolder.textView_readCounter_left.setVisibility(View.INVISIBLE);


            }

            long unixTime = (long) comments.get(position).timestamp; // 시간 받아오기
            Date date = new Date(unixTime); // 시간을 저장
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // 지역선택
            String time = simpleDateFormat.format(date); // String에 넣어주기
            messageViewHolder.textView_timestamp.setText(time); // 시간을 text에 넣어준다.


        }

        // 총인원이 몇인지, 안읽은 사람이 몇 명인지 카운트
        void setReadCounter(final int position, final TextView textView){
            // 서버에 무리가 가는것을 방지하기위해 처음만 실행되게
            if (peopleCount == 0) {  // 인원수가 0 일경우

                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(destinationRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // <유저:true>를 해쉬로 받겟다.
                        Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                        peopleCount = users.size(); // size 는 전체 인원수
                        //  읽은 사람을 빼면 읽지 않은 사람
                        int count = peopleCount - comments.get(position).readUsers.size();

                        if (count > 0) {
                            textView.setText(String.valueOf(count));
                            textView.setVisibility(View.VISIBLE);
                        } else {
                            textView.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else{
                int count = peopleCount - comments.get(position).readUsers.size();
                if (count > 0) {
                    textView.setText(String.valueOf(count));
                    textView.setVisibility(View.VISIBLE);
                } else {
                    textView.setVisibility(View.INVISIBLE);
                }
            }
        }


        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class GroupMessageViewHodler extends RecyclerView.ViewHolder {

            public TextView textView_message;
            public TextView textview_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;


            public GroupMessageViewHodler(View view) {
                super(view);

                textView_message = (TextView) view.findViewById(R.id.messageItem_textView_message);
                textview_name = (TextView)view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = (ImageView)view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = (LinearLayout)view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main = (LinearLayout)view.findViewById(R.id.messageItem_linearlayout_main);
                textView_timestamp = (TextView)view.findViewById(R.id.messageItem_textview_timestamp);
                textView_readCounter_left = (TextView)view.findViewById(R.id.messageItem_textview_readCounter_left);
                textView_readCounter_right = (TextView)view.findViewById(R.id.messageItem_textview_readCounter_right);

            }
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        // 메시지가 없어도 꺼지는 에러를 방지
        if(valueEventListener != null){
            // 채팅방 나간뒤에 읽기가 유지되는걸 방지
            databaseReference.removeEventListener(valueEventListener);
        }
        finish();  // 없어지는 것도 자연스럽게 finish 아래 있어야 작동
        overridePendingTransition(R.anim.fromleft,R.anim.toright);
    }




}

