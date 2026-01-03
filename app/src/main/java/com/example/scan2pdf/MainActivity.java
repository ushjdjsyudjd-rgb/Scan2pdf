package com.example.scan2pdf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.FrameLayout;
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
import java.io.IOException;
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
    private String currentPhotoPath; // ذخیره مسیر عکس اصلی

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
            new AlertDialog.Builder(this)
                .setTitle("حذف صفحه")
                .setMessage("آیا این صفحه از اسکن حذف شود؟")
                .setPositiveButton("بله، حذف شود", (d, w) -> {
                    imageList.remove(position);
                    adapter.notifyDataSetChanged();
                    if(imageList.isEmpty()) btnSavePdf.setVisibility(View.GONE);
                })
                .setNegativeButton("انصراف", null).show();
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        btnCapture.setOnClickListener(v -> checkPermissionAndOpen());
        btnSavePdf.setOnClickListener(v -> showNamingDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "اسکن فایل جدید").setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, 2, 0, "اسکن‌های قبلی").setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(0, 3, 0, "درباره برنامه").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 1) {
            checkPermissionAndOpen();
        } else if (id == 2) {
            Toast.makeText(this, "آرشیو در نسخه جدید اضافه می‌شود", Toast.LENGTH_SHORT).show();
        } else if (id == 3) {
            showAboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("درباره توسعه‌دهنده")
            .setMessage("ساخته شده توسط حامد شعبانی پور\n\nDevelop by Hamed@\nEmail: ushjdjsyudjd@gmail.com")
            .setPositiveButton("متوجه شدم", null).show();
    }

    private void checkPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "خطا در ایجاد فایل", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.scan2pdf.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // خواندن عکس از مسیر ذخیره شده با کیفیت اصلی
            Bitmap bitmap = decodeSampledBitmapFromFile(currentPhotoPath, 1024, 1024);
            if (bitmap != null) {
                Bitmap grayBitmap = applyGrayscale(bitmap);
                imageList.add(grayBitmap);
                adapter.notifyDataSetChanged();
                btnSavePdf.setVisibility(View.VISIBLE);
                
                // پاک کردن فایل موقت برای اشغال نشدن حافظه
                new File(currentPhotoPath).delete();
            }
        }
    }

    // متد بهینه‌سازی برای جلوگیری از کرش و حفظ کیفیت
    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
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
        input.setHint("مثلاً: صورتجلسه_اداری");
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("نام‌گذاری فایل PDF")
            .setView(container)
            .setPositiveButton("تایید و اشتراک", (dialog, which) -> {
                createPdf(input.getText().toString().trim());
            })
            .setNegativeButton("لغو", null).show();
    }

    private void createPdf(String fileName) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        try {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
            if (!root.exists()) root.mkdirs();
            
            File subFolder = new File(root, timeStamp);
            if (!subFolder.exists()) subFolder.mkdirs();

            String name = (fileName.isEmpty()) ? "Scan_" + timeStamp : fileName;
            File pdfFile = new File(subFolder, name + ".pdf");
            
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            for (Bitmap bmp : imageList) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                // استفاده از کیفیت 100 برای خروجی PDF
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                Image image = Image.getInstance(stream.toByteArray());
                image.scaleToFit(PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 40);
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
                document.newPage();
            }
            document.close();
            
            Toast.makeText(this, "فایل با کیفیت بالا ساخته شد", Toast.LENGTH_LONG).show();
            sharePdf(pdfFile);
            
            imageList.clear();
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.GONE);
            
        } catch (Exception e) {
            Toast.makeText(this, "خطا: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, "com.example.scan2pdf.provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "ارسال فایل..."));
        } catch (Exception e) {
            Toast.makeText(this, "خطا در اشت
