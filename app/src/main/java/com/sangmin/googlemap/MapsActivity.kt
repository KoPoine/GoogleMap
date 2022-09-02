package com.sangmin.googlemap

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.sangmin.googlemap.api.HospitalOpenApi
import com.sangmin.googlemap.api.HospitalOpenService
import com.sangmin.googlemap.data.Hospital
import com.sangmin.googlemap.databinding.ActivityMapsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val PERM_FLAG = 99


    private lateinit var mMap: GoogleMap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (isPermitted()) {
            startProcess()

        } else {
            ActivityCompat.requestPermissions(this, permissions, PERM_FLAG)
        }


    }

    fun isPermitted(): Boolean {
        for(perm in permissions){
            if(ContextCompat.checkSelfPermission(this, perm) != PERMISSION_GRANTED){
                return  false
            }
        }

        return true
    }


    fun startProcess() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        setUpdateLocationListner()


//        // Add a marker in Sydney and move the camera
//        val seoul = LatLng(37.5663, 126.9779)
//        // 마커 아이콘 만들기
//        val descriptor = getDescriptorFromDrawable(R.drawable.marker)
//
//        // 마커
//        val marker = MarkerOptions()
//            .position(seoul)
//            .title("Marker in Sydney")
//            .icon(descriptor)
//
//        mMap.addMarker(marker)
//        // 카메라의 위치
//        val cameraOption = CameraPosition.Builder()
//            .target(seoul)
//            .zoom(17f)
//            .build()
//
//        val camera = CameraUpdateFactory.newCameraPosition(cameraOption)
//
//
//        mMap.moveCamera(camera)
    }

//    -- 내 위치를 가져오는 코드
    lateinit var fusedLocationClient:FusedLocationProviderClient
    lateinit var locationCallback:LocationCallback

    @SuppressLint("MissingPermission")
    fun setUpdateLocationListner(){
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }

        locationCallback = object : LocationCallback (){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for((i, location) in it.locations.withIndex()){
                        Log.d("로케이션", "$i ${location.latitude}, ${location.longitude}")
                        setLastLocation(location)
                        loadHospital()
                }
                }

            }
        }


        // 로케이션 요청 함수 호출 (locationRequest, locationCallback
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

    }

    fun  setLastLocation(location : Location) {
        val myLocation = LatLng(location.latitude, location.longitude)
        val marker = MarkerOptions()
            .position(myLocation)
            .title("I am here!")
        val cameraOption = CameraPosition.Builder()
            .target(myLocation)
            .zoom(15.0f)
            .build()
        val  camera = CameraUpdateFactory.newCameraPosition(cameraOption)


        mMap.clear()

        mMap.addMarker(marker)
        mMap.moveCamera(camera)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
          PERM_FLAG -> {
              var check = true
              for (grant in grantResults) {
                  if (grant != PERMISSION_GRANTED) {
                      check = false
                      break
                  }
              }
              if (check){
                  startProcess()
              } else {
                  Toast.makeText(this, "권한을 승인해야지만 앱을 사용할 수 있습니다", Toast.LENGTH_SHORT).show()
                  finish()
              }
          }
        }
    }


//    Drawable Marker 사용시 필요한 함수수
   fun getDescriptorFromDrawable(drawableId: Int): BitmapDescriptor {
        var bitmapDrawable: BitmapDrawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bitmapDrawable = getDrawable(drawableId) as BitmapDrawable
        } else {
            bitmapDrawable = resources.getDrawable(drawableId) as BitmapDrawable
        }

        // 마커 크기 변환
        val scaledBitmap = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 80, 80, false)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)

    }


//    레트로핏 사용할 준비
    fun loadHospital() {
        val retrofit  = Retrofit.Builder()
            .baseUrl(HospitalOpenApi.DOMAIN)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(HospitalOpenService::class.java)

        service.getHospital(HospitalOpenApi.API_KEY, 200)
            .enqueue(object : Callback<Hospital>{
                override fun onResponse(call: Call<Hospital>, response: Response<Hospital>) {
                    val result = response.body()
                    showHospital(result)

                }

                override fun onFailure(call: Call<Hospital>, t: Throwable) {
                    Log.e("병원", "error= ${t.localizedMessage}")
                    Toast.makeText(this@MapsActivity, "데이터를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()

                }

            })



    }

//    병원들의 위치를 보여주는 함수
    fun showHospital(result:Hospital?) {
        result?.let {
            val latlngBounds = LatLngBounds.Builder()
            for (Hospital in it.TvEmgcHospitalInfo.row) {

                val position = LatLng(Hospital.WGS84LAT.toDouble(), Hospital.WGS84LON.toDouble())
                val marker = MarkerOptions().position(position).title(Hospital.DUTYNAME)


                mMap.addMarker(marker)

                latlngBounds.include(position)

            }

            val bounds = latlngBounds.build()
            val padding = 0


            val camera = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            mMap.moveCamera(camera)


        }

    }
}