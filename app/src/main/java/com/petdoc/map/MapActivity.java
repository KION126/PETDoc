package com.petdoc.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.petdoc.R;
import com.petdoc.main.BaseActivity;
import com.petdoc.main.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private FusedLocationProviderClient fusedLocationClient;
    private NaverMap naverMap;
    private final String KAKAO_API_KEY = "41f4c0f70e78c0df1b4d774a76acdc80";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // MainActivity로 이동
            Intent intent = new Intent(MapActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // 현재 액티비티 종료
        });


        // XML에 미리 선언된 fragment 태그를 가져옴
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
    }


    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();

                            // ✅ 내 위치 마커
                            Marker myLocationMarker = new Marker();
                            myLocationMarker.setPosition(new LatLng(lat, lng));
                            myLocationMarker.setCaptionText("내 위치");
                            myLocationMarker.setMap(naverMap);

                            fetchNearbyHospitals(lat, lng);
                        }
                    });
        }
    }

    private void fetchNearbyHospitals(double lat, double lng) {
        OkHttpClient client = new OkHttpClient();

        HttpUrl url = HttpUrl.parse("https://dapi.kakao.com/v2/local/search/keyword.json")
                .newBuilder()
                .addQueryParameter("query", "동물병원")
                .addQueryParameter("x", String.valueOf(lng))
                .addQueryParameter("y", String.valueOf(lat))
                .addQueryParameter("radius", "2000")
                .addQueryParameter("size", "15")
                .addQueryParameter("sort", "distance")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "KakaoAK " + KAKAO_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray documents = jsonObject.getJSONArray("documents");

                        for (int i = 0; i < documents.length(); i++) {
                            JSONObject item = documents.getJSONObject(i);

                            String name = item.getString("place_name");
                            String phone = item.optString("phone", "전화번호 없음");
                            String address = item.optString("road_address_name", item.optString("address_name", "주소 없음"));
                            String distance = item.optString("distance", "정보 없음") + "m";
                            String placeUrl = item.optString("place_url", "");
                            double x = item.getDouble("x");
                            double y = item.getDouble("y");

                            // 병원 정보 텍스트 구성
                            String hospitalInfo = "📍 " + name + "\n"
                                    + "📞 " + phone + "\n"
                                    + "🏠 " + address + "\n"
                                    + "📏 거리: " + distance + "\n"
                                    + "🔗 자세히 보기: " + placeUrl;

                            runOnUiThread(() -> {
                                Marker marker = new Marker();
                                marker.setPosition(new LatLng(y, x));
                                marker.setCaptionText(name);
                                marker.setIcon(OverlayImage.fromResource(R.drawable.animal_hospital));
                                marker.setMap(naverMap);

                                // 마커 클릭 이벤트: 병원 정보 표시
                                marker.setOnClickListener(overlay -> {
                                    View sheetView = getLayoutInflater().inflate(R.layout.hospital_bottom_sheet, null);
                                    BottomSheetDialog dialog = new BottomSheetDialog(MapActivity.this);
                                    dialog.setContentView(sheetView);

                                    TextView tvName = sheetView.findViewById(R.id.tvHospitalName);
                                    TextView tvPhone = sheetView.findViewById(R.id.tvHospitalPhone);
                                    TextView tvAddress = sheetView.findViewById(R.id.tvHospitalAddress);
                                    TextView tvDistance = sheetView.findViewById(R.id.tvHospitalDistance);
                                    TextView tvLink = sheetView.findViewById(R.id.tvHospitalLink);

                                    tvName.setText(name);
                                    tvPhone.setText("📞 " + phone);
                                    tvAddress.setText("🏠 " + address);
                                    tvDistance.setText("📏 거리: " + distance);
                                    tvLink.setOnClickListener(v -> {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(placeUrl));
                                        startActivity(intent);
                                    });

                                    dialog.show();
                                    return true;
                                });

                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
