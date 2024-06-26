package com.example.attendease;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.idling.CountingIdlingResource;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This class represents the Attendees Notifications page where an Attendee can see relevant information
 * about the events they have signed up for
 */
public class AttendeeNotifications extends AppCompatActivity implements ViewMsgDialog.AddMsgDialogListener {
    private ArrayList<Msg> dataList;
    private ListView MsgList;
    private ArrayAdapter<Msg> MsgAdapter;

    private CollectionReference eventsRef;
    private DocumentReference attendee_Ref;
    private ArrayList<String> stringArray;

    private BottomNavigationView bottomNav;
    private String deviceID;
    private CollectionReference signInRef;

    private ArrayList<String> eventArray;
    private CollectionReference realeventsRef;
    private Attendee attendee;
    private CountingIdlingResource countingIdlingResource;
    private final Database database = Database.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attendee_notification);
        countingIdlingResource = new CountingIdlingResource("FirebaseLoading");
        attendee = (Attendee) Objects.requireNonNull(getIntent().getExtras()).get("attendee");
        deviceID = attendee.getDeviceID();;
        //deviceID= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        attendee_Ref=database.getAttendeesRef().document(deviceID);
        realeventsRef = database.getEventsRef();
        signInRef=database.getSignInsRef();
        eventsRef=database.getNotificationsRef();
        countingIdlingResource = new CountingIdlingResource("FirebaseLoading");
        bottomNav = findViewById(R.id.attendee_bottom_nav);
        ImageView imageview=findViewById(R.id.backgroundimageview);
        TextView textview=findViewById(R.id.textView7);
        TextView textview2=findViewById(R.id.textView8);

        imageview.setVisibility(View.INVISIBLE);
        textview.setVisibility(View.INVISIBLE);
        textview2.setVisibility(View.INVISIBLE);

        eventArray=new ArrayList<>();

        MsgList=findViewById(R.id.Msg_list);
        String[] Title = {};
        String[] Messages = {};
        dataList = new ArrayList<Msg>();


        MsgAdapter = new MsgAdapter(this, dataList);
        MsgList.setAdapter(MsgAdapter);
        getallnotifications();

        BottomNavigationView bottomNavAdminDashboard = findViewById(R.id.attendee_bottom_nav);
        bottomNavAdminDashboard.setSelectedItemId(R.id.nav_bell);
        bottomNavAdminDashboard.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Intent intent=new Intent(AttendeeNotifications.this, AttendeeDashboardActivity.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_events) {
                    Intent intent=new Intent(AttendeeNotifications.this, BrowseAllEvents.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_bell) {
                    // Already on the AttendeeNotifications, no need to start a new instance
                    return true;
                } else if (id == R.id.nav_profile) {
                    Intent intent=new Intent(AttendeeNotifications.this, EditProfileActivity.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });


    }

    /**
     * Deletes the selected message from the list and Firestore.
     *
     * @param message The message to be deleted.
     * @param position The position of the message in the list.
     */
    public void deleteMsg(Msg message, int position){
        MsgAdapter.remove(message);
        MsgAdapter.notifyDataSetChanged();
        attendee_Ref.update("notification_deleted", FieldValue.arrayUnion(message.getUnique_id()));
        makeinvisible();


    }

    /**
     * Adds a message to the list.
     *
     * @param message The message to be added.
     */
    public void addMsg(Msg message){
        MsgAdapter.add(message);
        MsgAdapter.notifyDataSetChanged();
    }

    /**
     * Makes certain views invisible if the list of messages is empty.
     */
    public void makeinvisible(){
        if (dataList.isEmpty()){
            ImageView imageview=findViewById(R.id.backgroundimageview);
            TextView textview=findViewById(R.id.textView7);
            TextView textview2=findViewById(R.id.textView8);

            imageview.setVisibility(View.VISIBLE);
            textview.setVisibility(View.VISIBLE);
            textview2.setVisibility(View.VISIBLE);
        }
        else{
            ImageView imageview=findViewById(R.id.backgroundimageview);
            TextView textview=findViewById(R.id.textView7);
            TextView textview2=findViewById(R.id.textView8);

            imageview.setVisibility(View.INVISIBLE);
            textview.setVisibility(View.INVISIBLE);
            textview2.setVisibility(View.INVISIBLE);
        }
    }





    @Override
    protected void onResume() {
        super.onResume();

        bottomNav = findViewById(R.id.attendee_bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_bell);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Log.d("DEBUG", String.format("onNavigationItemSelected: %d", id));
                if (id == R.id.nav_home) {// Handle click on Home item
                    Log.d("DEBUG", "Home item clicked");
                    Intent intent=new Intent(AttendeeNotifications.this, AttendeeDashboardActivity.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                } else if (id == R.id.nav_events) {// Handle click on Events item
                    Log.d("DEBUG", "Events item clicked");
                    Intent intent=new Intent(AttendeeNotifications.this, BrowseAllEvents.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);
                } else if (id == R.id.nav_bell) {// Handle click on Bell item
                    Log.d("DEBUG", "Bell item clicked");
                } else if (id == R.id.nav_profile) {// Handle click on Profile item
                    Log.d("DEBUG", "Profile item clicked");
                    Intent intent = new Intent(AttendeeNotifications.this, EditProfileActivity.class);
                    intent.putExtra("attendee", attendee);
                    startActivity(intent);

                }
                return true;
            }
        });



        MsgList.setOnItemLongClickListener((parent, views, position, id) ->{
            Msg selectedMsg = dataList.get(position);
            new ViewMsgDialog(selectedMsg,position).show(getSupportFragmentManager(), "View Message");
            return true;
        });
        MsgList.setOnItemClickListener((parent, views, position, id) ->{
            Msg selectedMsg = dataList.get(position);
            String Title=selectedMsg.getTitle().toString();
            String Message=selectedMsg.getMessage().toString();
            String event=selectedMsg.getEvent().toString();
            String event_name=selectedMsg.getEvent_name().toString();
            Intent intent= new Intent(AttendeeNotifications.this, ViewMsg.class);
            intent.putExtra("Title",Title);
            intent.putExtra("Message",Message);
            intent.putExtra("event",event);
            intent.putExtra("event_name",event_name);

            startActivity(intent);
        });
    }

    /**
     * This method fetches notifications from Firestore.
     */
    private void getallnotifications(){
        FirebaseLoadingTestHelper.increment();
        Log.d("idli","increment");
        attendee_Ref.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Document exists in the database
                            if (documentSnapshot.contains("notification_deleted")) {
                                // 'notification_deleted' field exists in the document
                                stringArray = (ArrayList<String>) documentSnapshot.get("notification_deleted");
                            } else {
                                // 'notification_deleted' field doesn't exist in the document
                                stringArray = null;
                            }
                            eventlist();
                            // Now 'variableValue' contains the value of the variable from the Firestore
                        } else {
                            stringArray=null;
                            eventlist();
                            // Document does not exist
                        }
                    }
                });
    }

    /**
     * Retrieves notifications from Firestore and populates the notification list based on the attendee's events.
     */
    private void eve(){
        //forattendee only
        eventsRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    Log.d("error","isrunning2times");
                    ArrayList <String> test_array=stringArray;
                    //cityDataList.clear();
                    for (DocumentChange doc: queryDocumentSnapshots.getDocumentChanges()) {
                        switch (doc.getType()) {
                            case ADDED:
                            case MODIFIED:
                                String Title = doc.getDocument().getString("title");
                                String Notification = doc.getDocument().getString("message");
                                String event=doc.getDocument().getString("event");
                                String event_name=doc.getDocument().getString("event_name");
                                String Unique_id=doc.getDocument().getId().toString();
                                if(!eventArray.contains(event)){
                                    break;
                                }


                                if(stringArray!=null){
                                    if (stringArray.contains(Unique_id)){
                                        test_array.remove(Unique_id);
                                        break;
                                    }
                                }
                                //String sent_by= doc.getDocument().getString("sentBy");
                                Log.d("Firestore", String.format("City(%s, %s) fetched", Title,
                                        Notification));
                                Msg add_Msg=new Msg(Title, Notification,event,event_name);
                                add_Msg.setUnique_id(Unique_id);
                                dataList.add(add_Msg);
                                break;
                                /*case REMOVED:
                                    Log.d(TAG, "Removed document: " + dc.getDocument().getData());
                                    break;*/
                        }

                    }
                    if(test_array!=null){
                        for (String test : test_array){
                            attendee_Ref.update("notification_deleted",FieldValue.arrayRemove(test));
                        }
                    }


                    makeinvisible();

                    //addCitiesInit();
                    MsgAdapter.notifyDataSetChanged();
                    Log.d("idli","decrement");
                    FirebaseLoadingTestHelper.decrement();


                }
            }
        });
    }

    /**
     * Fetches events associated with the attendee from Firestore.
     */
    private void eventlist(){
        signInRef.whereEqualTo("attendeeID",deviceID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        // Document found where fieldName is equal to desiredValue
                        eventArray.add(doc.getString("eventID"));
                        Log.d("debug",doc.getString("eventID"));
                    }
                    eve();
                }
            }
        });
    }

}