/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client.ui.preferencesdialog.hardware;

import java.awt.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jspecify.annotations.NonNull;

/**
 * A class that contains utility methods that interact with app preferences and the graphic
 * environment to store and retrieve information about hardware.
 */
public class HardwareTabUtils {
  private static final Logger log = LogManager.getLogger(HardwareTabUtils.class);

  /** this is a utility class with static methods. */
  private HardwareTabUtils() {}

  /**
   * Information about display preferences.
   *
   * <p>Constructors throw IllegalArgumentException if:
   *
   * <ul>
   *   <li>detectedWidth <= 0
   *   <li>detectedHeight <= 0
   * </ul>
   *
   * <p>If the aspect ratio is <=0, then it is calculated from the detected width and height.
   *
   * @param idString From {@link GraphicsDevice}
   * @param detectedX From {@link GraphicsDevice}
   * @param detectedY From {@link GraphicsDevice}
   * @param detectedWidth From {@link GraphicsDevice}
   * @param detectedHeight From {@link GraphicsDevice}
   * @param detectedGcd This value is ignored, the detected cgd is calculated from the detected
   *     width and height
   * @param isCurrentlyConnected false if this is a value stored in preferences matching no
   *     connected device
   * @param useAspectRatioCorrection From user preferences.
   * @param fullscreenOnly From user preferences.
   * @param aspectX From user preferences. if <=0, this is calculated from the detected width and
   *     height.
   * @param aspectY From user preferences. if <=0, this is calculated from the detected width and
   *     height.
   */
  public record DisplayInfo(
      String idString,
      int detectedX,
      int detectedY,
      int detectedWidth,
      int detectedHeight,
      int detectedGcd,
      boolean isCurrentlyConnected,
      boolean useAspectRatioCorrection,
      boolean fullscreenOnly,
      int aspectX,
      int aspectY) {

    // todo - get id string from resources
    public static final DisplayInfo NO_SCREEN =
        new DisplayInfo("No Screen", 0, 0, 1, 1, false, false, false, 1, 1);

    /**
     * In this constructor, the passed value of detectedGcd is ignored - it is calculated from the
     * detected width and height
     */
    public DisplayInfo {
      if (idString == null) {
        idString = "";
      }

      if (detectedWidth <= 0) {
        throw new IllegalArgumentException("detected width must not be <= 0");
      }
      if (detectedHeight <= 0) {
        throw new IllegalArgumentException("detected height must not be <= 0");
      }

      detectedGcd =
          BigInteger.valueOf(detectedWidth).gcd(BigInteger.valueOf(detectedHeight)).intValue();

      if (aspectX <= 0) {
        aspectX = detectedWidth / detectedGcd;
      }
      if (aspectY <= 0) {
        aspectY = detectedHeight / detectedGcd;
      }
    }

    public DisplayInfo(
        String idString,
        int detectedX,
        int detectedY,
        int detectedWidth,
        int detectedHeight,
        boolean isCurrentlyConnected,
        boolean useAspectRatioCorrection,
        boolean fullscreenOnly,
        int aspectX,
        int aspectY) {
      this(
          idString,
          detectedX,
          detectedY,
          detectedWidth,
          detectedHeight,
          1, // passing 1 as detectedGcd, because the default constructor recalculates it.
          isCurrentlyConnected,
          useAspectRatioCorrection,
          fullscreenOnly,
          aspectX,
          aspectY);
    }

    public int detectedAspectX() {
      return detectedWidth / detectedGcd;
    }

    public int detectedAspectY() {
      return detectedHeight / detectedGcd;
    }

    public @NonNull String toString() {
      return "[\""
          + idString
          + "\" "
          + detectedWidth
          + "\u00D7"
          + detectedHeight
          + " @ "
          + detectedX
          + ","
          + detectedY
          + " - "
          + detectedAspectX()
          + ":"
          + detectedAspectY()
          + "]";
    }

    public boolean samePositionAs(Rectangle bounds) {
      return bounds.x == detectedX && bounds.y == detectedY;
    }

    public boolean sameBoundsAs(Rectangle bounds) {
      return bounds.x == detectedX
          && bounds.y == detectedY
          && bounds.width == detectedWidth
          && bounds.height == detectedHeight;
    }
  }

