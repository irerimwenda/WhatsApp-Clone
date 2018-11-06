package com.example.ireribrian.whatsapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;
    private String currentUserUID;
    private FirebaseAuth mAuth;

    private DatabaseReference rootRef;
    private static final int GalleryPick = 1;

    private StorageReference UserProfileImageRef;

    private ProgressDialog loadinBar;
    private Toolbar SettingsToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        InitializeFields();

        //userName.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();
        currentUserUID = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        
        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings();
            }
        });


        RetrieverUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GalleryPick);

            }
        });
    }


    private void InitializeFields() {
        updateAccountSettings = (Button) findViewById(R.id.update_settings_button);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView) findViewById(R.id.profile_image);

        loadinBar = new ProgressDialog(this);
        SettingsToolBar = (Toolbar)findViewById(R.id.app_settings_toolbar);
        setSupportActionBar(SettingsToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GalleryPick && resultCode == RESULT_OK && data != null){

            Uri ImageUri = data.getData();

            CropImage.activity(ImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(requestCode == RESULT_OK){
                Uri resultUri = result.getUri();

                loadinBar.setTitle("Profile image update");
                loadinBar.setMessage("Please wait while profile image updates..");
                loadinBar.setCanceledOnTouchOutside(false);
                loadinBar.show();

                final StorageReference filepath = UserProfileImageRef.child(currentUserUID + ".jpg");
                filepath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final Uri downloadUrl = uri;
                                Toast.makeText(SettingsActivity.this, "Profile Image Uploaded successfully", Toast.LENGTH_SHORT).show();

                                rootRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        rootRef.child("Users").child(currentUserUID).child("image")
                                                .setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if(task.isSuccessful()){
                                                    loadinBar.dismiss();
                                                    Toast.makeText(SettingsActivity.this, "Image save in db successfully", Toast.LENGTH_SHORT).show();
                                                }else
                                                {
                                                    loadinBar.dismiss();
                                                    String message = task.getException().toString();
                                                    Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        });
                    }
                });
            }

        }
    }

    private void updateSettings() {

        String settUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();

        if(TextUtils.isEmpty(settUserName)){
            Toast.makeText(this, "Please write username", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setStatus)){
            Toast.makeText(this, "Please write your status", Toast.LENGTH_SHORT).show();
        }else{
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserUID);
            profileMap.put("name", settUserName);
            profileMap.put("status", setStatus);

            rootRef.child("Users").child(currentUserUID).updateChildren(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()){
                                loadinBar.dismiss();
                                Toast.makeText(SettingsActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                sendUserToMainActivity();
                            }
                            else{
                                loadinBar.dismiss();
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }


    private void RetrieverUserInfo() {

        rootRef.child("Users").child(currentUserUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("image")))){

                            String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString();
                            String retrieveProfileImage = dataSnapshot.child("image").getValue().toString();

                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveStatus);
                            Picasso.with(SettingsActivity.this).load(retrieveProfileImage).into(userProfileImage);



                        }else if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))){

                        String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                        String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                        userName.setText(retrieveUserName);
                        userStatus.setText(retrieveStatus);

                        }
                        else{
                            //userName.setVisibility(View.VISIBLE);
                            Toast.makeText(SettingsActivity.this, "Profile set and update your profile info", Toast.LENGTH_SHORT).show();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }



    private void sendUserToMainActivity(){
    Intent mainIntent  = new Intent(SettingsActivity.this, MainActivity.class);
    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    finish();
    startActivity(mainIntent);
}

}
