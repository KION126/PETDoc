package com.petdoc.login;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class IconSliderAdapter extends RecyclerView.Adapter<IconSliderAdapter.IconViewHolder> {
    private int[] iconList;

    public IconSliderAdapter(int[] iconList) {
        this.iconList = iconList;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return new IconViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        holder.imageView.setImageResource(iconList[position]);
    }

    @Override
    public int getItemCount() {
        return iconList.length;
    }

    static class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView; // ← 여기 수정!
        }
    }

}

