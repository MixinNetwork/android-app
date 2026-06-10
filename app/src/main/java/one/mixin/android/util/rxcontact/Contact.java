package one.mixin.android.util.rxcontact;


import android.net.Uri;

import java.util.HashSet;
import java.util.Set;


public class Contact implements Comparable<Contact> {
    private final long mId;
    private int mInVisibleGroup;
    private String mDisplayName;
    private boolean mStarred;
    private Uri mPhoto;
    private Uri mThumbnail;
    private Set<String> mEmails = new HashSet<>();
    private Set<String> mPhoneNumbers = new HashSet<>();

    Contact(long id) {
        this.mId = id;
    }

    public long getId() {
        return mId;
    }

    public int getInVisibleGroup() {
        return mInVisibleGroup;
    }

    public void setInVisibleGroup(int inVisibleGroup) {
        mInVisibleGroup = inVisibleGroup;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public boolean isStarred() {
        return mStarred;
    }

    public void setStarred(boolean starred) {
        mStarred = starred;
    }

    public Uri getPhoto() {
        return mPhoto;
    }

    public void setPhoto(Uri photo) {
        mPhoto = photo;
    }

    public Uri getThumbnail() {
        return mThumbnail;
    }

    public void setThumbnail(Uri thumbnail) {
        mThumbnail = thumbnail;
    }

    public Set<String> getEmails() {
        return mEmails;
    }

    public void setEmails(Set<String> emails) {
        mEmails = emails;
    }

    public Set<String> getPhoneNumbers() {
        return mPhoneNumbers;
    }

    public void setPhoneNumbers(Set<String> phoneNumbers) {
        mPhoneNumbers = phoneNumbers;
    }


    @Override
    public int compareTo(Contact other) {
        if(mDisplayName != null && other.mDisplayName != null)
            return mDisplayName.compareTo(other.mDisplayName);
        else return Long.compare(mId, other.mId);
    }

    @Override
    public int hashCode () {
        return (int) (mId ^ (mId >>> 32));
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Contact contact = (Contact) o;
        return mId == contact.mId;
    }
}
