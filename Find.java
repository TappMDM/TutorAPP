package com.example.singularity.tapptutor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class Find extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private String tuteeId = "";

    private LinearLayout mTuteeInfo;
    private ImageView mTuteeProfileImage;
    private TextView mTuteeName, mTuteeProgram, mTuteeDestination;
    private Button mTutorAvailable;

    private Boolean setDriverAvailable = false;
    private int count = 0;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view= inflater.inflate(R.layout.activity_find,container,false);
        mTuteeInfo = (LinearLayout) view.findViewById(R.id.tuteeInfo);
        mTuteeProfileImage = (ImageView) view.findViewById(R.id.tuteeProfileImage);
        mTuteeName = (TextView) view.findViewById(R.id.tuteeName);
        mTuteeProgram = (TextView) view.findViewById(R.id.tuteeProgram);
        mTuteeDestination = (TextView) view.findViewById(R.id.tuteeDestination);
        mTutorAvailable = (Button) view.findViewById(R.id.settutoravailable) ;

        getAssignedTutee();
        mTutorAvailable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tuteeId==""){
                    switch (count){
                        case 0:
                            setDriverAvailable = true;
                            count = 1;
                            break;
                        case 1:
                            String Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            setDriverAvailable = false;
                            Toast.makeText(getActivity(), "Tutor on hold", Toast.LENGTH_LONG).show();
                            mTutorAvailable.setText("I'm Available");
                            count = 0;
                            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("Available Tutors");
                            GeoFire geoFireAvailable = new GeoFire(refAvailable);
                            geoFireAvailable.removeLocation(Uid);
                    }

                }
            }
        });
        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        SupportMapFragment fragment = new SupportMapFragment();
        transaction.add(R.id.mapView, fragment);
        transaction.commit();

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else {
            fragment.getMapAsync(this);
        }

    }

    private void getAssignedTuteeInfo(){
        mTuteeInfo.setVisibility(View.VISIBLE);
        DatabaseReference mTuteeDatabase = FirebaseDatabase.getInstance().getReference().child("User").child(tuteeId);
        mTuteeDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("userName")!= null){
                        mTuteeName.setText(map.get("userName").toString());
                    }
                    if(map.get("program")!= null){
                        mTuteeProgram.setText(map.get("program").toString());
                    }
                    if(map.get("profileImageUrl")!= null){
                        Glide.with(getActivity().getApplication()).load(map.get("profileImageUrl").toString()).into(mTuteeProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    private void getAssignedTutee(){
        String  tutorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedTuteeRef = FirebaseDatabase.getInstance().getReference().child("Profile").child(tutorId).child("tuteeRequest").child("TuteeId");
        assignedTuteeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    tuteeId = dataSnapshot.getValue().toString();
                    getAssignedTuteePickupLocation();
                    getAssignedTuteeDestination();
                    getAssignedTuteeInfo();
                }else{
                    tuteeId = "";
                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }
                    if (assignedTuteePickupLocationRefListener != null){
                        assignedTuteePickupLocationRef.removeEventListener(assignedTuteePickupLocationRefListener);
                    }
                    mTuteeInfo.setVisibility(View.GONE);
                    mTuteeName.setText("");
                    mTuteeProgram.setText("");
                    mTuteeDestination.setText("Destination: -- ");
                    mTuteeProfileImage.setImageResource(R.mipmap.ic_launcher);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedTuteeDestination(){
        String  tutorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedTuteeRef = FirebaseDatabase.getInstance().getReference().child("Profile").child(tutorId).child("tuteeRequest").child("destination");
        assignedTuteeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String destination = dataSnapshot.getValue().toString();
                    mTuteeDestination.setText("Destination: " + destination);
                }else{
                    mTuteeDestination.setText("Destination: -- ");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    Marker pickupMarker;
    private DatabaseReference assignedTuteePickupLocationRef;
    private ValueEventListener assignedTuteePickupLocationRefListener;

    private void getAssignedTuteePickupLocation(){
        assignedTuteePickupLocationRef = FirebaseDatabase.getInstance().getReference().child("Tutor Request").child(tuteeId).child("l");
        assignedTuteePickupLocationRefListener = assignedTuteePickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !tuteeId.equals("")){
                    List<Object> map = (List <Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(0) != null){
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng tutorlatLang = new LatLng(locationLat,locationLong);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(tutorlatLang).title("Assigned Tutee").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }




    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle("");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        //Getting update location

        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        String Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("Available Tutors");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("OccupiedTutor");

        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        GeoFire geoFireWorking = new GeoFire(refWorking);

        if(setDriverAvailable){
            switch (tuteeId) {
                case "":
                    geoFireWorking.removeLocation(Uid);
                    geoFireAvailable.setLocation(Uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    mTutorAvailable.setText("Waiting for tutee");
                    break;
                default:
                    geoFireAvailable.removeLocation(Uid);
                    geoFireWorking.setLocation(Uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    mTutorAvailable.setText("Tutee found!");
                    break;
            }
        }else{
            return;
        }


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Request for location on initialization
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }


    @Override
    public void onPause() {
        super.onPause();
        String Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Available Tutors");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(Uid);
        Toast.makeText(getActivity(), "Deleted", Toast.LENGTH_LONG).show();
        mGoogleApiClient.disconnect();

    }

    final int LOCATION_REQUEST_CODE=1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:{
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    FragmentManager manager = getFragmentManager();
                    FragmentTransaction transaction = manager.beginTransaction();
                    SupportMapFragment fragment = new SupportMapFragment();
                    transaction.add(R.id.mapView, fragment);
                    transaction.commit();
                    fragment.getMapAsync(this);
                }else {
                    Toast.makeText(getActivity(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


}