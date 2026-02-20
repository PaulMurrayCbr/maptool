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
package net.rptools.maptool.client.ui.preferencesdialog;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 * A class that contains utility methods that interact with app preferences to store and retrieve
 * information about hardware.
 */
public class HardwareTabUtils {

  public static record DisplayInfo(
      String idString,
      int detectedX,
      int detectedY,
      int detectedWidth,
      int detectedHeight,
      boolean isCurrentlyConnected,
      boolean useAspectRatioCorrection,
      int actualWidth,
      int actualHeight) {
    public DisplayInfo {
      if (detectedWidth <= 0) {
        throw new IllegalArgumentException("detected width must not be <= 0");
      }
      if (detectedHeight <= 0) {
        throw new IllegalArgumentException("detected height must not be <= 0");
      }
      if (actualWidth() <= 0) {
        actualWidth = detectedWidth;
      }
      if (actualHeight <= 0) {
        actualHeight = detectedHeight;
      }
    }

    public String toString() {
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
          + "]";
    }
  }

  /**
   * The currently detected raster displays. This is held in memory because we reference it each
   * time a map window is moved or made fullscreen. This means that it needs to be populated on
   * application startup. TODO: find out how to do something on application startup.
   */
  static List<DisplayInfo> detectedDispayList = Collections.emptyList();

  /**
   * Callbacks for things that are interested in changes to the list of detected displays.
   */
  static final EventListenerList detectedDisplayChangeListenerList = new EventListenerList();

  /**
   * Adapter for things that want to be informed when the list of displays is re-detected.
   * Typically used as a lambda.
   */
  @FunctionalInterface
  public interface DetectedDisplayChangeEventListener extends EventListener {
    /**
     * The display list has been updated. No information is passed, invoke {@link #getKnownDisplays()}
     * or {@link #findDispayAt(int, int)}  to retrieve the new items.
     */
    void displayChangeDetected();
  };

  /**
   * Registers a listener to be notified when the list of detected displays changes.
   * The listener will be invoked whenever a re-detection of displays occurs.
   *
   * @param listener the {@link DetectedDisplayChangeEventListener} to register.
   *                 Must not be null.
   */
  public static void addDetectedDisplayChangeListener(DetectedDisplayChangeEventListener listener) {
    detectedDisplayChangeListenerList.add(DetectedDisplayChangeEventListener.class, listener);
  }

  /**
   * Unregisters a listener from being notified when the list of detected displays changes.
   *
   * @param listener the {@link DetectedDisplayChangeEventListener} to unregister.
   *                 Must not be null.
   */
  public static void removeDetectedDisplayChangeListener(DetectedDisplayChangeEventListener listener) {
    detectedDisplayChangeListenerList.remove(DetectedDisplayChangeEventListener.class, listener);
  }

  private static void fireDetectedDisplayChange() {
    for(DetectedDisplayChangeEventListener listener : detectedDisplayChangeListenerList.getListeners(DetectedDisplayChangeEventListener.class)) {
      listener.displayChangeDetected();
    }
  }

  /**
   * Detects connected displays by querying the GraphicsEnvironment.
   *
   * <p>This method expects to be called in the swing thread, and starts a new thread to query the
   * graphics environment. It rejoins the swing thread to update the preferences and reply to the
   * caller.
   *
   * <p>The value returned to the consumer indicates if new display information was found. If new
   * display information was found, the caller can retrieve it by calling {@link
   * #getKnownDisplays()}.
   *
   * @param callback callback invoked when display detection has completed.
   * @return a future that can be used to check the status of the display detection. If the Future
   *     is cancelled, the callback will be invoked with false.
   */
  public static Future<?> detectDisplays(Consumer<Boolean> callback) {

    // make a copy of the currently saved list, as we will be modifying it.
    List<DisplayInfo> savedList = new ArrayList(loadDisplaysFromAppPreferences());

    try (var executor = Executors.newSingleThreadExecutor()) {
      return executor.submit(
          () -> {
            try {
              GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
              GraphicsDevice[] devices = ge.getScreenDevices();

              ArrayList<DisplayInfo> detected = new ArrayList<>();
              boolean foundChanges = false;

              for (int i = 0; i < devices.length; i++) {
                GraphicsDevice device = devices[i];

                if (device.getType() != GraphicsDevice.TYPE_RASTER_SCREEN) {
                  continue;
                }

                final Rectangle bounds = device.getDefaultConfiguration().getBounds();

                // if this detected device matches one of the currently saved devices, then
                // get its preference settings.
                // we match if the id matches, and if that doesn't work then we match if there's a
                // saved
                // device in exactly the same location. This hopefully catches the situation
                // where the device gets a different ID each time the app is started.

                Optional<DisplayInfo> saved =
                    savedList.stream()
                        .filter(d -> d.idString.equals(device.getIDstring()))
                        .findFirst()
                        .or(
                            () ->
                                savedList.stream()
                                    .filter(
                                        d ->
                                            d.detectedX == bounds.x
                                                && d.detectedY == bounds.y
                                                && d.detectedWidth == bounds.width
                                                && d.detectedHeight == bounds.height)
                                    .findFirst());

                DisplayInfo updatedInfo =
                    new DisplayInfo(
                        device.getIDstring(),
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        true,
                        saved.map(DisplayInfo::useAspectRatioCorrection).orElse(false),
                        saved.map(DisplayInfo::actualWidth).orElse(bounds.width),
                        saved.map(DisplayInfo::actualHeight).orElse(bounds.height));

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
                                        || theSaved.detectedHeight != updatedInfo.detectedHeight
                                        || !theSaved.isCurrentlyConnected
                            )
                            .orElse(true);

                saved.ifPresent(savedList::remove);
              }

              // the entries remaining in the savedList are no longer detected, but this may be
              // because
              // the display is currently unplugged. we will show them as not currently connected

              // if any of the saved list items are saved as 'connected', then this is a change
              if(savedList.stream().anyMatch(DisplayInfo::isCurrentlyConnected)) {
                foundChanges = true;
              }

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
                              saved.actualWidth,
                              saved.actualHeight))
                  .forEach(detected::add);

              saveDisplaysToAppPreferences(detected);

              detectedDispayList = List.copyOf(detected);

              if( foundChanges) {
                SwingUtilities.invokeLater(HardwareTabUtils::fireDetectedDisplayChange);
              }

              final boolean finalFoundChanges = foundChanges;
              SwingUtilities.invokeLater(() -> callback.accept(finalFoundChanges));
            } catch (Exception e) {
              SwingUtilities.invokeLater(() -> callback.accept(false));
            }
          });
    }
  }

  private static List<DisplayInfo> loadDisplaysFromAppPreferences() {
    return detectedDispayList;
  }

  private static void saveDisplaysToAppPreferences(List<DisplayInfo> detected) {}

  public static List<DisplayInfo> getKnownDisplays() {
    return detectedDispayList;
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
}
