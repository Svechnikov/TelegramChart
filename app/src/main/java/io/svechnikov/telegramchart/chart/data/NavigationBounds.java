package io.svechnikov.telegramchart.chart.data;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationBounds implements Parcelable {

    public final float left;
    public final float right;

    public NavigationBounds(Parcel parcel) {
        left = parcel.readFloat();
        right = parcel.readFloat();
    }

    public NavigationBounds(float left,
                            float right) {
        this.left = left;
        this.right = right;
    }

    public float getWidth() {
        return right - left + 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(left);
        dest.writeFloat(right);
    }

    @Override
    public String toString() {
        return "NavigationBounds{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }

    public static final Parcelable.Creator<NavigationBounds> CREATOR =
            new Parcelable.Creator<NavigationBounds>() {

                public NavigationBounds createFromParcel(Parcel in) {
                    return new NavigationBounds(in);
                }

                public NavigationBounds[] newArray(int size) {
                    return new NavigationBounds[size];
                }
            };
}
