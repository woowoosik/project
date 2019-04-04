package com.woojinsik.mytalk.chat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.woojinsik.mytalk.R;

//import com.woojinsik.mytalk.model.ChatModel;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.FirebaseDatabase;
import com.woojinsik.mytalk.model.ChatModel;
import com.woojinsik.mytalk.model.NotificationModel;
import com.woojinsik.mytalk.model.UserModel;

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

public class MessageActivity extends AppCompatActivity {

    private String destinatonUid;
    private Button button;
    private EditText editText;

    private String uid;
    private String chatRoomUid;

    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);


        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();  //채팅을 요구 하는 아아디 즉 단말기에 로그인된 UID
        destinatonUid = getIntent().getStringExtra("destinationUid"); // 채팅을 당하는 아이디
        button = (Button) findViewById(R.id.messageActivity_button);
        editText = (EditText) findViewById(R.id.messageActivity_editText);
        recyclerView = (RecyclerView)findViewById(R.id.messageActivity_reclclerview);

        // 메시지 보내기
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid,true);
                chatModel.users.put(destinatonUid,true);

                if(chatRoomUid == null){ // 대화할 사람과 채팅방이 없는경우 ( 방을 만든다 )
                    button.setEnabled(false); // 너무 빠르게 클릭시 중복되는 것을 방지( 버튼을 잠시만 꺼놓는다. )
                    // push가 있어야  체팅방이 만들어지고 데이터 쌓인다
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel)
                            // 리스트로 넣어줘서 방을 체크한다.
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    checkChatRoom();
                                }
                    });
                }else {
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = editText.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP; //시간 보내기
                    FirebaseDatabase.getInstance().getReference().child("chatrooms")
                            .child(chatRoomUid).child("comments").push().setValue(comment)
                            // 한 번 보내기 완료하면 Text초기화
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            editText.setText("");
                        }
                    });
                }


            }
        });
        checkChatRoom();

    }


    // 중복을 체크
    void  checkChatRoom(){
        FirebaseDatabase.getInstance().getReference().child("chatrooms")
                // users에 값 중복체크
                .orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()){
                    ChatModel  chatModel = item.getValue(ChatModel.class);
                    // 요구한 사람의 id가 있으면
                    if(chatModel.users.containsKey(destinatonUid) && chatModel.users.size() == 2){ // 2명일 경우에만
                        chatRoomUid = item.getKey(); // 방 아이디
                        button.setEnabled(true); // 버튼을 다시 살려준다.
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        List<ChatModel.Comment> comments;
        public RecyclerViewAdapter() {
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(destinatonUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // userModel로 캐스팅
                    destinationUserModel = dataSnapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }


        void getMessageList(){
            // 데이터베이스로 comments 접촉, addValueEvent리스너로 읽어드림
            databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("chatrooms").child(chatRoomUid).child("comments");

            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            comments.clear(); // 대화내용 데이터 쌓이는거 제거
                            Map<String, Object> readUsersMap = new HashMap<>(); // 메시지 읽음 유무
                            for(DataSnapshot item : dataSnapshot.getChildren()){
                                String key = item.getKey();
                                ChatModel.Comment comment_origin = item.getValue(ChatModel.Comment.class);
                                // 서버와 클라이언트간의 읽고 수정이 무한과정이 반복되어 comment를 분리
                                ChatModel.Comment comment_motify = item.getValue(ChatModel.Comment.class);

                                comment_motify.readUsers.put(uid, true); // 읽은 유무 put
                                readUsersMap.put(key, comment_motify); // 읽은 내용 put
                                comments.add(comment_origin);
                            }

                            // comment에 리드유저가 없을경우, 있을경우는 그냥 읽기
                            if(comments.size() == 0){
                                return;
                            }else if (!comments.get(comments.size() - 1).readUsers.containsKey(uid)) { // 마지막 코멘트 리드유저에 내가 없니 없을경우 보고
                                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
                                        // 서버 업데이트 하고 했으면 반응하는 리스너
                                        .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        notifyDataSetChanged(); // 리스트 갱신
                                        // 맨 마지막 포지션으로 이동
                                        recyclerView.scrollToPosition(comments.size() - 1);
                                    }
                                });
                            }else { //메세지가 갱신
                                notifyDataSetChanged();
                                recyclerView.scrollToPosition(comments.size() - 1);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
            });
        }



        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // view를 넣어줌
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);

            // viewHolder 는 view 재사용
            return new MessageViewHolder(view);
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // 캐스팅('()'한거)으로 가져온다.
            MessageViewHolder messageViewHolder = ((MessageViewHolder)holder);

            // 내가 보낸 메시지
            if(comments.get(position).uid.equals(uid)){ // 앞의 uid는 내꺼 뒤에는 상대방 ( 같을경우 = 나인 경우 )
                messageViewHolder.textView_message.setText(comments.get(position).message); // 메시지 가져오기
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble); // 말풍선 가져오기
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE); // 내 사진, 이름 감추기
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT); // 오른쪽으로 고정
                setReadCounter(position,messageViewHolder.textView_readCounter_left); // 내가 보낸 경우 읽은 숫자가 왼쪽
                messageViewHolder.textView_readCounter_right.setVisibility(View.INVISIBLE);
            }else { // 상대방이 보낸 메시지
                Glide.with(holder.itemView.getContext()) // 상대방 이미지 가져오기
                        .load(destinationUserModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textview_name.setText(destinationUserModel.userName); // 상대방 이름
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

                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid)
                        .child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // <유저:true>를 해쉬로 받겟다.
                        Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                        peopleCount = users.size(); // size 는 전체 인원수 ( 인원수 넣어주기 )
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



        private class MessageViewHolder extends RecyclerView.ViewHolder {

            public TextView textView_message;
            public TextView textview_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public MessageViewHolder(View view) {
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
