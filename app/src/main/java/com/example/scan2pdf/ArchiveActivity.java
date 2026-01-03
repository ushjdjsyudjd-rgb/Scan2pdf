package com.example.scan2pdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArchiveActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<File> pdfFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        Toolbar toolbar = findViewById(R.id.toolbarArchive);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerViewArchive);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFiles();
    }

    private void loadFiles() {
        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Scan2PDF");
        if (root.exists()) {
            findPdfs(root);
            if (pdfFiles.isEmpty()) {
                Toast.makeText(this, "هیچ فایلی یافت نشد", Toast.LENGTH_SHORT).show();
            }
            recyclerView.setAdapter(new ArchiveAdapter(pdfFiles));
        }
    }

    private void findPdfs(File dir) {
        File[] list = dir.listFiles();
        if (list != null) {
            for (File file : list) {
                if (file.isDirectory()) {
                    findPdfs(file);
                } else if (file.getName().endsWith(".pdf")) {
                    pdfFiles.add(file);
                }
            }
        }
    }

    private class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {
        private List<File> files;
        ArchiveAdapter(List<File> files) { this.files = files; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = files.get(position);
            holder.text1.setText(file.getName());
            holder.text2.setText(file.getParentFile().getName()); // نمایش تاریخ پوشه
            holder.itemView.setOnClickListener(v -> {
                Uri uri = FileProvider.getUriForFile(ArchiveActivity.this, getPackageName() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "باز کردن با..."));
            });
        }

        @Override
        public int getItemCount() { return files.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