  public interface KnownDisplaysListener extends EventListener {
    /**
     * The list of known displays has been updated. The list potentially has been reordered.
     * Consumers should use {@link #matchDisplay(GraphicsDevice, List)} to work out which display in
     * thier old list corresponds to which in the new current list, or should just relaod
     * themselves.
     */
    void displayListChanged();

    /**
     * One particular display in the list has had its settings changed.
     *
     * @param n the list item that has been updated.
     */
    void displayChanged(int n);

    /**
     * The awt graphics environment is currently being queried for display information on another
     * thread.
     *
     * <p>Listeners may wish to disable their 'redetect displays' buttons;
     *
     * @param isUpdating
     */
    void displaysListBeingUpdated(boolean isUpdating);
  }

  static final EventListenerList listeners = new EventListenerList();

  /**
   * Adds a listener to the list of listeners that are notified when the list of known displays
   * updates.
   *
   * <p><strong>Memory Leak warning:</strong> The list of listeners is static. Make certain to
   * remove listeners that are added on component initailization.
   */
  public static void addKnownDisplaysListener(KnownDisplaysListener listener) {
    listeners.add(KnownDisplaysListener.class, listener);
  }

  public static void removeKnownDisplaysListener(KnownDisplaysListener listener) {
    listeners.remove(KnownDisplaysListener.class, listener);
  }

  private static void fireDisplayListChanged() {
    for (KnownDisplaysListener listener : listeners.getListeners(KnownDisplaysListener.class)) {
      listener.displayListChanged();
    }
  }

  private static void fireDisplayChanged(int n) {
    for (KnownDisplaysListener listener : listeners.getListeners(KnownDisplaysListener.class)) {
      listener.displayChanged(n);
    }
  }

  private static void fireDisplaysListBeingUpdated(boolean isUpdating) {
    for (KnownDisplaysListener listener : listeners.getListeners(KnownDisplaysListener.class)) {
      listener.displaysListBeingUpdated(isUpdating);
    }
  }

  /**
   * The currently detected raster displays. This is held in memory because we reference it each
   * time a map window is moved or made fullscreen. This means that it needs to be populated on
   * application startup. TODO: find out how to do something on application startup.
   */
  static List<DisplayInfo> detectedDispayList = Collections.emptyList();

  /** This is not volatile, because it is only updated on the swing thread. */
  private static Future<?> currentlyExecutingGraphicsDetection = null;

  /**
   * Is the known displays list currently being updated on another thread?
   *
   * @return
   */
  public static boolean isDisplaysListBeingUpdated() {
    return currentlyExecutingGraphicsDetection != null;
  }

  /**
   * Detects connected displays by querying the GraphicsEnvironment.
   *
   * <p>This method expects to be called in the swing thread and starts a new thread to query the
   * graphics environment. It rejoins the swing thread to update the preferences and notify
   * listeners.
   *
   * <p>KnownDisplaysListeners on the listener list will be notified <strong>if</strong> the list
   * has changed.
   */
  public static void detectDisplays() {
    if (currentlyExecutingGraphicsDetection != null) return;

    try (var executor = Executors.newSingleThreadExecutor()) {
      fireDisplaysListBeingUpdated(true);

      currentlyExecutingGraphicsDetection =
          executor.submit(
              () -> {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                final GraphicsDevice[] devices = ge.getScreenDevices();
                SwingUtilities.invokeLater(
                    () -> {
                      boolean foundChanges = gotScreenDevices(devices);
                      if (foundChanges) {
                        fireDisplayListChanged();
                      }
                      currentlyExecutingGraphicsDetection = null;
                      fireDisplaysListBeingUpdated(false);
                    });
              });
    } catch (RejectedExecutionException e) {
      currentlyExecutingGraphicsDetection = null;
      fireDisplaysListBeingUpdated(false);
    }
  }

