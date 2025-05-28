package com.petdoc.login;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.petdoc.R;
import com.petdoc.login.model.Pet;

import java.util.List;

public class PetListAdapter extends RecyclerView.Adapter<PetListAdapter.PetViewHolder> {
    private Context context;
    private List<String> petIdList;
    private List<Pet> petList;
    private String selectedPetId;
    private OnPetSelectedListener listener;

    public interface OnPetSelectedListener {
        void onPetSelected(String selectedPetId);
    }

    public PetListAdapter(Context context, List<String> petIdList, List<Pet> petList,
                          String selectedPetId, OnPetSelectedListener listener) {
        this.context = context;
        this.petIdList = petIdList;
        this.petList = petList;
        this.selectedPetId = selectedPetId;
        this.listener = listener;
    }

    public void setSelectedPetId(String selectedPetId) {
        this.selectedPetId = selectedPetId;
        notifyDataSetChanged(); // UI 갱신
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet_row, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);
        String petId = petIdList.get(position);

        holder.petName.setText(pet.basicInfo.name);

        Glide.with(context)
                .load(pet.basicInfo.imagePath)
                .placeholder(R.drawable.ic_dog_icon)
                .into(holder.petImage);

        // 현재 선택된 반려견만 초록 체크 표시
        holder.checkIcon.setVisibility(petId.equals(selectedPetId) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            CurrentPetManager.getInstance().setCurrentPetId(petId); // 현재 선택된 펫 ID 저장
            setSelectedPetId(petId); // UI 갱신
            listener.onPetSelected(petId); // 외부 콜백 실행 (예: BottomSheet 닫기 등)
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView petImage, checkIcon;
        TextView petName;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            petImage = itemView.findViewById(R.id.petProfileImage);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            petName = itemView.findViewById(R.id.petName);
        }
    }
}


