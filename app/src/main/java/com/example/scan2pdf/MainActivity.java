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
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_CODE = 100;
    private List<Bitmap> imageList = new ArrayList<>();
    private ImageAdapter adapter;
    private ExtendedFloatingActionButton btnSavePdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // تنظیم نوار ابزار (Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnSavePdf = findViewById(R.id.btnSavePdf);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        ExtendedFloatingActionButton btnCapture = findViewById(R.id.btnCapture);

        // آداپتور با قابلیت حذف در صورت کلیک روی آیتم
        adapter = new ImageAdapter(imageList, position -> {
            new AlertDialog.Builder(this)
                .setTitle("حذف عکس")
                .setMessage("آیا این صفحه حذف شود؟")
                .setPositiveButton("بله", (d, w) -> {
                    imageList.remove(position);
                    adapter.notifyDataSetChanged();
                    if(imageList.isEmpty()) btnSavePdf.setVisibility(View.GONE);
                })
                .setNegativeButton("خیر", null).show();
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        btnCapture.setOnClickListener(v -> checkPermissionAndOpen());
        btnSavePdf.setOnClickListener(v -> showNamingDialog());
    }

    // ایجاد منوی سه نقطه
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
            case 2: Toast.makeText(this, "بخش اسکن‌های قبلی در حال توسعه است", Toast.LENGTH_SHORT).show(); break;
            case 3: showAboutDialog(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("درباره برنامه")
            .setMessage("ساخته شده توسط حامد شعبانی پور\nDevelop by Hamed@\nushjdjsyudjd@gmail.com")
            .setPositiveButton("بستن", null).show();
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
            Bitmap grayBitmap = applyGrayscale(bitmap);
            imageList.add(grayBitmap);
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap applyGrayscale(Bitmap bmp) {
        Bitmap result = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmp, 0, 0, paint);
        return result;
    }

    private void showNamingDialog() {
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        input.setHint("نام فایل (مثال: report)");

        new AlertDialog.Builder(this)
            .setTitle("تبدیل به PDF")
            .setMessage("نام فایل نهایی را وارد کنید:")
            .setView(input)
            .setPositiveButton("ذخیره و اشتراک", (dialog, which) -> {
                createPdf(input.getText().toString());
            })
            .setNegativeButton("انصراف", null).show();
    }

    private void createPdf(String fileName) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        try {
            // ساخت مسیر Downloads/Scan2PDF/تاریخ_ساعت
            String timeDir = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
            File subFolder = new File(root, timeDir);
            if (!subFolder.exists()) subFolder.mkdirs();

            String name = (fileName.isEmpty()) ? "Scan_" + timeDir : fileName;
            File pdfFile = new File(subFolder, name + ".pdf");
            
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            for (Bitmap bmp : imageList) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                Image image = Image.getInstance(stream.toByteArray());
                // فیت کردن تصویر در سایز A4
                image.scaleToFit(PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 40);
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
                document.newPage();
            }
            document.close();
            Toast.makeText(this, "ذخیره شد در پوشه Scan2PDF", Toast.LENGTH_LONG).show();
            sharePdf(pdfFile);
            
            // ریست کردن برنامه برای اسکن بعدی
            imageList.clear();
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.GONE);
            
        } catch (Exception e) {
            Toast.makeText(this, "خطا در تولید PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "ارسال فایل با:"));
    }
}