  // this method is executed on the swing thread
  private static boolean gotScreenDevices(GraphicsDevice[] devices) {
    // make a copy of the currently saved list, as we will be modifying it.
    List<DisplayInfo> savedList = new ArrayList<>(loadDisplaysFromAppPreferences());

    ArrayList<DisplayInfo> detected = new ArrayList<>();
    boolean foundChanges = false;

    for (GraphicsDevice device : devices) {
      if (device.getType() != GraphicsDevice.TYPE_RASTER_SCREEN) {
        continue;
      }

      final Rectangle bounds = device.getDefaultConfiguration().getBounds();

      OptionalInt savedIndex = matchDisplay(device, savedList);

      Optional<DisplayInfo> saved = savedIndex.stream().mapToObj(savedList::get).findFirst();

      DisplayInfo updatedInfo =
          new DisplayInfo(
              device.getIDstring(),
              bounds.x,
              bounds.y,
              bounds.width,
              bounds.height,
              true,
              saved.map(DisplayInfo::useAspectRatioCorrection).orElse(false),
              saved.map(DisplayInfo::fullscreenOnly).orElse(false),
              // if the aspect is 0:0, the constructor will calculate it
              saved.map(DisplayInfo::aspectX).orElse(0),
              saved.map(DisplayInfo::aspectY).orElse(0));

      detected.add(updatedInfo);

      foundChanges =
          foundChanges
              || saved
                  .map(
                      theSaved ->
                          !theSaved.idString.equals(updatedInfo.idString)
                              || theSaved.detectedX != updatedInfo.detectedX
                              || theSaved.detectedY != updatedInfo.detectedY
                              || theSaved.detectedWidth != updatedInfo.detectedWidth
                              || theSaved.detectedHeight != updatedInfo.detectedHeight)
                  .orElse(true);

      saved.ifPresent(savedList::remove);
    }

    // the entries remaining in the savedList are no longer detected, but this may be
    // because
    // the display is currently unplugged. we will show them as not currently connected

    savedList.stream()
        .map(
            saved ->
                new DisplayInfo(
                    saved.idString,
                    saved.detectedX,
                    saved.detectedY,
                    saved.detectedWidth,
                    saved.detectedHeight,
                    false,
                    saved.useAspectRatioCorrection,
                    saved.fullscreenOnly,
                    saved.aspectX,
                    saved.aspectY))
        .forEach(detected::add);

    saveDisplaysToAppPreferences(detected);

    detectedDispayList = List.copyOf(detected);

    return foundChanges;
  }

  // TODO: implement this
  private static List<DisplayInfo> loadDisplaysFromAppPreferences() {
    log.warn("loadDisplaysFromAppPreferences not yet implemented");
    return detectedDispayList;
  }

  // TODO: implement this
  private static void saveDisplaysToAppPreferences(List<DisplayInfo> detected) {
    log.warn("saveDisplaysToAppPreferences not yet implemented");
  }

  /**
   * The list of known displays - both saved in preferences and currently attached. This list is an
   * unmodifiable copy of an internal list.
   *
   * @return and Unmodifiable list
   */
  public static List<DisplayInfo> getKnownDisplays() {
    return detectedDispayList;
  }

