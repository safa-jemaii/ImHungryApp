package isetr.tp.imhungryapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //initialize variable
    Spinner spType;
    Button btFind;
    SupportMapFragment supportMapFragment;
    GoogleMap map;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat = 0, currentLong = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadLocale();

        setContentView(R.layout.activity_main);

        //Multilangage
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name));

        Button changeLang = findViewById(R.id.changeLang);
        changeLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show alerdialog to display list of language
                showChangeLanguageDialog();
            }
        });



        //Assign varibale
        spType = findViewById(R.id.sp_type);
        btFind = findViewById(R.id.bt_find);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        //initialize array of place type
        String[] placeTypeList = {"bank", "restaurants"};

        //initialize array of place name
        String[] placeNameList = {"Bank", "Restaurants"};


        //set adapter on spnner
        spType.setAdapter(new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item, placeNameList));


        //Initialize Fused Location Provider Client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //check permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            //when permission granted
            //call method
            getCurrentLocation();
        }else{
            //when permission refused
            //request permission

            ActivityCompat.requestPermissions(MainActivity.this
            , new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        btFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get selected position of spinner
                int i = spType.getSelectedItemPosition();
                //initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //url
                "?location=" + currentLat + " , " + currentLong + //location lat lng
                "&radius=5000" + //nearby radius
                "&types=" + placeTypeList[i] + //place type
                "&sensor=true" + //sensor
                "&key" + getResources().getString(R.string.google_map_key);

                //execute place task method to download json data
                new PlaceTask().execute(url);

            }
        });

    }

    //lets create seperate strings.xml for each language
    private void showChangeLanguageDialog() {
        final String[] listItems = {"French", "हिंदी", "English", "عربي"};
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle("choose language ...");
        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

                if(i == 0){
                    setLocale("fr");
                    recreate();

                }
                else if(i == 1){
                    setLocale("hi");
                    recreate();

                }

                else if(i == 2){
                    setLocale("en");
                    recreate();

                }

                else if(i == 3){
                    setLocale("ar");
                    recreate();

                }
                //dismiss alert dialog when language selected
                dialog.dismiss();
            }
        });
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config  = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        //save data to shared prefere,ces
        SharedPreferences.Editor editor =getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();
    }
    //load language saved in shared
    public void  loadLocale(){
        SharedPreferences prefs = getSharedPreferences("settings", Activity.MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        setLocale(language);
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        //initialize task location
        Task<Location> task = fusedLocationProviderClient.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //when sucess
                if ((location != null)) {

                    //when location is not equal to null
                    //get current lattitude
                    currentLat = location.getLatitude();

                    //get current longitude
                    currentLong = location.getLongitude();

                    //sync map
                    supportMapFragment.getMapAsync(new OnMapReadyCallback(){
                        @Override
                        public  void onMapReady(GoogleMap googleMap){
                            //whe map is ready
                            map=googleMap;
                            //zoom current location on map
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(currentLat,currentLong),10
                            ));
                        }

                    });

                }

            }
        });

    }}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //when permission granted
                //call method
                getCurrentLocation();
            }
        }
    }

    private class PlaceTask extends AsyncTask<String,Integer,String> {

        @Override
        protected String doInBackground(String... strings) {
            String data=null;
            try {
                //intialize data
                 data = downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            //execute parser task
            new ParserTask().execute(s);
        }
    }

    private String downloadUrl(String string) throws IOException {
        //intialize url
        URL url = new URL(string);
        //intialize connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //connect connection
        connection.connect();
        //intialize input stream
        InputStream stream = connection.getInputStream();
        //intialize buffer reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        //intialize strinf buildre
        StringBuilder builder = new StringBuilder();

        //intialize string variable
        String line ="";
        while ((line = reader.readLine()) != null) {
           //append line
           builder.append(line);

        }
        //get append adata
        String data = builder.toString();
        //close reader
        reader.close();
        //return data
        return  data;
    }

    private class ParserTask extends  AsyncTask<String,Integer, List<HashMap<String,String>>>{
        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
           //create json parser class
            JsonParser jsonParser = new JsonParser();
            //initialize hash map list
            List<HashMap<String,String>> mapList=null;
            JSONObject object = null;
            try {
                //initialize json object
                 object = new JSONObject(strings[0]);
                 //parse json object
                mapList = jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //return map list
            return mapList;
        }


        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            //clear map
            map.clear();
            //use for loop
            for (int i=0;i<hashMaps.size();i++){
                //initialize hash map
                HashMap<String,String> hashMapList = hashMaps.get(i);

                //get latitude
                double lat =Double.parseDouble(hashMapList.get("lat"));

                //get longitude
                double lng =Double.parseDouble(hashMapList.get("lng"));

                //get name
                String name = hashMapList.get("name");
                //concat latitude and longitude
                LatLng latLng = new LatLng(lat,lng);
                //initialize marker options
                MarkerOptions options = new MarkerOptions();
                //set position
                options.position(latLng);
                //set title
                options.title(name);
                //app marker on map
                map.addMarker(options);

            }
        }
    }
}