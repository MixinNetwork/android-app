package one.mixin.android.util.rxcontact;


import android.database.Cursor;
import android.net.Uri;

class ColumnMapper {

    // Utility class -> No instances allowed
    private ColumnMapper () {}

    static void mapInVisibleGroup (Cursor cursor, Contact contact, int columnIndex) {
        contact.setInVisibleGroup(cursor.getInt(columnIndex));
    }

    static void mapDisplayName (Cursor cursor, Contact contact, int columnIndex) {
        String displayName = cursor.getString(columnIndex);
        if (displayName != null && !displayName.isEmpty()) {
            contact.setDisplayName(displayName);
        }
    }

    static void mapEmail (Cursor cursor, Contact contact, int columnIndex) {
        String email = cursor.getString(columnIndex);
        if (email != null && !email.isEmpty()) {
            contact.getEmails().add(email);
        }
    }

    static void mapPhoneNumber (Cursor cursor, Contact contact, int columnIndex) {
        String phoneNumber = cursor.getString(columnIndex);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // Remove all whitespaces
            phoneNumber = phoneNumber.replaceAll("\\s+","");
            contact.getPhoneNumbers().add(phoneNumber);
        }
    }

    static void mapPhoto (Cursor cursor, Contact contact, int columnIndex) {
        String uri = cursor.getString(columnIndex);
        if (uri != null && !uri.isEmpty()) {
            contact.setPhoto(Uri.parse(uri));
        }
    }

    static void mapStarred (Cursor cursor, Contact contact, int columnIndex) {
        contact.setStarred(cursor.getInt(columnIndex) != 0);
    }

    static void mapThumbnail (Cursor cursor, Contact contact, int columnIndex) {
        String uri = cursor.getString(columnIndex);
        if (uri != null && !uri.isEmpty()) {
            contact.setThumbnail( Uri.parse(uri));
        }
    }
}
