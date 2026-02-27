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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class DisplayAspectCorrectionPane implements ListSelectionListener {
  // injected by intellij forms framework
  private JPanel mainPanel;
  private JButton resetButton;
  private JButton saveButton;
  private JCheckBox useAspectCorrectionChk;
  private JCheckBox fullscreenOnlyChk;
  private JSpinner actualWidthSpn;
  private JSpinner actualHeightSpn;
  private JLabel detectedSizeLbl;

  public JPanel getRootComponent() {
    return mainPanel;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) return;

    JList<HardwareTabUtils.DisplayInfo> list = (JList<HardwareTabUtils.DisplayInfo>) e.getSource();
    HardwareTabUtils.DisplayInfo selected = list.getSelectedValue();

    // use selected
  }
}
