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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Edit and save aspect correction settings for a display selected in a JList */
public class AspectCorrectionPane implements ListSelectionListener {
  private static final Logger log = LogManager.getLogger(AspectCorrectionPane.class);

  // injected by intellij forms framework
  private JPanel mainPanel;
  private JButton resetButton;
  private JButton saveButton;
  private JCheckBox useAspectCorrectionChk;
  private JCheckBox fullscreenOnlyChk;
  private JSpinner aspectXSpn;
  private JSpinner aspectYSpn;
  private JLabel detectedSizeLbl;

  /** We trust that the JList being supplied to us matches the known displays in HarwareTabUtils. */
  HardwareTabUtils.DisplayInfo selected;

  int selectedIndex;

  AspectCorrectionPane() {
    ((SpinnerNumberModel) aspectXSpn.getModel()).setMinimum(1);
    ((SpinnerNumberModel) aspectXSpn.getModel()).setMaximum(10000);
    ((SpinnerNumberModel) aspectYSpn.getModel()).setMinimum(1);
    ((SpinnerNumberModel) aspectYSpn.getModel()).setMaximum(10000);
    clearSelected();

    ChangeListener changeListener = this::checkChange;

    useAspectCorrectionChk.addChangeListener(changeListener);
    fullscreenOnlyChk.addChangeListener(changeListener);
    aspectXSpn.addChangeListener(changeListener);
    aspectYSpn.addChangeListener(changeListener);

    resetButton.addActionListener(e -> loadDisplayInfo());

    saveButton.addActionListener(e -> saveDisplayInfo());
  }

  public JPanel getRootComponent() {
    return mainPanel;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) return;

    JList<HardwareTabUtils.DisplayInfo> list = (JList<HardwareTabUtils.DisplayInfo>) e.getSource();
    HardwareTabUtils.DisplayInfo selected = list.getSelectedValue();
    selectedIndex = list.getSelectedIndex();

    if (selected == null) {
      clearSelected();
    } else {
      setSelected(selected);
    }
  }

  void clearSelected() {
    selected = HardwareTabUtils.DisplayInfo.NO_SCREEN;
    loadDisplayInfo();

    useAspectCorrectionChk.setEnabled(false);
    fullscreenOnlyChk.setEnabled(false);
    aspectXSpn.setEnabled(false);
    aspectYSpn.setEnabled(false);
    resetButton.setEnabled(false);
    saveButton.setEnabled(false);

    detectedSizeLbl.setText(" ");
  }

  void setSelected(HardwareTabUtils.DisplayInfo selected) {
    this.selected = selected;
    loadDisplayInfo();

    useAspectCorrectionChk.setEnabled(true);
    fullscreenOnlyChk.setEnabled(true);
    aspectXSpn.setEnabled(true);
    aspectYSpn.setEnabled(true);
    resetButton.setEnabled(false);
    saveButton.setEnabled(false);

    detectedSizeLbl.setText(
        selected.detectedWidth()
            + "x"
            + selected.detectedHeight()
            + " - "
            + selected.detectedAspectX()
            + ":"
            + selected.detectedAspectY());
  }

  void loadDisplayInfo() {
    useAspectCorrectionChk.setSelected(selected.useAspectRatioCorrection());
    fullscreenOnlyChk.setSelected(selected.fullscreenOnly());
    aspectXSpn.setValue(selected.aspectX());
    aspectYSpn.setValue(selected.aspectY());
  }

  void saveDisplayInfo() {
    if (selected == null) return;

    HardwareTabUtils.updateDisplayAspectCorrectionPreferences(
        selectedIndex,
        useAspectCorrectionChk.isSelected(),
        fullscreenOnlyChk.isSelected(),
        (Integer) aspectXSpn.getValue(),
        (Integer) aspectYSpn.getValue());
  }

  void checkChange(ChangeEvent e) {
    boolean hasChange = false;

    if (useAspectCorrectionChk.isSelected() != selected.useAspectRatioCorrection()) {
      hasChange = true;
    }
    if (fullscreenOnlyChk.isSelected() != selected.fullscreenOnly()) {
      hasChange = true;
    }
    if ((Integer) aspectXSpn.getValue() != selected.aspectX()) {
      hasChange = true;
    }
    if ((Integer) aspectYSpn.getValue() != selected.aspectY()) {
      hasChange = true;
    }

    saveButton.setEnabled(hasChange);
    resetButton.setEnabled(hasChange);
  }
}
