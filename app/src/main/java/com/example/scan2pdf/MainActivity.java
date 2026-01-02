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
    // شما به یک Adapter ساده برای RecyclerView نیاز دارید که در مرحله بعد می‌سازیم
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExtendedFloatingActionButton btnCapture = findViewById(R.id.btnCapture);
        FloatingActionButton btnSavePdf = findViewById(R.id.btnSavePdf);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

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
            findViewById(R.id.btnSavePdf).setVisibility(View.VISIBLE);
            Toast.makeText(this, "صفحه اضافه شد. تعداد کل: " + imageList.size(), Toast.LENGTH_SHORT).show();
            // اینجا باید لیست را رفرش کنید
        }
    }

    private void showNamingDialog() {
        // ایجاد یک دیالوگ ساده برای پرسیدن نام فایل
        final android.widget.EditText input = new android.widget.EditText(this);
        new AlertDialog.Builder(this)
            .setTitle("نام فایل PDF")
            .setView(input)
            .setPositiveButton("ذخیره", (dialog, which) -> createPdf(input.getText().toString()))
            .show();
    }

    private void createPdf(String fileName) {
        Document document = new Document();
        try {
            if (fileName.isEmpty()) fileName = "Scan_" + System.currentTimeMillis();
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName + ".pdf");
            
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            
            for (Bitmap bmp : imageList) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                Image image = Image.getInstance(stream.toByteArray());
                image.scaleToFit(document.getPageSize().getWidth() - 50, document.getPageSize().getHeight() - 50);
                document.add(image);
                document.newPage();
            }
            
            document.close();
            Toast.makeText(this, "PDF در پوشه Downloads ذخیره شد", Toast.LENGTH_LONG).show();
            imageList.clear();
            findViewById(R.id.btnSavePdf).setVisibility(View.GONE);
        } catch (Exception e) {
            Toast.makeText(this, "خطا: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
