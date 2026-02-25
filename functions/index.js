const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendFamilyRequestNotification = functions.firestore
    .document("notifications/{id}")
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        const receiverId = data.receiverId;

        const userDoc = await admin.firestore()
            .collection("users")
            .doc(receiverId)
            .get();

        if (!userDoc.exists) return;

        const fcmToken = userDoc.get("fcmToken");
        if (!fcmToken) return;

        const payload = {
            notification: {
                title: `${data.senderName} sent you a ${data.relationRequested} request`,
                body: "Tap to respond",
                image: data.senderPhoto
            },
            data: {
                notificationId: data.id,
                senderId: data.senderId,
                type: "family_request"
            },
        };

        return admin.messaging().sendToDevice(fcmToken, payload);
    });
