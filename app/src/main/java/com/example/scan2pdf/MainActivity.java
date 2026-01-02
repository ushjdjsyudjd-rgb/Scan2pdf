package com.example.scan2pdf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PERMISSION_CODE = 100;
    private Bitmap imageBitmap;
    private ImageView imageView;
    private Button btnSavePdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        btnSavePdf = findViewById(R.id.btnSavePdf);
        Button btnCapture = findViewById(R.id.btnCapture);

        btnCapture.setOnClickListener(v -> {
            // چک کردن اجازه دوربین قبل از باز کردن
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        btnSavePdf.setOnClickListener(v -> createPdf());
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "اجازه دوربین داده نشد", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            btnSavePdf.setVisibility(View.VISIBLE);
        }
    }

    private void createPdf() {
        Document document = new Document();
        try {
            String fileName = "Scan_" + System.currentTimeMillis() + ".pdf";
            File filePath = new File(getExternalFilesDir(null), fileName);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            Image image = Image.getInstance(stream.toByteArray());
            
            float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                    - document.rightMargin()) / image.getWidth()) * 100;
            image.scalePercent(scaler);
            
            document.add(image);
            document.close();
            Toast.makeText(this, "PDF در پوشه Android/data ذخیره شد", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "خطا در ساخت فایل", Toast.LENGTH_SHORT).show();
        }
    }
}
