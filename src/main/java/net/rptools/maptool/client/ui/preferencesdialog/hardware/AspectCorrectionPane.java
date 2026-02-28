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

/** Edit and save aspect correction settings for a display selected in a JList */
public class AspectCorrectionPane implements ListSelectionListener {
  // injected by intellij forms framework
  private JPanel mainPanel;
  private JButton resetButton;
  private JButton saveButton;
  private JCheckBox useAspectCorrectionChk;
  private JCheckBox fullscreenOnlyChk;
  private JSpinner aspectXSpn;
  private JSpinner aspectYSpn;
  private JLabel detectedSizeLbl;

  boolean dirty = false;

  AspectCorrectionPane() {
    ((SpinnerNumberModel) aspectXSpn.getModel()).setMinimum(1);
    ((SpinnerNumberModel) aspectXSpn.getModel()).setMaximum(10000);
    ((SpinnerNumberModel) aspectYSpn.getModel()).setMinimum(1);
    ((SpinnerNumberModel) aspectYSpn.getModel()).setMaximum(10000);
    clearSelected();
  }

  public JPanel getRootComponent() {
    return mainPanel;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) return;

    JList<HardwareTabUtils.DisplayInfo> list = (JList<HardwareTabUtils.DisplayInfo>) e.getSource();
    HardwareTabUtils.DisplayInfo selected = list.getSelectedValue();

    if (selected == null) {
      clearSelected();
    } else {
      setSelected(selected);
    }
  }

  void clearSelected() {
    useAspectCorrectionChk.setEnabled(false);
    fullscreenOnlyChk.setEnabled(false);
    aspectXSpn.setEnabled(false);
    aspectYSpn.setEnabled(false);
    resetButton.setEnabled(false);
    saveButton.setEnabled(false);

    dirty = false;

    useAspectCorrectionChk.setSelected(false);
    fullscreenOnlyChk.setSelected(false);
    aspectXSpn.setValue(1);
    aspectYSpn.setValue(1);
    detectedSizeLbl.setText("");
  }

  void setSelected(HardwareTabUtils.DisplayInfo selected) {
    useAspectCorrectionChk.setEnabled(true);
    fullscreenOnlyChk.setEnabled(true);
    aspectXSpn.setEnabled(true);
    aspectYSpn.setEnabled(true);
    resetButton.setEnabled(false);
    saveButton.setEnabled(false);

    dirty = false;

    useAspectCorrectionChk.setSelected(selected.useAspectRatioCorrection());
    fullscreenOnlyChk.setSelected(selected.fullscreenOnly());
    detectedSizeLbl.setText(
        selected.detectedWidth()
            + "x"
            + selected.detectedHeight()
            + " - "
            + selected.detectedAspectX()
            + ":"
            + selected.detectedAspectY());
    aspectXSpn.setValue(selected.aspectX());
    aspectYSpn.setValue(selected.aspectY());
  }
}
