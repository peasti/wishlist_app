package com.nightwithfireworks.wishlist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private LinearLayout containerList;
    private MaterialCardView cardAdd;

    private Uri tempImageUri = null;
    private ImageView dialogImageView;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        containerList = findViewById(R.id.containerList);
        cardAdd = findViewById(R.id.cardAdd);

        setupImagePicker();

        cardAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        tempImageUri = result.getData().getData();

                        if (dialogImageView != null) {
                            dialogImageView.setImageURI(tempImageUri);
                        }
                    }
                }
        );
    }

    private void showAddDialog() {
        tempImageUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_wish, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogImageView = dialogView.findViewById(R.id.imgPreview);
        TextView btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String price = etPrice.getText().toString();

            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill all info", Toast.LENGTH_SHORT).show();
                return;
            }

            createNewCard(name, price, tempImageUri);
            dialog.dismiss(); // ปิด Dialog
        });

        dialog.show();
    }

    private void createNewCard(String name, String price, Uri imageUri) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View newCard = inflater.inflate(R.layout.item_card, containerList, false);

        TextView tvName = newCard.findViewById(R.id.tvItemName);
        TextView tvPrice = newCard.findViewById(R.id.tvItemPrice);
        ImageView imgItem = newCard.findViewById(R.id.imgItem);

        tvName.setText(name);
        tvPrice.setText("฿" + price);

        if (imageUri != null) {
            imgItem.setImageURI(imageUri);
        } else {
            imgItem.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        int insertIndex = containerList.getChildCount() - 1;
        containerList.addView(newCard, insertIndex);

        Toast.makeText(this, "Wish Added!", Toast.LENGTH_SHORT).show();
    }
}