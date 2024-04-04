package com.example.attendease;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsAttendeeCheckInFromQRScannerTest {
    Attendee testAttendee = new Attendee("testDevice", "name", "phone", "email", "image", false);
    @Rule
    public ActivityScenarioRule<EventDetailsAttendee> scenario = new ActivityScenarioRule<>(new Intent(ApplicationProvider.getApplicationContext(), EventDetailsAttendee.class)
            .putExtra("attendee", testAttendee)
            .putExtra("eventID", "testEvent")
            .putExtra("prevActivity", "QRScanncerActivity")
            .putExtra("title","Teset Event")
            .putExtra("description","Event for testing purposes")
            .putExtra("dateTime", "March 6, 2024 at 2:24:11")
            .putExtra("location","tester spot")
            .putExtra("canCheckIn", true));

    @Test
    public void testCheckIn() throws InterruptedException {
        onView(withId(R.id.signup_or_checkin)).check(matches(withText("Check In")));
        onView(withId(R.id.signup_or_checkin)).perform(ViewActions.click());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference checkInsRef = db.collection("checkIns");

        String deviceID = "testDevice";

        final CountDownLatch latch = new CountDownLatch(1);

        checkInsRef
                .whereEqualTo("attendeeID", deviceID)
                .whereEqualTo("eventID", "testEvent")
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
                .whereEqualTo("eventID", "testEvent")
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
    }

}