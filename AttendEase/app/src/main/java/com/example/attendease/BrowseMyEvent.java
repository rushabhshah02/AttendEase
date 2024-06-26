package com.example.attendease;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.idling.CountingIdlingResource;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This class represents the Browse Events screen where
 * an Attendee can browse events they have signed up for
 */
public class BrowseMyEvent extends AppCompatActivity {
    private ArrayList<Event> dataList;
    private ListView EventList;
    private ArrayAdapter<Event> EventAdapter;
    private final Database database = Database.getInstance();
    private CollectionReference eventsRef;
    private CollectionReference signInRef;

    private Attendee attendee;
    private String eventID;
    private BottomNavigationView bottomNav;

    private CountingIdlingResource countingIdlingResource;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_my_event);

        attendee = (Attendee) Objects.requireNonNull(getIntent().getExtras()).get("attendee");
        signInRef = database.getSignInsRef();
        eventsRef = database.getEventsRef();
        countingIdlingResource = new CountingIdlingResource("FirebaseLoading");



        EventList=findViewById(R.id.Event_list);
        dataList=new ArrayList<Event>();
        EventAdapter=new BrowseEventAdapter(this,dataList);
        EventList.setAdapter(EventAdapter);
        updateDatalist();

        bottomNav = findViewById(R.id.attendee_bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Log.d("DEBUG", String.format("onNavigationItemSelected: %d", id));
                if (id == R.id.nav_home) {// Handle click on Home item
                    Log.d("DEBUG", "Home item clicked");
                    Intent intent=new Intent(BrowseMyEvent.this, AttendeeDashboardActivity.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_events) {// Handle click on Events item
                    Log.d("DEBUG", "Events item clicked");
                    return true;
                } else if (id == R.id.nav_bell) {// Handle click on Bell item
                    Log.d("DEBUG", "Bell item clicked");
                    Intent intent=new Intent(BrowseMyEvent.this, AttendeeNotifications.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_profile) {// Handle click on Profile item
                    Log.d("DEBUG", "Profile item clicked");
                    Intent intent=new Intent(BrowseMyEvent.this, EditProfileActivity.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                    return true;

                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Event event=dataList.get(position);
                Intent intent=new Intent(BrowseMyEvent.this, EventDetailsAttendee.class);
                intent.putExtra("attendee", attendee);
                intent.putExtra("eventID", event.getEventId());
                intent.putExtra("title",event.getTitle());
                intent.putExtra("QR",event.getCheckInQR());
                intent.putExtra("description",event.getDescription());
                intent.putExtra("dateTime",event.getDateTime().toDate().toString());
                intent.putExtra("location",event.getLocation());
                intent.putExtra("posterUrl",event.getPosterUrl());
                intent.putExtra("prevActivity", "BrowseMyEvent");
                startActivity(intent);
            }
        });

    }


    /**
     * Updates the event list array adapter with the events on the attendee has signed up for
     */
    private void updateDatalist(){
        //Log.d("error","2nddb_decrement_Before");
        FirebaseLoadingTestHelper.increment();

        signInRef.whereEqualTo("attendeeID",attendee.getDeviceID()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        // Document found where fieldName is equal to desiredValue
                        eventID = doc.getString("eventID");
                        //Log.d("error",eventID);

                        eventsRef.document(eventID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                DocumentSnapshot eventDocument = task.getResult();
                                String Title = eventDocument.getString("title");
                                String eventId=eventDocument.getId().toString();
                                String QR = eventDocument.getString("checkInQR");
                                String description = eventDocument.getString("description");

                                String organizerId=eventDocument.getString("organizerId");
                                Timestamp dateTime=eventDocument.getTimestamp("dateTime");

                                String location=eventDocument.getString("location");
                                String posterUrl=eventDocument.getString("posterUrl");

                                if (posterUrl == null) {
                                    Log.d("DEBUG", String.format("onComplete: PosterURL was null here for event : %s", eventId));
                                }

                                Boolean isGeoTrackingEnabled=eventDocument.getBoolean("isGeoTrackingEnabled");
                                //not able to import this?
                                int maxAttendees=0;
                                Event new_event= new Event(eventId,Title,description,organizerId,dateTime,location,null,QR,posterUrl,false,0);
                                dataList.add(new_event);
                                EventAdapter.notifyDataSetChanged();

                            }
                        });


                    }
                    FirebaseLoadingTestHelper.decrement();
                }


            }
        });
    }


}