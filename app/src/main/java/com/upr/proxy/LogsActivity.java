package com.upr.proxy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LogsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView txtEmpty;
    private LogsAdapter adapter;
    private char filterLevel = 'A'; // 'A' = All
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final LogBuffer.Listener listener = entry -> ui.post(() -> {
        if (filterLevel == 'A' || entry.level == filterLevel) {
            adapter.add(entry);
            recycler.scrollToPosition(adapter.getItemCount() - 1);
            updateEmpty();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_logs);

        recycler = findViewById(R.id.recyclerLogs);
        txtEmpty = findViewById(R.id.txtEmpty);

        adapter = new LogsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // Cargar logs ya existentes
        List<LogBuffer.Entry> existing = LogBuffer.snapshot();
        adapter.setAll(existing);
        updateEmpty();
        if (!existing.isEmpty()) {
            recycler.scrollToPosition(existing.size() - 1);
        }

        LogBuffer.register(listener);

        findViewById(R.id.btnCopy).setOnClickListener(v -> copiarTodo());

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            LogBuffer.clear();
            adapter.setAll(new ArrayList<>());
            updateEmpty();
            L.i("LogsActivity", "Logs borrados por el usuario");
        });

        com.google.android.material.chip.ChipGroup cg = findViewById(R.id.chipGroup);
        cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipInfo) filterLevel = 'I';
            else if (id == R.id.chipWarn) filterLevel = 'W';
            else if (id == R.id.chipError) filterLevel = 'E';
            else filterLevel = 'A';
            aplicarFiltro();
        });
    }

    private void aplicarFiltro() {
        List<LogBuffer.Entry> all = LogBuffer.snapshot();
        if (filterLevel == 'A') {
            adapter.setAll(all);
        } else {
            List<LogBuffer.Entry> filtered = new ArrayList<>();
            for (LogBuffer.Entry e : all) {
                if (e.level == filterLevel) filtered.add(e);
            }
            adapter.setAll(filtered);
        }
        updateEmpty();
    }

    private void updateEmpty() {
        boolean vacio = adapter.getItemCount() == 0;
        txtEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recycler.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void copiarTodo() {
        String texto = LogBuffer.fullText();
        if (texto.isEmpty()) {
            Toast.makeText(this, "No hay logs para copiar", Toast.LENGTH_SHORT).show();
            return;
        }
        final ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("UPR Proxy logs", texto));
            Toast.makeText(this, "Logs copiados (" + texto.length() + " caracteres).\n"
                    + "Se borrarán del portapapeles en 60 segundos.",
                    Toast.LENGTH_LONG).show();

            // Limpiar automáticamente en 60 s para que otras apps no puedan leerlos
            ui.postDelayed(() -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        cm.clearPrimaryClip();
                    } else {
                        cm.setPrimaryClip(ClipData.newPlainText("", ""));
                    }
                    L.i("LogsActivity", "Portapapeles limpiado automáticamente");
                } catch (Exception e) {
                    L.w("LogsActivity", "No se pudo limpiar el portapapeles: " + e.getMessage());
                }
            }, 60000);
        }
    }

    @Override
    protected void onDestroy() {
        LogBuffer.unregister(listener);
        super.onDestroy();
    }

    // ---------- Adapter ----------

    static class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.VH> {
        private final List<LogBuffer.Entry> items = new ArrayList<>();

        void add(LogBuffer.Entry e) {
            items.add(e);
            notifyItemInserted(items.size() - 1);
        }

        void setAll(List<LogBuffer.Entry> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            LogBuffer.Entry e = items.get(position);
            h.txt.setText(LogBuffer.formatEntry(e));
            int color;
            switch (e.level) {
                case 'E': color = 0xFFD32F2F; break;
                case 'W': color = 0xFFF57C00; break;
                case 'I': color = 0xFF388E3C; break;
                default:  color = 0xFF37474F; break;
            }
            h.txt.setTextColor(color);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView txt;
            VH(@NonNull View v) {
                super(v);
                txt = v.findViewById(R.id.txtLogLine);
            }
        }
    }
}
