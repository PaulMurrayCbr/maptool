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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * A DisplayPane shows preference settings for attached displays. It displays a JList of detected
 * displays and allows editing of preferences for the selected display. Currently there is one
 * sub-pane, which allows editing of aspect correction settings.
 */
public class DisplayPane implements HardwareTabUtils.KnownDisplaysListener {
  private static final Logger log = LogManager.getLogger(DisplayPane.class);

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

    detectDisplaysBtn.addActionListener(e -> HardwareTabUtils.detectDisplays());
    detectDisplaysBtn.setEnabled(!HardwareTabUtils.isDisplaysListBeingUpdated());

    loadDetectedDisplays();
  }

  public JPanel getRootComponent() {
    return mainPanel;
  }

  private void loadDetectedDisplays() {
    detectedDisplaysModel.clear();
    detectedDisplaysModel.addAll(HardwareTabUtils.getKnownDisplays());
  }

  private void createUIComponents() {

    /* After several failed attempts with IntelliJ forms to have DisplayPanel
    implement JPanel directly and not need a maninPanel, keeping mainPanel but
    using Custom Create was the best way I found to catch the removeNotify call
    in order to correctly clean up the listeners and avoid memory leaks.

    The other way to do it is to listen for component hierarchy events, but that's
    messier.
     */

    mainPanel =
        new JPanel() {
          @Override
          public void addNotify() {
            super.addNotify();
            HardwareTabUtils.addKnownDisplaysListener(DisplayPane.this);
          }

          @Override
          public void removeNotify() {
            HardwareTabUtils.removeKnownDisplaysListener(DisplayPane.this);
            super.removeNotify();
          }
        };
  }

  @Override
  public void displayListChanged() {
    detectedDisplaysModel.clear();
    detectedDisplaysModel.addAll(HardwareTabUtils.getKnownDisplays());
  }

  @Override
  public void displayChanged(int n) {
    detectedDisplaysModel.set(n, HardwareTabUtils.getKnownDisplays().get(n));
  }

  @Override
  public void displaysListBeingUpdated(boolean isUpdating) {
    detectDisplaysBtn.setEnabled(!isUpdating);
  }
}
