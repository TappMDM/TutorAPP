package com.example.singularity.tapp;

import android.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tutee extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button requesttutor;
    private LatLng pickupLocation;
    private Boolean requestBol =  false;

    private Marker pickupMarker;

    private String destination, requestSubject, value;

    private LinearLayout mTutorInfo;
    private ImageView mTutorProfileImage;
    private TextView mTutorName, mTutorProgram, mTutorDestination;
    private RadioGroup mRadioGroup;
    private RadioButton radioButton;
    private ImageView nextProfileImge;
    private TextView nexttutorName;
    private String nName;

    private int selectedId;

    private int radius = 1;
    private Boolean tutorfound = false;
    private String tutorfoundId;
    private LatLng destinationLatLng;

    private int state;

    GeoQuery geoQuery;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.tutee_layout, container, false);

        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        SupportMapFragment fragment = new SupportMapFragment();
        transaction.add(R.id.mapViewTutee, fragment);
        transaction.commit();
        requesttutor = (Button) view.findViewById(R.id.requesttutor);
        destinationLatLng = new LatLng(0.0,0.0);
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else {
            fragment.getMapAsync(this);
        }

        mTutorInfo = (LinearLayout) view.findViewById(R.id.tutorInfo);
        mTutorProfileImage = (ImageView) view.findViewById(R.id.tutorProfileImage);
        mTutorName = (TextView) view.findViewById(R.id.tutorName);
        mTutorDestination = (TextView) view.findViewById(R.id.tutorLocation);
        mTutorProgram = (TextView) view.findViewById(R.id.tutorProgram);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.tuteeradiogroup);
        mRadioGroup.check(R.id.math);
        selectedId = mRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) view.findViewById(selectedId);
        value = radioButton.getText().toString();

        //final RadioButton radioButton = (RadioButton) view.findViewById(selectedId);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                selectedId = mRadioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = (RadioButton) view.findViewById(selectedId);
                value = radioButton.getText().toString();
                Toast.makeText(getActivity(), value, Toast.LENGTH_LONG).show();
            }
        });

        requesttutor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(requestBol){
                    endSession();

                }else {
                    if(value == ""){
                        return;
                    }
                    requestSubject = value;
                    requestBol = true;
                    radius = 1;
                    String Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tutor Request");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(Uid, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pick up here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));

                    requesttutor.setText("Getting your tutor...");
                    getClosestTutor();
                }
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
            }
        });

        return view;
    }

    private void showTutorDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View dialogView = inflater.inflate(R.layout.searchtutordialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Find your Tutor");

        final TextView nexttutorName = dialogView.findViewById(R.id.nextTutorName);
        final TextView nexttutorProgram = dialogView.findViewById(R.id.nextTutorProgram);
        final Button bNext = dialogView.findViewById(R.id.nexttutordialog);
        final Button bBook = dialogView.findViewById(R.id.booktutordialog);
        nextProfileImge = (ImageView) dialogView.findViewById(R.id.nextimagedialog);


        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();



        DatabaseReference nexttutordat = FirebaseDatabase.getInstance().getReference().child("User").child(tutorfoundId);
        nexttutordat.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("userName") !=null) {
                        nName = map.get("userName").toString();
                        nexttutorName.setText(nName);
                    }
                    if(map.get("program")!= null){
                        nexttutorProgram.setText(map.get("program").toString());
                    }
                    if(map.get("profileImageUrl")!= null){
                        Glide.with(getActivity().getApplication()).load(map.get("profileImageUrl").toString()).into(nextProfileImge);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        bBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                getTutorLocation();
                getTutorfinished();
            }
        });

        bNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tutorfoundId != null){
                    DatabaseReference tutorref = FirebaseDatabase.getInstance().getReference().child("Profile").child(tutorfoundId);
                    tutorref.setValue(true);
                    tutorfoundId = null;
                    tutorfound = false;
                }
                alertDialog.dismiss();
                radius++;
                return;
            }
        });

    }

    private void getClosestTutor(){

        final DatabaseReference tutorLocation = FirebaseDatabase.getInstance().getReference().child("Available Tutors");
        final GeoFire geoFire = new GeoFire(tutorLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!tutorfound && requestBol){
                    final DatabaseReference mTuteeDatabase = FirebaseDatabase.getInstance().getReference().child("User").child(key);
                    mTuteeDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                Map<String, Object> Tutormap = (Map<String, Object>) dataSnapshot.getValue();
                                if(tutorfound){
                                    return;
                                }
                                if(Tutormap.get("subject").equals(requestSubject)){
                                    tutorfound = true;
                                    tutorfoundId = dataSnapshot.getKey();
                                    DatabaseReference tutorref = FirebaseDatabase.getInstance().getReference().child("Profile").child(tutorfoundId).child("tuteeRequest");
                                    String tuteeId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("TuteeId", tuteeId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);
                                    tutorref.updateChildren(map);
                                    showTutorDialog();
                                    getTutorInfo();
                                    //getTutorLocation();
                                }
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!tutorfound){
                    radius++;
                    String radiusnew = String.valueOf(radius);
                    if(radius>15){
                        radius = 1;
                        String Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tutor Request");
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.removeLocation(Uid);
                        Toast.makeText(getActivity(), "No tutor around!", Toast.LENGTH_LONG).show();
                        requestBol = false ;
                        requesttutor.setText("Find Tutor");
                        return;
                    }else {
                        getClosestTutor();
                    }
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getTutorInfo(){
        mTutorInfo.setVisibility(View.VISIBLE);
        DatabaseReference mTuteeDatabase = FirebaseDatabase.getInstance().getReference().child("User").child(tutorfoundId);
        mTuteeDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if(map.get("userName")!= null){
                            mTutorName.setText(map.get("userName").toString());
                        }
                        if(map.get("program")!= null){
                            mTutorProgram.setText(map.get("program").toString());
                        }
                        if(map.get("profileImageUrl")!= null){
                            Glide.with(getActivity().getApplication()).load(map.get("profileImageUrl").toString()).into(mTutorProfileImage);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private  DatabaseReference sessionendedRef;
    private ValueEventListener sessionendedRefListener;
    private void getTutorfinished(){
        sessionendedRef = FirebaseDatabase.getInstance().getReference().child("Profile").child(tutorfoundId).child("tuteeRequest").child("TuteeId");
        sessionendedRefListener = sessionendedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                }else{
                    endSession();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endSession(){
        requestBol = false;
        geoQuery.removeAllListeners();
        tutorlocationref.removeEventListener(tutorlocationrefListener);
        sessionendedRef.removeEventListener(sessionendedRefListener);

        if(tutorfoundId != null){
            DatabaseReference tutorref = FirebaseDatabase.getInstance().getReference().child("Profile").child(tutorfoundId);
            tutorref.setValue(true);
            tutorfoundId = null;
        }
        tutorfound = false;
        radius = 1;
        String Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tutor Request");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(Uid);

        if(pickupMarker != null){
            pickupMarker.remove();
        }
        requesttutor.setText("Find Tutor");
        mTutorInfo.setVisibility(View.GONE);
        mTutorName.setText("");
        mTutorProgram.setText("");
        mTutorProfileImage.setImageResource(R.mipmap.ic_launcher);

    }




    private DatabaseReference tutorlocationref;
    private ValueEventListener tutorlocationrefListener;
    Marker mtutorMarker;
    private void getTutorLocation(){
        tutorlocationref = FirebaseDatabase.getInstance().getReference().child("OccupiedTutor").child(tutorfoundId).child("l");
        tutorlocationrefListener = tutorlocationref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;
                    requesttutor.setText("Tutor Found");
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(0) != null){
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng tutorlatLang = new LatLng(locationLat,locationLong);
                    if(mtutorMarker != null){
                        mtutorMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(tutorlatLang.latitude);
                    loc2.setLongitude(tutorlatLang.longitude);

                    //returns the distance between the two
                    float distance = loc1.distanceTo(loc2);

                    if(distance < 100){
                        requesttutor.setText("Tutor nearby");
                    }else{
                        requesttutor.setText("Tutor Found" +String.valueOf(distance));
                    }
                    mtutorMarker = mMap.addMarker(new MarkerOptions().position(tutorlatLang).title("your tutor").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)));
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

        getActivity().setTitle("TuteePage");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Request for location on initialization
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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