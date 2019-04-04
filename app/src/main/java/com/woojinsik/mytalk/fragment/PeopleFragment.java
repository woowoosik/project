package com.woojinsik.mytalk.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.woojinsik.mytalk.chat.MessageActivity;
import com.woojinsik.mytalk.model.UserModel;


import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);
        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new PeopleFragmentRecyclerViewAdapter());

        FloatingActionButton floatingActionButton = (FloatingActionButton)view.findViewById(R.id.peoplefragment_floatingButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(),SelectFriendActivity.class));
            }
        });

        return view;
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<UserModel> userModels;

       public PeopleFragmentRecyclerViewAdapter() {

            userModels = new ArrayList<>();
            //DB검색
            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                     userModels.clear(); //중복된 데이터 제거
                    final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    // 내 uid가 있을경우 list에 안넣음
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friends,parent,false);

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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) { // 젤리빈 이상부터 가능
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
        }


        @Override
        public int getItemCount() {
            return userModels.size();
        }



        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_comment;

           // public LinearLayout textView_comment_linearlayout;

            public CustomViewHolder(View view) {
                super(view);
                imageView = (ImageView) view.findViewById(R.id.frienditem_imageview);
                textView = (TextView) view.findViewById(R.id.frienditem_textview);
                textView_comment = (TextView)view.findViewById(R.id.frienditem_textview_comment);

            }
        }
    }


}
