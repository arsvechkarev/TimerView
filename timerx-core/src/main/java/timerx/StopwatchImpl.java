package timerx;

import static timerx.TimeCountingState.INACTIVE;
import static timerx.TimeCountingState.PAUSED;
import static timerx.TimeCountingState.RESUMED;
import static timerx.util.Checker.assertThat;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import timerx.format.Semantic;
import timerx.format.TimeFormatter;

/**
 * Stopwatch with base functions like {@link #start() start}, {@link #stop() stop}, etc.
 * Also allows to format time by specific pattern and executing actions at a certain time.
 * See {@link TimeFormatter} to find out how to user syntax. Use {@link StopwatchBuilder}
 * to configure and instantiate the stopwatch.
 * <p>Example of usage:</p>
 * <pre>
 *   Stopwatch stopwatch = new StopwatchBuilder()
 *         // Set start format of time
 *         .startFormat("SS:LL")
 *         // Set tick listener that receives formatted time
 *         .tickListener(time -> textViewTime.setText(time))
 *         // Executing the action at a certain time
 *         .actionWhen(30, TimeUnit.SECONDS, () -> {
 *            Toast.makeText(context, "30 seconds past!", Toast.LENGTH_SHORT).show();
 *         })
 *         // When time will be equal to one minute, change format to "MM:SS:LL"
 *         .changeFormatWhen(1, TimeUnit.MINUTES, "MM:SS:LL")
 *         .build();
 *
 *   stopwatch.start();
 *   stopwatch.stop();
 *   stopwatch.start();
 *   stopwatch.stop();
 *   long resultMillis = stopwatch.getTimeIn(TimeUnit.MILLISECONDS);
 *   stopwatch.reset();
 * </pre>
 *
 * @author Arseny Svechkarev
 * @see StopwatchBuilder
 * @see TimerImpl
 */
public class StopwatchImpl {

  // Message id for handler
  private static final int MSG = 2;

  // Current time of stopwatch (in millis)
  private long currentTime;

  // Time when stopwatch started (in millis)
  private long baseTime;

  /**
   * Delay for handler in millis
   */
  private long delay;

  /**
   * Listener for sending formatted time
   *
   * @see TimeTickListener
   */
  private final TimeTickListener tickListener;

  // Semantic of start format
  private final Semantic startSemantic;

  /**
   * Current state of stopwatch
   *
   * @see TimeCountingState
   */
  private TimeCountingState state = INACTIVE;

  /**
   * Formatter for formatting {@link #currentTime} according to particular format
   */
  private TimeFormatter timeFormatter;

  /**
   * Time formats to be applied in future
   */
  private final SortedSet<NextFormatsHolder> nextFormatsHolder;

  /**
   * Actions to execute in future
   */
  private final SortedSet<ActionsHolder> actionsHolder;

  private SortedSet<NextFormatsHolder> copyOfFormatsHolder;
  private SortedSet<ActionsHolder> copyOfActionsHolder;

  @RestrictTo(Scope.LIBRARY)
  StopwatchImpl(Semantic startSemantic, TimeTickListener tickListener,
      SortedSet<NextFormatsHolder> nextFormatsHolder,
      SortedSet<ActionsHolder> actionsHolder) {
    this.startSemantic = startSemantic;
    this.tickListener = tickListener;
    this.nextFormatsHolder = nextFormatsHolder;
    this.actionsHolder = actionsHolder;
  }

  /**
   * Returns start time (in this case, 0) formatted according to start format<br/> For
   * example, if start format is "MM:SS.LL", then result is "00:00.00"
   */
  public CharSequence getFormattedStartTime() {
    return new TimeFormatter(startSemantic).format(0L);
  }

  /**
   * Depending on state of stopwatch there are three possible variants of behaviour:
   * <p> - If stopwatch hasn't started yet or {@link #reset()} was called, then
   * stopwatch normally starts</p>
   * <p> - If stopwatch has been started, and method {@link #stop()} was called, then
   * stopwatch continues with previously saved time</p>
   * <p> - If stopwatch is running now, then the method does nothing</p>
   */
  public void start() {
    if (state != RESUMED) {
      if (state == INACTIVE) {
        copyOfFormatsHolder = new TreeSet<>(nextFormatsHolder);
        copyOfActionsHolder = new TreeSet<>(actionsHolder);
        applyFormat(startSemantic);
        baseTime = SystemClock.elapsedRealtime();
      } else {
        assertThat(state == PAUSED);
        baseTime = SystemClock.elapsedRealtime() - currentTime;
      }
      handler.sendEmptyMessage(MSG);
      state = RESUMED;
    }
  }

  /**
   * Stops stopwatch if it is active
   */
  public void stop() {
    state = PAUSED;
    handler.removeMessages(MSG);
  }

  /**
   * Stops stopwatch and resets time to zero
   */
  public void reset() {
    currentTime = 0;
    baseTime = 0;
    state = INACTIVE;
    handler.removeMessages(MSG);
  }

  /**
   * Returns current time of stopwatch converted to specified time unit.<br/> Example:
   */
  public long getTimeIn(TimeUnit timeUnit) {
    return timeUnit.convert(currentTime, TimeUnit.MILLISECONDS);
  }

  private void applyFormat(Semantic semantic) {
    timeFormatter = new TimeFormatter(semantic);
    delay = timeFormatter.getOptimizedDelay();
  }

  @SuppressLint("HandlerLeak")
  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      synchronized (StopwatchImpl.this) {
        long executionStartedTime = SystemClock.elapsedRealtime();
        currentTime = SystemClock.elapsedRealtime() - baseTime;
        changeFormatIfNeeded();
        notifyActionIfNeeded();
        if (tickListener != null) {
          CharSequence format = timeFormatter.format(currentTime);
          tickListener.onTick(format);
        }
        long executionDelay = SystemClock.elapsedRealtime() - executionStartedTime;
        sendEmptyMessageDelayed(MSG, delay - executionDelay);
      }
    }
  };

  private void notifyActionIfNeeded() {
    if (copyOfActionsHolder.size() > 0
        && currentTime >= copyOfActionsHolder.first().getMillis()) {
      copyOfActionsHolder.first().getAction().run();
      copyOfActionsHolder.remove(copyOfActionsHolder.first());
    }
  }

  private void changeFormatIfNeeded() {
    if (copyOfFormatsHolder.size() > 0
        && !timeFormatter.currentFormat()
        .equals(copyOfFormatsHolder.first().getSemantic().getFormat())
        && currentTime >= copyOfFormatsHolder.first().getMillis()) {
      applyFormat(copyOfFormatsHolder.first().getSemantic());
      copyOfFormatsHolder.remove(copyOfFormatsHolder.first());
    }
  }
}