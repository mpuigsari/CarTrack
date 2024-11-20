package com.example.cartrack.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cartrack.Model.carModel;
import com.example.cartrack.R;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;


public class carInfoAdapter extends RecyclerView.Adapter<carInfoAdapter.ViewHolder> {
    private ArrayList<carModel> carList;
    private Context context;
    private OnCarEditListener editListener;
    private OnCarDeleteListener deleteListener;
    private OnCarDeviceListener deviceListener;

    public carInfoAdapter(ArrayList<carModel> carList, Context context, OnCarEditListener editListener, OnCarDeleteListener deleteListener, OnCarDeviceListener deviceListener) {
        this.carList = carList;
        this.context = context;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.deviceListener = deviceListener;
    }

    @NonNull
    @Override
    public carInfoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_carinfo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull carInfoAdapter.ViewHolder holder, int position) {
        carModel model = carList.get(position);
        holder.txtcarName.setText(model.getCarName());
        holder.txtcarLocation.setText(model.getCarRealAddress());
        holder.txtcarModel.setText(model.getCarModel());
        holder.txtcarPlate.setText(model.getCarPlate());

        try {
            String imageUrl = model.getCarImage() + "w=300&h=300&fit=crop&auto=format";
            Log.d("Picasso", "Image URL: " + imageUrl);
            // Try loading from cache first
            Picasso.get()
                    .load(imageUrl).networkPolicy(NetworkPolicy.OFFLINE) // Load from cache if available
                    .into(holder.carImageview, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("Picasso", "Image loaded from cache.");}
                        @Override
                        public void onError(Exception e) {
                            Log.w("Picasso", "Cache miss, loading from network.");
                            // If not in cache, load from network
                            Picasso.get().load(imageUrl).into(holder.carImageview);}
                    });
        } catch (IllegalArgumentException e) {Log.e("Picasso", "Error loading image: " + e.getMessage());}

        // Set car state
        holder.txtcarState.setText("Stationary"); // Default state

        // Edit button click
        holder.buttonEditCar.setOnClickListener(v -> editListener.onEditCar(model));

        // Delete button click
        holder.buttonDeleteCar.setOnClickListener(v -> deleteListener.onDeleteCar(model, position));

        // Device button click
        holder.layoutdevice.setOnClickListener(v -> deviceListener.onDeviceCar(model));


    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView carImageview, buttonEditCar, buttonDeleteCar;
        private TextView txtcarName, txtcarLocation, txtcarModel, txtcarPlate, txtcarState;
        private LinearLayout layoutdevice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtcarName = itemView.findViewById(R.id.txtcarName);
            txtcarLocation = itemView.findViewById(R.id.txtcarLocation);
            txtcarModel = itemView.findViewById(R.id.txtcarModel);
            txtcarPlate = itemView.findViewById(R.id.txtcarPlate);
            txtcarState = itemView.findViewById(R.id.txtcarState);
            buttonEditCar = itemView.findViewById(R.id.buttonEditCar);
            buttonDeleteCar = itemView.findViewById(R.id.buttonDeleteCar);
            carImageview = itemView.findViewById(R.id.CarImage);
            layoutdevice = itemView.findViewById(R.id.buttonDevice);
        }
        // Add a method to update the car state
        public void setCarState(String state) {
            txtcarState.setText(state);
        }
        public String getCarState() {
            return txtcarState.getText().toString();
        }
    }


    // Listener interfaces
    public interface OnCarEditListener {
        void onEditCar(carModel car);
    }

    public interface OnCarDeleteListener {
        void onDeleteCar(carModel car, int position);
    }
    public interface OnCarDeviceListener {
        void onDeviceCar(carModel car);
    }
}
