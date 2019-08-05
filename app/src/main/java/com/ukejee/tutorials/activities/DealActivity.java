package com.ukejee.tutorials.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.ukejee.tutorials.utils.FirebaseUtil;
import com.ukejee.tutorials.R;
import com.ukejee.tutorials.TravelDeal;

public class DealActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT =42;

    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    ImageView imageView;
    TravelDeal deal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        FirebaseUtil.openFbReference("traveldeals");

        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtPrice = findViewById(R.id.txtPrice);
        imageView = findViewById(R.id.image);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if(deal == null){
            deal = new TravelDeal();
        }
        this.deal = deal;

        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent,
                        "Insert Picture"), PICTURE_RESULT);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_menu, menu);
        if(FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditText(true);
        }
        else{
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditText(false);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            final StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    //deal.setImageUrl(url);
                    Snackbar.make(findViewById(R.id.parent_layout),"Upload Successful",Snackbar.LENGTH_SHORT);
                    Toast.makeText(DealActivity.this,"Upload Successful", Toast.LENGTH_LONG).show();
                    Log.d("Upload Progree:","Worked");

                    String pictureName = taskSnapshot.getStorage().getPath();
                    deal.setImageName(pictureName);
                    Log.d("Image Name:", pictureName);
                }
            }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        String url = task.getResult().toString();
                        Log.d("On Completion:", "It Got Here");
                        deal.setImageUrl(url);
                        showImage(url);
                    }
                    else{
                        Log.d("On Completion:", "Task has failed");
                    }
                }
            });

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){

        switch(menuItem.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Log.d("TRAVEL", "SAVE SUCESSFUL");
                Toast.makeText(this,"Saved",Toast.LENGTH_SHORT);
                Snackbar.make(findViewById(R.id.parent_layout),"Deal Saved", Snackbar.LENGTH_LONG).show();
                clean();
                backToList();
                return true;

            case R.id.delete_menu:
                deleteDeal();
                Snackbar.make(findViewById(R.id.parent_layout), "Deal Deleted", Snackbar.LENGTH_LONG).show();
                backToList();
                return true;

            default:
                Log.d("TRAVEL", "SAVE FAILED");
                return super.onOptionsItemSelected(menuItem);

        }
    }

    private void clean() {

        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();
    }

    private void saveDeal() {

        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());

        if(deal.getId() == null){
            mDatabaseReference.push().setValue(deal);
        }
        else{
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }

    }

    private void  deleteDeal(){
        if(deal == null){
            Snackbar.make(findViewById(R.id.parent_layout), "Please save the deal before deleting", Snackbar.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();

        if(deal.getImageName() != null && !deal.getImageName().isEmpty()){

            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image","Successful");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());
                }
            });
        }
    }

    private void backToList(){

        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    private void enableEditText(boolean isEnabled){

        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
    }

    private void showImage(String url){

        if(url != null && !url.isEmpty()){

            Log.d("IMAGE URI:", url);
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;

            //Log.d("Image Uri Processing:", width));
            Picasso.get().load(url).resize(width, width * 2/3).centerCrop()
                    .into(imageView);

        }
    }
}