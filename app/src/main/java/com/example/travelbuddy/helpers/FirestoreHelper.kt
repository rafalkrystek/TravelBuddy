package com.example.travelbuddy.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference

fun FirebaseFirestore.getTripDocument(tripId: String): DocumentReference {
    return collection("trips").document(tripId)
}

