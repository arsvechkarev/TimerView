package timerx

import androidx.annotation.UiThread
import timerx.Constants.TimeValues
import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.TimeUnit

/**
 * Builder to create an instance of [Timer]. Use function [buildTimer] to instantiate
 * the builder. Usage example:
 * <pre>
 *
 * val timer = buildTimer {
 *    // Set the start format of timer
 *    startFormat("MM:SS")
 *    // Set the start time of the timer
 *    startTime(60, TimeUnit.SECONDS)
 *    // Set the tick listener that gets notified when time changes
 *    onTick { millis: Long, time: CharSequence -> myTextView.text = time }
 *    // Run actions at a certain time
 *    actionWhen(40, TimeUnit.SECONDS) { showToast("40 seconds left") }
 *    actionWhen(20, TimeUnit.SECONDS) { showToast("20 seconds left") }
 *    // When time is equal to ten seconds, change format to "SS:LL"
 *    changeFormatWhen(10, TimeUnit.SECONDS, "SS:LL")
 * }
 *
 * // You can set formatted start time to TextView before timer started
 * myTextView.text = timer.formattedStartTime
 *
 * // Start timer
 * timer.start();
 *
 * // Get current time in milliseconds
 * long currentTime = timer.currentTimeInMillis
 *
 * // You can change time of timer using [Timer.setTime] method. Time will be changed whether timer
 * // is running or not. All formats will be applied accordingly
 * timer.setTime(20, TimeUnit.SECONDS)
 *
 * // When timer is not needed anymore
 * timer.release()
 *
 * @see Timer
 */
public class TimerBuilder internal constructor() {
  
  private var startSemantic: Semantic? = null
  private var startTime: Long = TimeValues.NONE
  private var tickListener: TimeTickListener? = null
  private var finishAction: Runnable? = null
  private val semanticsHolders: SortedSet<SemanticsHolder> = TreeSet(Collections.reverseOrder())
  private val actionsHolders: SortedSet<ActionsHolder> = TreeSet(Collections.reverseOrder())
  private var useExactDelay = false
  
  /**
   * Set the start format to timer
   *
   * @param format Format for timer. See [TimeFormatter] to find out about formats
   */
  public fun startFormat(format: String) {
    startSemantic = Analyzer.analyze(format)
  }
  
  /**
   * Set the start time to timer
   *
   * @param time Time to set
   * @param timeUnit Unit of the time
   */
  public fun startTime(time: Long, timeUnit: TimeUnit) {
    require(time >= 0) { "Time shouldn't be negative" }
    startTime = timeUnit.toMillis(time)
  }
  
  /**
   * Set tick listener to receive formatted time
   */
  public fun onTick(tickListener: TimeTickListener) {
    this.tickListener = tickListener
  }
  
  /**
   * Set tick listener to be notified when timer ends
   *
   * @param finishAction Action that fires up when timer reaches 0
   */
  public fun onFinish(finishAction: Runnable) {
    this.finishAction = finishAction
  }
  
  /**
   * Schedules changing format at a certain time. Format is applied as soon as timer
   * reaches given time. This method can be called many times, all received formats will
   * be scheduled. When called with the same time, only first invocation is scheduled.
   * Examples:
   * <pre>
   * val timer = buildTimer {
   *    // When the time reaches 1 minute, then format will be changed to "M:SS:LL"
   *    changeFormatWhen(1, TimeUnits.MINUTES, "M:SS:LL")
   *    // When the time reaches 10 minutes, then format will be changed to "MM:SS:LL"
   *    changeFormatWhen(10, TimeUnits.MINUTES, "MM:SS:LL")
   * }
   * </pre>
   */
  public fun changeFormatWhen(time: Long, timeUnit: TimeUnit, newFormat: String) {
    require(time >= 0) { "Time cannot be negative" }
    val semantic = Analyzer.analyze(newFormat)
    val millis = timeUnit.toMillis(time)
    semanticsHolders.add(SemanticsHolder(millis, semantic))
  }
  
  /**
   * Like [changeFormatWhen], but schedules an action at a certain time.
   * Example:
   * <pre>
   *  val timer = buildTimer {
   *     // When the remaining time is equal to 1 minute, show toast
   *     actionWhen(1, TimeUnit.MINUTES) { showToast("1 minute left") }
   *  }
   * </pre>
   */
  public fun actionWhen(time: Long, timeUnit: TimeUnit, action: Runnable) {
    require(time >= 0) { "Time cannot be negative" }
    val millis = timeUnit.toMillis(time)
    actionsHolders.add(ActionsHolder(millis, action))
  }
  
  /**
   * Determines whether timer should have precise delays between ticks or not.
   *
   * If flag is set to _true_ then delay between ticks will happen according to format.
   * For example if current format on timer is "MM:SS" delay between onTick() invocations
   * will be exactly 1 second, if format is "HH:MM" - exactly one minute, "SS:LL" - exactly 10
   * milliseconds and so on
   *
   * If, however, this flag is set to _false_ then delay between ticks might be less than exact
   * time. For example, if format is "MM:SS", delay between ticks might be less than seconds, like
   * 100 milliseconds. In this case [TimeTickListener.onTick] will be called multiple times per
   * second with **same** formatted time, but **different** milliseconds
   *
   * By default this flag is set to false and generally you don't have to worry about it. If you
   * are just displaying formatted time in TextView and you want to have good precision then leave
   * this flag as false. You need to set it to true if you want [TimeTickListener.onTick] method to
   * be called called exactly between delays specified by your format
   *
   * @param [useExactDelay] Whether timer should use exact delays or not. Default is false
   */
  public fun useExactDelay(useExactDelay: Boolean) {
    this.useExactDelay = useExactDelay
  }
  
  /**
   * Creates and returns timer instance
   */
  internal fun build(): Timer {
    val startSemantic = startSemantic ?: error("Start format is not provided. Call" +
        " startFormat(String) to provide initial format")
    require(startTime != TimeValues.NONE) { "Start time is not provided" }
    semanticsHolders.add(SemanticsHolder(startTime, startSemantic))
    return TimerImpl(useExactDelay, tickListener, finishAction, semanticsHolders.toMutableList(),
      actionsHolders.toMutableList())
  }
}

/**
 * Use this method to create and configure an instance of timer. Note that you should call
 * this method either on UI thread or on a thread that has its own Looper
 *
 * @see TimerBuilder
 */
@UiThread
public fun buildTimer(action: TimerBuilder.() -> Unit): Timer {
  return TimerBuilder().apply(action).build()
}