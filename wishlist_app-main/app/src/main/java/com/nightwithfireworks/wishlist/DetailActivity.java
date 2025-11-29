package com.nightwithfireworks.wishlist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback; // อย่าลืม Import บรรทัดนี้
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    private TextView tvName, tvOriginalPrice, tvSavedAmount, tvRemainingAmount;
    private ImageView imgDetail, btnBack;
    private Button btnAddDeposit, btnDelete;

    private int itemIndex;
    private double originalPrice;
    private double currentSavedAmount;
    private boolean isDeleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvName = findViewById(R.id.detailName);
        tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        tvSavedAmount = findViewById(R.id.tvSavedAmount);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);
        imgDetail = findViewById(R.id.detailImage);
        btnBack = findViewById(R.id.btnBack);
        btnAddDeposit = findViewById(R.id.btnAddDeposit);
        btnDelete = findViewById(R.id.btnDelete);

        Intent intent = getIntent();
        itemIndex = intent.getIntExtra("itemIndex", -1);
        String name = intent.getStringExtra("name");

        originalPrice = intent.getDoubleExtra("price", 0.0);
        currentSavedAmount = intent.getDoubleExtra("savedAmount", 0.0);

        if (itemIndex == -1) {
            Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName.setText(name);
        updateFinancialUI();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("itemIndex", itemIndex);
                resultIntent.putExtra("isDeleted", isDeleted);
                resultIntent.putExtra("savedAmount", currentSavedAmount);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnAddDeposit.setOnClickListener(v -> showAddDepositDialog());

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void updateFinancialUI() {
        double remaining = Math.max(0, originalPrice - currentSavedAmount);

        tvOriginalPrice.setText(getString(R.string.price_label) + String.format("%.2f", originalPrice));
        tvSavedAmount.setText(getString(R.string.saved_label) + String.format("%.2f", currentSavedAmount));
        tvRemainingAmount.setText(getString(R.string.remaining_label) + String.format("%.2f", remaining));

        double percentage = 0;
        if (originalPrice > 0) {
            percentage = (Math.min(currentSavedAmount, originalPrice) / originalPrice) * 100;
        } else if (currentSavedAmount > 0) {
            percentage = 100;
        }

        if (percentage >= 100) {
            imgDetail.setImageResource(R.drawable.forgy_state3);
        } else if (percentage >= 31) {
            imgDetail.setImageResource(R.drawable.forgy_state2);
        } else {
            imgDetail.setImageResource(R.drawable.forgy_state1);
        }

        if (percentage >= 100) {
            btnAddDeposit.setEnabled(false);
            btnAddDeposit.setText(getString(R.string.completed));
        } else {
            btnAddDeposit.setEnabled(true);
            btnAddDeposit.setText(getString(R.string.btn_water));
        }
    }

    private void showAddDepositDialog() {
        if (!btnAddDeposit.isEnabled()) {
            Toast.makeText(this, getString(R.string.completed), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_deposit_title));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(getString(R.string.hint_amount));
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.btn_confirm_deposit), (dialog, which) -> {
            try {
                double deposit = Double.parseDouble(input.getText().toString());
                double remaining = Math.max(0, originalPrice - currentSavedAmount);

                if (deposit <= 0) {
                    Toast.makeText(this, getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show();
                } else if (deposit > remaining && remaining > 0) {
                    String msg = getString(R.string.error_amount_exceed) + String.format(" ฿%.2f", remaining);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                } else {
                    currentSavedAmount += deposit;
                    updateFinancialUI();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_msg))
                .setPositiveButton(getString(R.string.btn_confirm_delete), (dialog, which) -> {
                    isDeleted = true;
                    getOnBackPressedDispatcher().onBackPressed();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

}