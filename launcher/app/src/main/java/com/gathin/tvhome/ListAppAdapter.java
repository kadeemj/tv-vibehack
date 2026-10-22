package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The photo-style home: one vertical list of app icons and names. The favourites (dock_apps order)
 * float to the top, everything else follows alphabetically. Focus only brightens the row.
 */
public class ListAppAdapter extends RecyclerView.Adapter<ListAppAdapter.Holder> {

    interface RowListener {
        void onRowFocused(int position, View row);

        /** Called for D-pad Left/Right on a row. Return true when handled. */
        boolean onRowHorizontalKey(int position, int keyCode);
    }

    private final Context ctx;
    private final RowListener listener;
    /** Dock order (res/values/config.xml, dock_apps). */
    private final List<String> favourites;
    /** Never shown (res/values/config.xml, hidden_apps). */
    private final Set<String> hidden;
    private final List<AppAdapter.Entry> items = new ArrayList<>();

    ListAppAdapter(Context ctx, RowListener listener) {
        this.ctx = ctx;
        this.listener = listener;
        this.favourites = Arrays.asList(ctx.getResources().getStringArray(R.array.dock_apps));
        this.hidden = new HashSet<>(Arrays.asList(ctx.getResources().getStringArray(R.array.hidden_apps)));
        setHasStableIds(true);
    }

    void reload() {
        PackageManager pm = ctx.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<AppAdapter.Entry> fresh = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(probe, 0)) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName()) || hidden.contains(pkg)) continue;
            Drawable icon = ri.loadIcon(pm);
            String label = String.valueOf(ri.loadLabel(pm));
            fresh.add(new AppAdapter.Entry(label, new ComponentName(pkg, ri.activityInfo.name), null, icon));
        }
        Collections.sort(fresh, (a, b) -> {
            int fa = favourites.indexOf(a.pkg), fb = favourites.indexOf(b.pkg);
            if (fa >= 0 || fb >= 0) {
                if (fa < 0) return 1;
                if (fb < 0) return -1;
                return Integer.compare(fa, fb);
            }
            return a.label.compareToIgnoreCase(b.label);
        });
        items.clear();
        items.addAll(fresh);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_row, parent, false);
        return new Holder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AppAdapter.Entry e = items.get(position);
        h.icon.setImageDrawable(e.icon);
        h.label.setText(e.label);
        boolean focused = h.row.isFocused();   // a rebind after reload() can hit the focused row
        h.label.setAlpha(focused ? 1f : 0.88f);
        h.chip.setScaleX(focused ? 1.10f : 1f);
        h.chip.setScaleY(focused ? 1.10f : 1f);
        h.row.setOnClickListener(v -> launch(e));
        h.row.setOnLongClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + e.pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ctx.startActivity(i);
            } catch (Exception ignored) {
            }
            return true;
        });
    }

    private void launch(AppAdapter.Entry e) {
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(e.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            ctx.startActivity(i);
        } catch (Exception ex) {
            Toast.makeText(ctx, ctx.getString(R.string.cant_open, e.label), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).component.flattenToShortString().hashCode();
    }

    /** Stable key for the row at a position, or null. */
    String keyAt(int position) {
        return position >= 0 && position < items.size() ? items.get(position).component.flattenToShortString() : null;
    }

    /** Position of the row with this key, or -1 when it is gone. */
    int positionOf(String key) {
        if (key == null) return -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).component.flattenToShortString().equals(key)) return i;
        }
        return -1;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View row;
        final View chip;
        final ImageView icon;
        final TextView label;

        Holder(View v, RowListener listener) {
            super(v);
            row = v.findViewById(R.id.card);
            chip = v.findViewById(R.id.chip);
            icon = v.findViewById(R.id.icon);
            label = v.findViewById(R.id.label);

            // Left/Right: hand the keys to the listener (Right jumps to the bottom-right pills).
            row.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                int pos = getBindingAdapterPosition();
                return listener != null && pos != RecyclerView.NO_POSITION
                        && listener.onRowHorizontalKey(pos, keyCode);
            });
            row.setOnFocusChangeListener((view, focused) -> {
                // The chip pops, the label brightens; the row itself stays put.
                chip.animate().scaleX(focused ? 1.10f : 1f).scaleY(focused ? 1.10f : 1f)
                        .setDuration(focused ? 220 : 180)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(focused ? 1.5f : 0f))
                        .start();
                label.animate().alpha(focused ? 1f : 0.88f)
                        .setDuration(180).start();
                int pos = getBindingAdapterPosition();
                if (focused && listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onRowFocused(pos, view);
                }
            });
        }
    }
}
