package com.example.scan2pdf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PERMISSION_CODE = 100;
    private List<Bitmap> imageList = new ArrayList<>();
    private ImageAdapter adapter;
    private ExtendedFloatingActionButton btnSavePdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnSavePdf = findViewById(R.id.btnSavePdf);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        ExtendedFloatingActionButton btnCapture = findViewById(R.id.btnCapture);

        adapter = new ImageAdapter(imageList, position -> {
            imageList.remove(position);
            adapter.notifyDataSetChanged();
            if(imageList.isEmpty()) btnSavePdf.setVisibility(View.GONE);
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        btnCapture.setOnClickListener(v -> checkPermissionAndOpen());
        btnSavePdf.setOnClickListener(v -> showNamingDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "اسکن فایل جدید");
        menu.add(0, 2, 0, "اسکن‌های قبلی");
        menu.add(0, 3, 0, "درباره برنامه");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: checkPermissionAndOpen(); break;
            case 2: Toast.makeText(this, "بخش آرشیو در آپدیت بعد", Toast.LENGTH_SHORT).show(); break;
            case 3: showAboutDialog(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("درباره برنامه")
            .setMessage("ساخته شده توسط حامد شعبانی پور\nDevelop by Hamed@\nushjdjsyudjd@gmail.com")
            .setPositiveButton("باشه", null)
            .show();
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
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            // اعمال فیلتر سیاه و سفید (Grayscale)
            Bitmap grayBitmap = toGrayscale(bitmap);
            imageList.add(grayBitmap);
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.VISIBLE);
        }
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private void showNamingDialog() {
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setHint("نام فایل را وارد کنید");

        new AlertDialog.Builder(this)
            .setTitle("ذخیره نهایی PDF")
            .setView(input)
            .setPositiveButton("ذخیره و اشتراک", (dialog, which) -> createPdf(input.getText().toString()))
            .setNegativeButton("لغو", null)
            .show();
    }

    private void createPdf(String fileName) {
        Document document = new Document(PageSize.A4); // تنظیم سایز A4
        try {
            // ایجاد مسیر: Downloads/Scan2PDF/yyyy-MM-dd_HH-mm
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
            File subFolder = new File(root, timeStamp);
            if (!subFolder.exists()) subFolder.mkdirs();

            File file = new File(subFolder, (fileName.isEmpty() ? "Scan" : fileName) + ".pdf");
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            for (Bitmap bmp : imageList) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                Image image = Image.getInstance(stream.toByteArray());
                image.scaleToFit(PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 40);
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
                document.newPage();
            }
            document.close();
            Toast.makeText(this, "ذخیره شد در: " + subFolder.getName(), Toast.LENGTH_LONG).show();
            shareFile(file);
        } catch (Exception e) {
            Toast.makeText(this, "خطا: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "اشتراک گذاری PDF در تلگرام/واتس‌اپ"));
    }
}
