package com.example.scan2pdf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PERMISSION_CODE = 100;
    private List<Bitmap> imageList = new ArrayList<>();
    private ImageAdapter adapter;
    private FloatingActionButton btnSavePdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExtendedFloatingActionButton btnCapture = findViewById(R.id.btnCapture);
        btnSavePdf = findViewById(R.id.btnSavePdf);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        
        // تنظیمات لیست
        adapter = new ImageAdapter(imageList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        btnCapture.setOnClickListener(v -> checkPermissionAndOpen());
        btnSavePdf.setOnClickListener(v -> showNamingDialog());
    }

    private void checkPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            imageList.add(bitmap);
            adapter.notifyDataSetChanged(); // رفرش لیست
            btnSavePdf.setVisibility(View.VISIBLE);
        }
    }

    private void showNamingDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("نام فایل را وارد کنید");
        new AlertDialog.Builder(this)
            .setTitle("ذخیره PDF")
            .setMessage("یک نام برای فایل خود انتخاب کنید:")
            .setView(input)
            .setPositiveButton("تایید و ذخیره", (dialog, which) -> {
                String name = input.getText().toString();
                createPdf(name.isEmpty() ? "Scan_" + System.currentTimeMillis() : name);
            })
            .setNegativeButton("انصراف", null)
            .show();
    }

    private void createPdf(String fileName) {
        Document document = new Document();
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName + ".pdf");
            
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            
            for (Bitmap bmp : imageList) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                Image image = Image.getInstance(stream.toByteArray());
                // فیت کردن تصویر در صفحه PDF
                float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                        - document.rightMargin()) / image.getWidth()) * 100;
                image.scalePercent(scaler);
                document.add(image);
                document.newPage();
            }
            
            document.close();
            Toast.makeText(this, "فایل با موفقیت در Downloads ذخیره شد", Toast.LENGTH_LONG).show();
            
            // پاکسازی برای اسکن بعدی
            imageList.clear();
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.GONE);
            
        } catch (Exception e) {
            Toast.makeText(this, "خطا در ذخیره سازی", Toast.LENGTH_SHORT).show();
        }
    }
}
