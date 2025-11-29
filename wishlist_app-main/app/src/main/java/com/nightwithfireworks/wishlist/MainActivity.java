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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private LinearLayout containerList;
    private MaterialCardView cardAdd;
    private static final String PREF_NAME = "wishlist_prefs";
    private static final String KEY_WISHLIST = "wishlist_data";
    private boolean isLoading = false;

    private Uri tempImageUri = null;
    private ImageView dialogImageView;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> detailActivityLauncher;

    private long pressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        containerList = findViewById(R.id.containerList);
        cardAdd = findViewById(R.id.cardAdd);

        setupImagePicker();
        setupDetailActivityLauncher();
        loadWishlistFromPrefs();

        cardAdd.setOnClickListener(v -> showAddDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pressedTime + 2000 > System.currentTimeMillis()) {
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.press_back_again), Toast.LENGTH_SHORT).show();
                    pressedTime = System.currentTimeMillis();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveWishlistToPrefs();
    }

    private void setupDetailActivityLauncher() {
        detailActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleDetailResult
        );
    }

    private void handleDetailResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            int index = data.getIntExtra("itemIndex", -1);

            if (index == -1 || index >= containerList.getChildCount() - 1) {
                return;
            }

            View card = containerList.getChildAt(index);
            boolean isDeleted = data.getBooleanExtra("isDeleted", false);

            if (isDeleted) {
                containerList.removeView(card);
                Toast.makeText(this, getString(R.string.item_deleted), Toast.LENGTH_SHORT).show();
            } else {
                double newSavedAmount = data.getDoubleExtra("savedAmount", 0.0);
                card.setTag(newSavedAmount);
                Toast.makeText(this, getString(R.string.progress_saved), Toast.LENGTH_SHORT).show();
            }

            saveWishlistToPrefs();
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        tempImageUri = result.getData().getData();
                        if (tempImageUri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        tempImageUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
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
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            imagePickerLauncher.launch(intent);
        });

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String price = etPrice.getText().toString();

            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(MainActivity.this, getString(R.string.please_fill), Toast.LENGTH_SHORT).show();
                return;
            }

            createNewCard(name, price, tempImageUri, 0.0);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createNewCard(String name, String price, Uri imageUri, double savedAmount) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View newCard = inflater.inflate(R.layout.item_card, containerList, false);

        TextView tvName = newCard.findViewById(R.id.tvItemName);
        TextView tvPrice = newCard.findViewById(R.id.tvItemPrice);
        ImageView imgItem = newCard.findViewById(R.id.imgItem);

        tvName.setText(name);
        tvPrice.setText("฿" + price);

        if (imageUri != null) {
            imgItem.setImageURI(imageUri);
            imgItem.setTag(imageUri.toString());
        } else {
            imgItem.setImageResource(android.R.drawable.ic_menu_gallery);
            imgItem.setTag("");
        }

        newCard.setTag(savedAmount);

        newCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            int index = containerList.indexOfChild(newCard);

            intent.putExtra("itemIndex", index);
            intent.putExtra("name", name);
            try {
                String priceStr = tvPrice.getText().toString().replace("฿", "");
                intent.putExtra("price", Double.parseDouble(priceStr));
            } catch (NumberFormatException e) {
                try {
                    intent.putExtra("price", Double.parseDouble(price));
                } catch (NumberFormatException ex) {
                    intent.putExtra("price", 0.0);
                }
            }
            intent.putExtra("savedAmount", (Double) newCard.getTag());

            if (imageUri != null)
                intent.putExtra("imageUri", imageUri.toString());

            detailActivityLauncher.launch(intent);
        });

        int index = containerList.indexOfChild(cardAdd);
        containerList.addView(newCard, index);

        if (!isLoading) {
            saveWishlistToPrefs();
            Toast.makeText(this, getString(R.string.wish_added), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveWishlistToPrefs() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < containerList.getChildCount(); i++) {
                View card = containerList.getChildAt(i);
                if (card.getId() == R.id.cardAdd) continue;

                TextView tvName = card.findViewById(R.id.tvItemName);
                TextView tvPrice = card.findViewById(R.id.tvItemPrice);
                ImageView imgItem = card.findViewById(R.id.imgItem);

                double savedAmount = (card.getTag() == null) ? 0.0 : (Double) card.getTag();

                JSONObject obj = new JSONObject();
                obj.put("name", tvName.getText().toString());
                obj.put("price", tvPrice.getText().toString().replace("฿", ""));
                obj.put("imageUri", imgItem.getTag() == null ? "" : imgItem.getTag().toString());
                obj.put("savedAmount", savedAmount);

                jsonArray.put(obj);
            }

            String jsonString = jsonArray.toString();
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_WISHLIST, jsonString)
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWishlistFromPrefs() {
        isLoading = true;
        String json = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getString(KEY_WISHLIST, null);

        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String name = obj.getString("name");
                        String price = obj.getString("price");
                        String imageUri = obj.getString("imageUri");

                        double savedAmount = obj.optDouble("savedAmount", 0.0);

                        Uri imgUri = imageUri.isEmpty() ? null : Uri.parse(imageUri);
                        createNewCard(name, price, imgUri, savedAmount);

                    } catch (Exception e_inner) {
                        e_inner.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        isLoading = false;
    }
}