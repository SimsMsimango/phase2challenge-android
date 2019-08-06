package com.google.travelmantics;

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
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String DEAL = "Deal";
    public static final String TRAVEL_DEALS = "traveldeals";
    public static final int PICTURE_RESULT = 42;

    private DatabaseReference mDatabaseReference;

    private EditText txtTitle;
    private EditText txtDescription;
    private EditText txtPrice;
    private Button btnImage;
    private ImageView imageView;
    private ProgressBar progressBar;

    private TravelDeal deal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        initializeViews();

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra(DEAL);
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        assignDealValues(deal);
        showImage(deal.getImageUrl());
    }

    private void assignDealValues(TravelDeal deal) {
        if (deal != null) {
            txtTitle.setText(deal.getTitle());
            txtDescription.setText(deal.getDescription());
            txtPrice.setText(deal.getPrice());
        }
    }

    private void initializeViews() {
        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtPrice = findViewById(R.id.txtPrice);
        btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(this);
        imageView = findViewById(R.id.productImage);
        progressBar = findViewById(R.id.pbLoadingImage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal saved", Toast.LENGTH_LONG).show();
                clean();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnImage) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null && data.getData().getLastPathSegment() != null) {
                Uri imageUri = data.getData();
                final StorageReference ref = FirebaseUtil.mStorageReference.child(imageUri.getLastPathSegment());
                final UploadTask uploadTask = ref.putFile(imageUri);

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUrl) {
                                String url = downloadUrl.toString();
                                String pictureName = taskSnapshot.getStorage().getPath();
                                deal.setImageUrl(url);
                                deal.setImageName(pictureName);
                                showImage(url);
                            }
                        });

                    }
                });
            }
        }
    }

    private void clean() {
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();
        imageView.setImageResource(android.R.color.transparent);
    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        if (deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference picRef = FirebaseUtil.mFirebaseStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image", "Image Successfully deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());
                }
            });
        }
    }

    private void backToList() {
        Intent listActivityIntent = new Intent(this, ListActivity.class);
        startActivity(listActivityIntent);
    }

    private void enableEditTexts(boolean isEnabled) {
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        btnImage.setClickable(isEnabled);
        if (!isEnabled) {
            btnImage.setBackgroundColor(getResources().getColor(R.color.colorGray));
        } else {
            btnImage.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }
    }

    private void showImage(String url) {
        if (url != null && !url.isEmpty()) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width, width)
                    .centerCrop()
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            imageView.setImageResource(R.drawable.ic_launcher_foreground);
                        }
                    });
        }
    }
}
