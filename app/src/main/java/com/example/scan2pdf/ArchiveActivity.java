package com.example.scan2pdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ArchiveActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<File> pdfFiles = new ArrayList<>();
    private ArchiveAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        Toolbar toolbar = findViewById(R.id.toolbarArchive);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("اسکن‌های قبلی");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerViewArchive);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFiles();
    }

    private void loadFiles() {
        pdfFiles.clear();
        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
        if (root.exists()) {
            findPdfs(root);
            adapter = new ArchiveAdapter(pdfFiles);
            recyclerView.setAdapter(adapter);
        }
        if (pdfFiles.isEmpty()) {
            Toast.makeText(this, "هیچ فایلی یافت نشد", Toast.LENGTH_SHORT).show();
        }
    }

    private void findPdfs(File dir) {
        File[] list = dir.listFiles();
        if (list != null) {
            for (File file : list) {
                if (file.isDirectory()) findPdfs(file);
                else if (file.getName().endsWith(".pdf")) pdfFiles.add(file);
            }
        }
    }

    private class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {
        private List<File> files;
        ArchiveAdapter(List<File> files) { this.files = files; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // نکته: ما از یک لایه سفارشی استفاده می‌کنیم که در مرحله بعد کدش را می‌دهم
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archive, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = files.get(position);
            holder.fileName.setText(file.getName());
            
            // باز کردن فایل
            holder.itemView.setOnClickListener(v -> {
                Uri uri = FileProvider.getUriForFile(ArchiveActivity.this, getPackageName() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "باز کردن فایل"));
            });

            // دکمه حذف
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ArchiveActivity.this)
                    .setTitle("حذف فایل")
                    .setMessage("آیا از حذف این فایل اطمینان دارید؟")
                    .setPositiveButton("بله", (d, w) -> {
                        if (file.delete()) {
                            files.remove(position);
                            notifyDataSetChanged();
                            Toast.makeText(ArchiveActivity.this, "فایل حذف شد", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("خیر", null).show();
            });
        }

        @Override
        public int getItemCount() { return files.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName;
            ImageView btnDelete;
            ViewHolder(View v) {
                super(v);
                fileName = v.findViewById(R.id.txtFileName);
                btnDelete = v.findViewById(R.id.imgDeleteFile);
            }
        }
    }
}
