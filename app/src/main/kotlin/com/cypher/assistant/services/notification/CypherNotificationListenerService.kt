package com.cypher.assistant.services.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification Listener Service - STUB.
 *
 * Declared in AndroidManifest.xml.
 * Requires user to grant access in: Settings -> Apps -> Special App Access -> Notification Access.
 *
 * Full implementation: Stage 12 (Notification feature module).
 *
 * When implemented, this service will:
 *  - Maintain a live list of active notifications.
 *  - Expose them to the NotificationHandler via a shared repository.
 *  - Support READ_NOTIFICATION and DISMISS_NOTIFICATION intents.
 */
class CypherNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // TODO Stage 12: cache notification in NotificationRepository
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // TODO Stage 12: remove from NotificationRepository
    }
}
