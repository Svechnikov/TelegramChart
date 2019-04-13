package io.svechnikov.telegramchart.chart.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

import timber.log.Timber;

public class ChartViewState implements Parcelable {

    public final NavigationBounds navigationBounds;
    public final boolean[] entityVisibility;
    public final int selectedPointIndex;

    public ChartViewState(NavigationBounds bounds,
                          boolean[] entityVisibility,
                          int selectedPointIndex) {
        this.navigationBounds = bounds;
        this.entityVisibility = entityVisibility;
        this.selectedPointIndex = selectedPointIndex;
    }

    public ChartViewState(Parcel parcel) {
        navigationBounds = parcel.readParcelable(NavigationBounds.class.getClassLoader());
        entityVisibility = parcel.createBooleanArray();
        selectedPointIndex = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(navigationBounds, flags);
        dest.writeBooleanArray(entityVisibility);
        dest.writeInt(selectedPointIndex);
    }

    @Override
    public String toString() {
        return "ChartViewState{" +
                "navigationBounds=" + navigationBounds +
                ", entityVisibility=" + Arrays.toString(entityVisibility) +
                ", selectedPointIndex=" + selectedPointIndex +
                '}';
    }

    public static final Parcelable.Creator<ChartViewState> CREATOR =
            new Parcelable.Creator<ChartViewState>() {

                public ChartViewState createFromParcel(Parcel in) {
                    return new ChartViewState(in);
                }

                public ChartViewState[] newArray(int size) {
                    return new ChartViewState[size];
                }
            };
}
