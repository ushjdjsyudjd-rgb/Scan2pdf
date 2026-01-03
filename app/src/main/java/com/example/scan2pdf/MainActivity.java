package com.example.scan2pdf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PDF_MERGE_PICK = 4;
    private static final int SCANNER_REQUEST_CODE = 1000;
    
    private List<Bitmap> imageList = new ArrayList<>();
    private ImageAdapter adapter;
    private ExtendedFloatingActionButton btnSavePdf;
    private GmsDocumentScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Scan2PDF");
        }

        // تنظیمات اسکنر گوگل
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build();
        scanner = GmsDocumentScanning.getClient(options);

        btnSavePdf = findViewById(R.id.btnSavePdf);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        
        MaterialCardView cardCapture = findViewById(R.id.cardCapture);
        MaterialCardView cardGallery = findViewById(R.id.cardGallery);
        MaterialCardView cardMergePdf = findViewById(R.id.cardMergePdf);
        MaterialCardView cardArchive = findViewById(R.id.cardArchive);

        adapter = new ImageAdapter(imageList, position -> {
            new AlertDialog.Builder(this)
                .setTitle("حذف")
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

        cardCapture.setOnClickListener(v -> startScanning());
        cardGallery.setOnClickListener(v -> startScanning());
        cardMergePdf.setOnClickListener(v -> openFilePicker("application/pdf", REQUEST_PDF_MERGE_PICK));
        cardArchive.setOnClickListener(v -> startActivity(new Intent(this, ArchiveActivity.class)));

        btnSavePdf.setOnClickListener(v -> showNamingDialog());
    }

    private void startScanning() {
        scanner.getStartScanIntent(this)
            .addOnSuccessListener(intentSender -> {
                try {
                    startIntentSenderForResult(intentSender, SCANNER_REQUEST_CODE, null, 0, 0, 0);
                } catch (Exception e) {
                    Toast.makeText(this, "خطا در اجرای اسکنر", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "خطای گوگل سرویس: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SCANNER_REQUEST_CODE) {
                GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);
                if (result != null && result.getPages() != null) {
                    for (GmsDocumentScanningResult.Page page : result.getPages()) {
                        handleUri(page.getImageUri());
                    }
                }
            } else if (requestCode == REQUEST_PDF_MERGE_PICK && data != null) {
                handlePdfMerge(data);
            }
        }
    }

    private void handleUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                Bitmap gray = applyGrayscale(bitmap);
                imageList.add(gray);
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    btnSavePdf.setVisibility(View.VISIBLE);
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطا در بارگذاری تصویر", Toast.LENGTH_SHORT).show();
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

    private void openFilePicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (requestCode == REQUEST_PDF_MERGE_PICK) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    private void showNamingDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter file name");
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2);
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);
        container.addView(input);
        new AlertDialog.Builder(this).setTitle("Save PDF").setView(container)
            .setPositiveButton("OK", (d, w) -> createPdf(input.getText().toString().trim()))
            .setNegativeButton("Cancel", null).show();
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
            Toast.makeText(this, "Saved to Scan2PDF folder", Toast.LENGTH_LONG).show();
            sharePdf(pdfFile);
            imageList.clear();
            adapter.notifyDataSetChanged();
            btnSavePdf.setVisibility(View.GONE);
        } catch (Exception e) {
            Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share File"));
        } catch (Exception e) { }
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
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    PdfReader reader = new PdfReader(is);
                    copy.addDocument(reader);
                    reader.close();
                }
            }
            document.close();
            Toast.makeText(this, "Merged successfully in Downloads/Scan2PDF", Toast.LENGTH_SHORT).show();
            sharePdf(mergedFile);
        } catch (Exception e) {
            Toast.makeText(this, "Error merging files", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "About");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            new AlertDialog.Builder(this).setTitle("Scan2PDF")
                .setMessage("Developer: Hamed Shabani Pour")
                .setPositiveButton("OK", null).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
