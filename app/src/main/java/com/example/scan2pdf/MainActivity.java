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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_PICK = 2;
    private static final int REQUEST_WORD_PICK = 3;
    private static final int REQUEST_PDF_MERGE_PICK = 4;
    private static final int REQUEST_OCR_PICK = 5;
    private static final int PERMISSION_CODE = 100;
    
    private List<Bitmap> imageList = new ArrayList<>();
    private ImageAdapter adapter;
    private ExtendedFloatingActionButton btnSavePdf;
    private String currentPhotoPath; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("پی دی اف ساز حرفه‌ای");
        }

        btnSavePdf = findViewById(R.id.btnSavePdf);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        
        MaterialCardView cardCapture = findViewById(R.id.cardCapture);
        MaterialCardView cardGallery = findViewById(R.id.cardGallery);
        MaterialCardView cardWordToPdf = findViewById(R.id.cardWordToPdf);
        MaterialCardView cardMergePdf = findViewById(R.id.cardMergePdf);
        MaterialCardView cardArchive = findViewById(R.id.cardArchive);
        MaterialCardView cardOcr = findViewById(R.id.cardOcr); // دکمه جدید OCR

        adapter = new ImageAdapter(imageList, position -> {
            new AlertDialog.Builder(this)
                .setTitle("حذف صفحه")
                .setMessage("آیا این صفحه حذف شود؟")
                .setPositiveButton("بله", (d, w) -> {
                    imageList.remove(position);
                    adapter.notifyDataSetChanged();
                    if(imageList.isEmpty()) btnSavePdf.setVisibility(View.GONE);
                })
                .setNegativeButton("خیر", null).show();
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); 
        recyclerView.setAdapter(adapter);

        // Click Listeners
        if (cardCapture != null) cardCapture.setOnClickListener(v -> checkPermissionAndOpen(true));
        if (cardGallery != null) cardGallery.setOnClickListener(v -> openGallery());
        if (cardWordToPdf != null) cardWordToPdf.setOnClickListener(v -> openFilePicker("application/vnd.openxmlformats-officedocument.wordprocessingml.document", REQUEST_WORD_PICK));
        if (cardMergePdf != null) cardMergePdf.setOnClickListener(v -> openFilePicker("application/pdf", REQUEST_PDF_MERGE_PICK));
        if (cardArchive != null) cardArchive.setOnClickListener(v -> startActivity(new Intent(this, ArchiveActivity.class)));
        if (cardOcr != null) cardOcr.setOnClickListener(v -> openFilePicker("application/pdf", REQUEST_OCR_PICK));

        btnSavePdf.setOnClickListener(v -> showNamingDialog());
    }

    private void openFilePicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (requestCode == REQUEST_PDF_MERGE_PICK) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "انتخاب فایل"), requestCode);
    }

    private void checkPermissionAndOpen(boolean isCamera) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
            }, PERMISSION_CODE);
        } else {
            if (isCamera) openCamera();
            else openGallery();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try { photoFile = createImageFile(); } catch (IOException ignored) {}
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "انتخاب تصاویر"), REQUEST_GALLERY_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bitmap = decodeSampledBitmapFromFile(currentPhotoPath, 1024, 1024);
                if (bitmap != null) processBitmap(bitmap);
            } else if (requestCode == REQUEST_GALLERY_PICK && data != null) {
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) 
                        handleGalleryUri(data.getClipData().getItemAt(i).getUri());
                } else if (data.getData() != null) handleGalleryUri(data.getData());
            } else if (requestCode == REQUEST_WORD_PICK && data != null) {
                convertWordToPdf(data.getData());
            } else if (requestCode == REQUEST_PDF_MERGE_PICK && data != null) {
                handlePdfMerge(data);
            } else if (requestCode == REQUEST_OCR_PICK && data != null) {
                performOcrOnPdf(data.getData());
            }
        }
    }

    private void handlePdfMerge(Intent data) {
        List<Uri> pdfUris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) pdfUris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) pdfUris.add(data.getData());
        mergePdfFiles(pdfUris);
    }

    private void mergePdfFiles(List<Uri> uris) {
        try {
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
            if (!root.exists()) root.mkdirs();
            File mergedFile = new File(root, "Merged_" + System.currentTimeMillis() + ".pdf");
            Document document = new Document();
            PdfCopy copy = new PdfCopy(document, new FileOutputStream(mergedFile));
            document.open();
            for (Uri uri : uris) {
                InputStream is = getContentResolver().openInputStream(uri);
                PdfReader reader = new PdfReader(is);
                copy.addDocument(reader);
                reader.close();
                is.close();
            }
            document.close();
            Toast.makeText(this, "ادغام با موفقیت در پوشه دانلود انجام شد", Toast.LENGTH_SHORT).show();
            sharePdf(mergedFile);
        } catch (Exception e) {
            Toast.makeText(this, "خطا در ادغام فایل‌ها", Toast.LENGTH_SHORT).show();
        }
    }

    private void convertWordToPdf(Uri wordUri) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(wordUri);
                XWPFDocument doc = new XWPFDocument(is);
                File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
                if (!root.exists()) root.mkdirs();
                File pdfFile = new File(root, "Word_" + System.currentTimeMillis() + ".pdf");
                
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
                document.open();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    document.add(new Paragraph(p.getText()));
                }
                document.close();
                doc.close();
                is.close();
                runOnUiThread(() -> {
                    Toast.makeText(this, "تبدیل ورد انجام شد", Toast.LENGTH_SHORT).show();
                    sharePdf(pdfFile);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "خطا در پردازش ورد: احتمالاً فایل سنگین است", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void performOcrOnPdf(Uri pdfUri) {
        // فعلاً نمایش پیام در حال توسعه برای OCR
        new AlertDialog.Builder(this)
            .setTitle("قابلیت OCR")
            .setMessage("این قابلیت برای تشخیص متن به فایل‌های زبان نیاز دارد. آیا پردازش آزمایشی شروع شود؟")
            .setPositiveButton("بله", (d, w) -> Toast.makeText(this, "در حال تحلیل متن...", Toast.LENGTH_LONG).show())
            .setNegativeButton("لغو", null).show();
    }

    private void handleGalleryUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) processBitmap(bitmap);
            is.close();
        } catch (Exception e) {
            Toast.makeText(this, "خطا در بارگذاری تصویر", Toast.LENGTH_SHORT).show();
        }
    }

    private void processBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            Bitmap gray = applyGrayscale(bitmap);
            imageList.add(gray);
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.VISIBLE);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File image = File.createTempFile("IMG_" + timeStamp, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            final int halfHeight = options.outHeight / 2;
            final int halfWidth = options.outWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) inSampleSize *= 2;
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
        input.setHint("نام فایل را وارد کنید");
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2);
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);
        container.addView(input);
        new AlertDialog.Builder(this).setTitle("ذخیره PDF").setView(container)
            .setPositiveButton("تایید", (d, w) -> createPdf(input.getText().toString().trim()))
            .setNegativeButton("لغو", null).show();
    }

    private void createPdf(String fileName) {
        if (imageList.isEmpty()) return;
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        try {
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
            if (!root.exists()) root.mkdirs();
            String name = (fileName.isEmpty()) ? "Scan_" + System.currentTimeMillis() : fileName;
            File pdfFile = new File(root, name + ".pdf");
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();
            for (Bitmap bmp : imageList) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                Image image = Image.getInstance(stream.toByteArray());
                image.scaleToFit(PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 40);
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
                document.newPage();
            }
            document.close();
            Toast.makeText(this, "در پوشه دانلود ذخیره شد", Toast.LENGTH_LONG).show();
            sharePdf(pdfFile);
            imageList.clear();
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.GONE);
        } catch (Exception e) {
            Toast.makeText(this, "خطا در تولید فایل", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "اشتراک گذاری فایل"));
        } catch (Exception e) {
            Toast.makeText(this, "خطا در باز کردن فایل", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "درباره ما");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            new AlertDialog.Builder(this).setTitle("Scan2PDF")
                .setMessage("توسعه دهنده: حامد شعبانی پور")
                .setPositiveButton("تایید", null).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
