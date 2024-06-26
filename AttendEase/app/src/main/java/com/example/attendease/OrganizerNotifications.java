package com.example.attendease;

import static com.example.attendease.R.id.organizer_bottom_nav;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.idling.CountingIdlingResource;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

/**
 * Activity for managing notifications sent by the organizer to attendees.
 * Allows organizers to view, add, and delete notifications.
 */
public class    OrganizerNotifications extends AppCompatActivity implements ViewMsgDialog.AddMsgDialogListener {
    private ActivityResultLauncher<Intent> addMsgLauncher;
    private ArrayList<Msg> dataList;
    private ListView MsgList;
    private ArrayAdapter<Msg> MsgAdapter;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private String deviceID;

    private CollectionReference realeventsRef;
    private Attendee attendee;
    private CountingIdlingResource countingIdlingResource;
    private final Database database = Database.getInstance();
    private String event_name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_notifications);
        addMsgLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String title = result.getData().getStringExtra("Title");
                        String events = result.getData().getStringExtra("Events");
                        String body = result.getData().getStringExtra("Body");
                        event_name=result.getData().getStringExtra("EventName");

                        // Now you have the data, you can do whatever you want with it.
                        Msg message = new Msg(title, body, events,event_name);
                        addMsg(message);
                    }
                });

        ImageView imageview=findViewById(R.id.backgroundimageview);
        TextView textview=findViewById(R.id.textView7);
        TextView textview2=findViewById(R.id.textView8);

        imageview.setVisibility(View.INVISIBLE);
        textview.setVisibility(View.INVISIBLE);
        textview2.setVisibility(View.INVISIBLE);
        //Attendee attendee= (Attendee) getIntent().getSerializableExtra("Attendee");
        //need to implements Serializable in Attendee class
        //attendee.getsignupids
        countingIdlingResource = new CountingIdlingResource("FirebaseLoading");
        //attendee = (Attendee) Objects.requireNonNull(getIntent().getExtras()).get("attendee");
        //deviceID = attendee.getDeviceID();
        Intent intent=getIntent();
        deviceID=intent.getStringExtra("deviceId");
        realeventsRef = database.getEventsRef();
        eventsRef=database.getNotificationsRef();
        countingIdlingResource = new CountingIdlingResource("FirebaseLoading");


        MsgList=findViewById(R.id.Msg_list);
        String[] Title = {};
        String[] Messages = {};
        dataList = new ArrayList<Msg>();
        FirebaseLoadingTestHelper.increment();
        eventsRef
                //.whereEqualTo("sentBy", "name")
                .whereEqualTo("sentBy", deviceID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                // Document found where fieldName is equal to desiredValue
                                String Title = doc.getString("title");
                                String Notification = doc.getString("message");
                                String event_name = doc.getString("event_name");
                                Msg notif=new Msg(Title, Notification,"name",event_name);
                                notif.setUnique_id(doc.getId());
                                dataList.add(notif);

                            }
                            MsgAdapter = new MsgAdapter(OrganizerNotifications.this, dataList);
                            MsgList.setAdapter(MsgAdapter);
                            FirebaseLoadingTestHelper.decrement();
                        } else {
                            //Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        makeinvisible();
                    }
                });
        /*for (int i = 0; i < Title.length; i++) {
            dataList.add(new Msg(Title[i], Messages[i]));
        }*/

        BottomNavigationView bottomNavOrganizerNotifications = findViewById(R.id.organizer_bottom_nav);
        bottomNavOrganizerNotifications.setSelectedItemId(R.id.nav_notifications);
        bottomNavOrganizerNotifications.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Intent intent = new Intent(OrganizerNotifications.this, OrganizerDashboardActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_events) {
                    Intent intent = new Intent(OrganizerNotifications.this, OrganizerMyEventsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_notifications) {
                    // Already on the OrganizerNotification, no need to start a new instance
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Deletes a notification from the list and Firestore.
     *
     * @param message   The notification message to delete.
     * @param position  The position of the notification in the list.
     */
    public void deleteMsg(Msg message, int position){
        MsgAdapter.remove(message);
        MsgAdapter.notifyDataSetChanged();
        eventsRef.document(message.getUnique_id()).delete();
        makeinvisible();
    }

    /**
     * Adds a new notification to the list and Firestore.
     *
     * @param message   The notification message to add.
     */
    public void addMsg(Msg message){
        MsgAdapter.add(message);
        MsgAdapter.notifyDataSetChanged();
        makeinvisible();
        String Title= message.getTitle().toString();
        String Message= message.getMessage().toString();
        String event=message.getEvent();
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateString = sdf.format(new Date(currentTimeMillis));

        HashMap<String, String> data = new HashMap<>();
        data.put("title", Title);
        data.put("message",Message);
        data.put("timestamp", dateString);
        //need to get name
        data.put("sentBy",deviceID);
        data.put("event",event);
        data.put("event_name",event_name);
        eventsRef.document(message.getUnique_id()).set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", "DocumentSnapshot successfully written!");
                    }
                });
    }

    /**
     * Controls the visibility of UI elements based on the presence of notifications.
     * Hides background image and text if notifications exist, otherwise shows them.
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

        MsgList.setOnItemLongClickListener((parent, views, position, id) ->{
            Msg selectedMsg = dataList.get(position);
            new ViewMsgDialog(selectedMsg,position).show(getSupportFragmentManager(), "View Message");
            return true;
            /*Bundle bundle = new Bundle();
            bundle.putString("selectedMsg",selectedMsg.getMessage());
            bundle.putString("selectedTitle", selectedMsg.getTitle());
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment,bundle);*/

        });
        MsgList.setOnItemClickListener((parent, views, position, id) ->{
            Msg selectedMsg = dataList.get(position);
            String Title=selectedMsg.getTitle().toString();
            String Message=selectedMsg.getMessage().toString();
            String event_name=selectedMsg.getEvent_name().toString();
            Intent intent= new Intent(OrganizerNotifications.this, ViewMsgOrganizer.class);
            intent.putExtra("Title",Title);
            intent.putExtra("Message",Message);
            intent.putExtra("event_name",event_name);
            startActivity(intent);
            //new ViewMsgDialog(selectedMsg,position).show(getSupportFragmentManager(), "View Message");
            /*Bundle bundle = new Bundle();
            bundle.putString("selectedMsg",selectedMsg.getMessage());
            bundle.putString("selectedTitle", selectedMsg.getTitle());
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment,bundle);*/

        });
        //for organizers

        ImageButton adds=findViewById(R.id.AddButton);
        adds.setOnClickListener(v -> {
            //new ViewMsgDialog().show(getSupportFragmentManager(), "Add Message");
            ArrayList<String> eventIDs=new ArrayList<>();
            ArrayList<String> eventslist=new ArrayList<>();
            realeventsRef.whereEqualTo("organizerId",deviceID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            // Document found where fieldName is equal to desiredValue
                            eventIDs.add(doc.getString("eventId"));
                            eventslist.add(doc.getString("title"));

                        }
                        Intent intent= new Intent(OrganizerNotifications.this, MsgAdd.class);
                        intent.putExtra("eventIDs",eventIDs);
                        intent.putExtra("eventslist",eventslist);
                        addMsgLauncher.launch(intent);
                    }
                }
            });

            /*Bundle extras = getIntent().getExtras();
            String Title=extras.getString("Title");
            String Events=extras.getString("Events");
            String Body=extras.getString("Body");
            Msg message=new Msg(Title,Body,Events);
            addMsg(message);*/


        });
    }
}