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

import javax.swing.*;

/**
 * A DisplayPane shows preference settings for attached displays. It displays a JList of detected displays and allows
 * editing of preferences for the selected display.
 * Currently there is one sub-pane, which allows editing of aspect correction settings.
 */

public class DisplayPane {
  // injected by intellij forms framework
  private JPanel mainPanel;
  private JList<HardwareTabUtils.DisplayInfo> displaysList;
  private JButton detectDisplaysBtn;
  private AspectCorrectionPane aspectCorrectionPanel;

  private final DefaultListModel<HardwareTabUtils.DisplayInfo> detectedDisplaysModel =
      new DefaultListModel<>();

  DisplayPane() {
    displaysList.setModel(detectedDisplaysModel);
    displaysList.setCellRenderer(new DetectedDisplayListRenderer());

    displaysList.addListSelectionListener(aspectCorrectionPanel);

    detectDisplaysBtn.addActionListener(
        e -> {
          detectDisplaysBtn.setEnabled(false);
          HardwareTabUtils.detectDisplays(
              found -> {
                detectDisplaysBtn.setEnabled(true);
                if (found) {
                  loadDetectedDisplays();
                }
              });
        });

    loadDetectedDisplays();
  }

  public JPanel getRootComponent() {
    return mainPanel;
  }

  private void loadDetectedDisplays() {
    detectedDisplaysModel.clear();
    detectedDisplaysModel.addAll(HardwareTabUtils.getKnownDisplays());
  }
}
