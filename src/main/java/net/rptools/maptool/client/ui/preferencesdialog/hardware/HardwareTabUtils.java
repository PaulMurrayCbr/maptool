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

import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * A class that contains utility methods that interact with app preferences and the graphic
 * environement to store and retrieve information about hardware.
 */
public class HardwareTabUtils {

  /**
   * this is a utility class with static methods.
   */
  private HardwareTabUtils() {
  }


  /**
   * Information about display preferences.
   * <p>Constriuctors throw IllegalArgumentException if:
   * <ul>
   *   <li>detectedWidth <= 0</li>
   *   <li>detectedHeight <= 0</li>
   * </ul>
   * <p>
   * If the aspect ratio is <=0, then it is calculated from the detected width and height.
   *
   * @param idString                 From {@link GraphicsDevice}
   * @param detectedX                From {@link GraphicsDevice}
   * @param detectedY                From {@link GraphicsDevice}
   * @param detectedWidth            From {@link GraphicsDevice}
   * @param detectedHeight           From  {@link GraphicsDevice}
   * @param detectedGcd              This value is ignored, the detected cgd is calculated from the detected width and height
   * @param isCurrentlyConnected     false if this is a value stored in preferences matching no connected device
   * @param useAspectRatioCorrection From user preferences.
   * @param fullscreenOnly           From user preferences.
   * @param aspectX                  From user preferences. if <=0, this is calculated from the detected width and height.
   * @param aspectY                  From user preferences. if <=0, this is calculated from the detected width and height.
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
      return bounds.x == detectedX && bounds.y == detectedY && bounds.width == detectedWidth && bounds.height == detectedHeight;
    }
  }

  /**
   * The currently detected raster displays. This is held in memory because we reference it each
   * time a map window is moved or made fullscreen. This means that it needs to be populated on
   * application startup. TODO: find out how to do something on application startup.
   */
  static List<DisplayInfo> detectedDispayList = Collections.emptyList();

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
   * is cancelled, the callback will be invoked with false.
   */
  public static Future<?> detectDisplays(Consumer<Boolean> callback) {

    // make a copy of the currently saved list, as we will be modifying it.
    List<DisplayInfo> savedList = new ArrayList<>(loadDisplaysFromAppPreferences());

    try (var executor = Executors.newSingleThreadExecutor()) {
      return executor.submit(
          () -> {
            try {
              GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
              GraphicsDevice[] devices = ge.getScreenDevices();

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

              final boolean finalFoundChanges = foundChanges;

              SwingUtilities.invokeLater(() -> callback.accept(finalFoundChanges));
            } catch (Exception e) {
              SwingUtilities.invokeLater(() -> callback.accept(false));
            }
          });
    }
  }

  // TODO: implement this
  private static List<DisplayInfo> loadDisplaysFromAppPreferences() {

    return detectedDispayList;
  }

  private static void saveDisplaysToAppPreferences(List<DisplayInfo> detected) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

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


  @FunctionalInterface
  private interface DisplayMatcher {
    boolean matches(GraphicsDevice find, DisplayInfo info);
  }

  /**
   * A series of display matchers in order of best "fit".
   * These are used by {@link #matchDisplay(GraphicsDevice, List)} until a unique match is found
   */
  private static final DisplayMatcher[] DISPLAY_MATCHERS_SEQUENCE = {
      (find, info) -> info.idString.equals(find.getIDstring()),
      (find, info) -> info.idString.equals(find.getIDstring()) && info.samePositionAs(find.getDefaultConfiguration().getBounds()),
      (find, info) -> info.idString.equals(find.getIDstring()) && info.sameBoundsAs(find.getDefaultConfiguration().getBounds()),
      (find, info) -> info.samePositionAs(find.getDefaultConfiguration().getBounds()),
      (find, info) -> info.sameBoundsAs(find.getDefaultConfiguration().getBounds())
  };

  /**
   * Find the best match of a display in a list.
   * We try the predicates in DISPLAY_MATCHERS_SEQUENCE, first looking for a unique match, then
   * looking for a first match.
   *
   * @param find The display that we are looking for.
   * @param list The list of displays to search.
   * @return the index of the match, or -1 if no match found
   */
  public static OptionalInt matchDisplay(final GraphicsDevice find, final List<DisplayInfo> list) {
    // find the first unique match

    OptionalInt findUnique = Arrays.stream(DISPLAY_MATCHERS_SEQUENCE)
        .map(matcher -> {
              // we run through the list twice, but this method is not heavily used so
              // optimisation is not required

              OptionalInt first = IntStream.range(0, list.size())
                  .filter(i -> matcher.matches(find, list.get(i)))
                  .findFirst();

              OptionalInt second = IntStream.range(0, list.size())
                  .filter(i -> matcher.matches(find, list.get(i)))
                  .skip(1)
                  .findFirst();

              if (first.isPresent() && second.isEmpty()) {
                return first;
              } else {
                return OptionalInt.empty();
              }
            }
        )
        .flatMapToInt(OptionalInt::stream)
        .findFirst();

    if (findUnique.isPresent()) return findUnique;

    // find any match

    return Arrays.stream(DISPLAY_MATCHERS_SEQUENCE)
        .map(matcher ->
            IntStream.range(0, list.size())
                .filter(i -> matcher.matches(find, list.get(i)))
                .findFirst()
        )
        .flatMapToInt(OptionalInt::stream)
        .findFirst();
  }

}
