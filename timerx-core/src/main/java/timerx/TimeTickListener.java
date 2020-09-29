package timerx;

import androidx.annotation.NonNull;

/**
 * Tick listener that receives formatted time
 */
public interface TimeTickListener {

  void onTick(@NonNull CharSequence time);
}