  /**
   * Updates the user-settable properties of a display and informs listeners of the change.
   *
   * @param displayIndex the index of the display to update
   * @param useAspectRatioCorrection user setting
   * @param fullscreenOnly user setting
   * @param aspectX user setting
   * @param aspectY user setting
   */
  public static void updateDisplayAspectCorrectionPreferences(
      int displayIndex,
      boolean useAspectRatioCorrection,
      boolean fullscreenOnly,
      int aspectX,
      int aspectY) {
    DisplayInfo display = getKnownDisplays().get(displayIndex);

    DisplayInfo newDisplay =
        new DisplayInfo(
            display.idString,
            display.detectedX,
            display.detectedY,
            display.detectedWidth,
            display.detectedHeight,
            display.isCurrentlyConnected,
            useAspectRatioCorrection,
            fullscreenOnly,
            aspectX,
            aspectY);

    updateDisplayPreferences(displayIndex, newDisplay);
  }

  private static void updateDisplayPreferences(int displayIndex, DisplayInfo updateTo) {
    // TODO: implement saving the updated preferences
    log.warn("saving the updated preferences not yet implemented");

    ArrayList<DisplayInfo> newList = new ArrayList<>(detectedDispayList);
    newList.set(displayIndex, updateTo);
    detectedDispayList = List.copyOf(newList);

    fireDisplayChanged(displayIndex);
  }

  /**
   * Find the display currently at point x,y
   *
   * @return the display info, or null if no display is found.
   */
  public static DisplayInfo findDispayAt(int x, int y) {
    return getKnownDisplays().stream()
        .filter(
            d ->
                x >= d.detectedX
                    && x < d.detectedX + d.detectedWidth
                    && y >= d.detectedY
                    && y < d.detectedY + d.detectedHeight)
        .findFirst()
        .orElse(null);
  }

  @FunctionalInterface
  private interface DisplayMatcher {
    boolean matches(GraphicsDevice find, DisplayInfo info);
  }

  /**
   * A series of display matchers in order of best "fit". These are used by {@link
   * #matchDisplay(GraphicsDevice, List)} until a unique match is found
   */
  private static final DisplayMatcher[] DISPLAY_MATCHERS_SEQUENCE = {
    (find, info) -> info.idString.equals(find.getIDstring()),
    (find, info) ->
        info.idString.equals(find.getIDstring())
            && info.samePositionAs(find.getDefaultConfiguration().getBounds()),
    (find, info) ->
        info.idString.equals(find.getIDstring())
            && info.sameBoundsAs(find.getDefaultConfiguration().getBounds()),
    (find, info) -> info.samePositionAs(find.getDefaultConfiguration().getBounds()),
    (find, info) -> info.sameBoundsAs(find.getDefaultConfiguration().getBounds())
  };

  /**
   * Find the best match of a display in a list. We try the predicates in DISPLAY_MATCHERS_SEQUENCE,
   * first looking for a unique match, then looking for a first match.
   *
   * @param find The display that we are looking for.
   * @param list The list of displays to search.
   * @return the index of the match, or -1 if no match found
   */
  public static OptionalInt matchDisplay(final GraphicsDevice find, final List<DisplayInfo> list) {
    // find the first unique match

    OptionalInt findUnique =
        Arrays.stream(DISPLAY_MATCHERS_SEQUENCE)
            .map(
                matcher -> {
                  // we run through the list twice, but this method is not heavily used so
                  // optimisation is not required

                  OptionalInt first =
                      IntStream.range(0, list.size())
                          .filter(i -> matcher.matches(find, list.get(i)))
                          .findFirst();

                  OptionalInt second =
                      IntStream.range(0, list.size())
                          .filter(i -> matcher.matches(find, list.get(i)))
                          .skip(1)
                          .findFirst();

                  if (first.isPresent() && second.isEmpty()) {
                    return first;
                  } else {
                    return OptionalInt.empty();
                  }
                })
            .flatMapToInt(OptionalInt::stream)
            .findFirst();

    if (findUnique.isPresent()) return findUnique;

    // find any match

    return Arrays.stream(DISPLAY_MATCHERS_SEQUENCE)
        .map(
            matcher ->
                IntStream.range(0, list.size())
                    .filter(i -> matcher.matches(find, list.get(i)))
                    .findFirst())
        .flatMapToInt(OptionalInt::stream)
        .findFirst();
  }
}
