package timerx.format;

import androidx.annotation.NonNull;
import timerx.TimeUnit;

public class Semantic {

  Position hoursPosition;
  Position minutesPosition;
  Position secondsPosition;
  Position rMillisPosition;
  TimeUnit smallestAvailableUnit;

  String format;
  String strippedFormat;

  Semantic(@NonNull Position hoursPosition, @NonNull Position minutesPosition,
      @NonNull Position secondsPosition, @NonNull Position rMillisPosition,
      String format, String strippedFormat,
      TimeUnit smallestAvailableUnit) {
    this.hoursPosition = hoursPosition;
    this.minutesPosition = minutesPosition;
    this.secondsPosition = secondsPosition;
    this.rMillisPosition = rMillisPosition;
    this.format = format;
    this.strippedFormat = strippedFormat;
    this.smallestAvailableUnit = smallestAvailableUnit;
  }

  public String getFormat() {
    return strippedFormat;
  }

  boolean has(TimeUnit unitType) {
    switch (unitType) {
      case HOURS:
        return hoursPosition.isNotEmpty();
      case MINUTES:
        return minutesPosition.isNotEmpty();
      case SECONDS:
        return secondsPosition.isNotEmpty();
      case R_MILLISECONDS:
        return rMillisPosition.isNotEmpty();
    }
    throw new IllegalArgumentException("Incorrect type of unit");
  }

  boolean hasOnlyRMillis() {
    return (rMillisPosition.isNotEmpty() && secondsPosition.isEmpty()
        && minutesPosition.isEmpty() && hoursPosition.isEmpty());
  }
}