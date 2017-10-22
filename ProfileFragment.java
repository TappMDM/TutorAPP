package com.example.singularity.tapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by singularity on 10/4/2017.
 */

public class ProfileFragment extends Fragment implements View.OnClickListener{

    TextView textViewUserName, textViewUniversity, textViewYear, textViewProgram;
    private ImageView mProfileImge;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef, databaseProfile, databaseUser;;
    private String UID;
    private String mName, mUniversity, mYear, mProgram, mImage, mSubjects;
    private Uri resultUri;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //Set Variable for profile fields
        View view= inflater.inflate(R.layout.activity_main,container,false);
        textViewUserName = (TextView) view.findViewById(R.id.textViewName);
        textViewUniversity = (TextView) view.findViewById(R.id.textViewUniversity);
        textViewYear = (TextView) view.findViewById(R.id.textViewYear);
        textViewProgram = (TextView) view.findViewById(R.id.textViewProgram);
        mProfileImge = (ImageView) view.findViewById(R.id.imageViewprofile);

        //Set Variable for button
        final TextView bUpdate = (TextView) view.findViewById(R.id.textViewEditProfile);


        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        FirebaseUser user = mAuth.getCurrentUser();
        UID = user.getUid();

        databaseProfile = FirebaseDatabase.getInstance().getReference("Profile").child(UID);
        databaseUser = FirebaseDatabase.getInstance().getReference("User").child(UID);

        //update data
        bUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUpdateDialog(UID);
            }
        });
        //end update data
        return view;
    }

    private void showUpdateDialog(final String Uid){
        //Initialize dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View dialogView = inflater.inflate(R.layout.profile_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Edit Profile");

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        //Set Variable to fields in the dialog
        final EditText dialogName = dialogView.findViewById(R.id.dialogName);
        final EditText dialogUniversity = dialogView.findViewById(R.id.dialogUniversity);
        final EditText dialogYear = dialogView.findViewById(R.id.dialogYear);
        final EditText dialogProgram = dialogView.findViewById(R.id.dialogProgram);
        final Button bUpdate = dialogView.findViewById(R.id.bUpdate);
        final RadioGroup dialogRadioGroup = dialogView.findViewById(R.id.radiogroup);

        //Import data from database to the dialog
        databaseUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("userName") !=null) {
                            mName = map.get("userName").toString();
                        dialogName.setText(mName);
                    }
                    if(map.get("university")!=null){
                        mUniversity = map.get("university").toString();
                        dialogUniversity.setText(mUniversity);
                    }
                    if(map.get("yearLevel")!=null){
                        mYear = map.get("yearLevel").toString();
                        dialogYear.setText(mYear);
                    }
                    if(map.get("program")!=null){
                        mProgram = map.get("program").toString();
                        dialogProgram.setText(mProgram);
                    }
                    if(map.get("profileImageUrl") != null){
                        mImage = map.get("profileImageUrl").toString();
                        String image = (String)dataSnapshot.child("profileImageUrl").getValue();
                        Picasso.with(getActivity()).load(image).into(mProfileImge);
                    }
                    if(map.get("subject") != null){
                        mSubjects = map.get("subject").toString();
                        dialogProgram.setText(mSubjects);
                        switch (mSubjects){
                            case "Math":
                                dialogRadioGroup.check(R.id.math);
                                break;
                            case "Science":
                                dialogRadioGroup.check(R.id.science);
                                break;
                            case "Engineering":
                                dialogRadioGroup.check(R.id.engineering);
                                break;
                        }
                    }
                }}

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //Load Photo to dialog
        mProfileImge = (ImageView) dialogView.findViewById(R.id.uploadImage);
        mProfileImge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);

            }
        });

        //Update data
        bUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mName = dialogName.getText().toString().trim();
                String university = dialogUniversity.getText().toString().trim();
                String yearLevel = dialogYear.getText().toString().trim();
                String program = dialogProgram.getText().toString().trim();

                int selectedId = dialogRadioGroup.getCheckedRadioButtonId();

                final RadioButton radioButton = (RadioButton) dialogView.findViewById(selectedId);

                if(radioButton.getText() == null){
                    return;
                }

                String mSubjects = radioButton.getText().toString();

                if(TextUtils.isEmpty(mName)){
                    dialogName.setError("Data required");
                    return;
                }
                if(TextUtils.isEmpty(university)){
                    dialogUniversity.setError("Data required");
                    return;
                }
                if(TextUtils.isEmpty(yearLevel)){
                    dialogYear.setError("Data required");
                    return;
                }
                if(TextUtils.isEmpty(program)){
                    dialogProgram.setError("Data required");
                    return;
                }


                //Update data
                Map userInfo = new HashMap();
                userInfo.put("userName", mName);
                userInfo.put("university", university);
                userInfo.put("yearLevel", yearLevel);
                userInfo.put("program", program);
                userInfo.put("subject", mSubjects);
                databaseUser.updateChildren(userInfo);
                databaseProfile.updateChildren(userInfo);
                //end

                if(resultUri != null){
                    StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(Uid);
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getActivity().getApplication().getContentResolver(), resultUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ByteArrayOutputStream boas = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas);
                    byte[] data = boas.toByteArray();

                    UploadTask uploadTask = filePath.putBytes(data);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            return;
                        }
                    });

                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri  downloadUrl = taskSnapshot.getDownloadUrl();

                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl", downloadUrl.toString());
                            databaseProfile.updateChildren(newImage);
                            databaseUser.updateChildren(newImage);
                            //finish();

                            return;
                        }

                    });
                }
                alertDialog.dismiss();
            }
        });
        //end
    }



    @Override
    public void onStart() {
        super.onStart();
        //Initialize data on the profile from the database
        databaseUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String name = (String)dataSnapshot.child("userName").getValue();
                String university = (String)dataSnapshot.child("university").getValue();
                String program = (String)dataSnapshot.child("program").getValue();
                String year = (String)dataSnapshot.child("yearLevel").getValue();
                String image = (String)dataSnapshot.child("profileImageUrl").getValue();

                textViewProgram.setText(program);
                textViewUniversity.setText(university);
                textViewYear.setText(year);
                textViewUserName.setText(name);
                Picasso.with(getActivity()).load(image).into(mProfileImge);
                //Glide.with(getActivity().getApplication()).load(image);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Responds to picture being imported from the device
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImge.setImageURI(resultUri);
        }
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("ProfileFragment");

        //Add User Information
    }

    @Override
    public void onClick(View view) {

    }
}
