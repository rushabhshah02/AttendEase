package com.example.attendease;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsAttendeeTest {
    Attendee testAttendee = new Attendee("testDevice", "name", "phone", "email", "image", false);
    CountingIdlingResource countingIdlingResource;

    @Rule
    public ActivityScenarioRule<EventDetailsAttendee> scenario = new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), EventDetailsAttendee.class)
            .putExtra("attendee", testAttendee)
            .putExtra("eventID", "bd7145c0-2fdb-4796-8098-49d41feb9ed2_1712533095391")
            .putExtra("prevActivity", "QRScannerActivity")
            .putExtra("title","Teset Event")
            .putExtra("description","Event for testing purposes")
            .putExtra("dateTime", "March 6, 2024 at 2:24:11")
            .putExtra("location","tester spot")
            .putExtra("posterUrl", "null")
            .putExtra("canCheckIn", true));

    @Before
    public void setup() {
        Intents.init();
        scenario.getScenario().onActivity(activity -> {
            countingIdlingResource = activity.getCountingIdlingResource();
        });
        IdlingRegistry.getInstance().register(countingIdlingResource);
    }

    @Test
    public void testCheckIn() throws InterruptedException {
//        Thread.sleep(2000);

        IdlingRegistry.getInstance().register(FirebaseLoadingTestHelper.getIdlingResource());

        onView(withId(R.id.signup_or_checkin)).perform(ViewActions.click());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference checkInsRef = db.collection("checkIns");

        String deviceID = "testDevice";

        final CountDownLatch latch = new CountDownLatch(1);

        checkInsRef
                .whereEqualTo("attendeeID", deviceID)
                .whereEqualTo("eventID", "bd7145c0-2fdb-4796-8098-49d41feb9ed2_1712533095391")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Signal that the query has completed
                            latch.countDown();
                        }
                    }
                });
        latch.await(); // Make sure to await the latch countdown

        checkInsRef.whereEqualTo("attendeeID", deviceID)
                .whereEqualTo("eventID", "bd7145c0-2fdb-4796-8098-49d41feb9ed2_1712533095391")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // Iterate through the documents and delete each one
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().delete();
                        }
                    }
                });
    }
    @After
    public void tearDown() throws Exception {
        // Code to delete test data from Firestore
        Intents.release();
    }

}
