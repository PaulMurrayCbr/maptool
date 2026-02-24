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
import javax.swing.*;
import net.rptools.maptool.client.swing.AbeillePanel;

public class DetectedDisplayRenderer extends AbeillePanel
    implements ListCellRenderer<HardwareTabUtils.DisplayInfo> {

  JPanel mainContentPanel;
  JLabel nameLabel;
  JLabel descriptionLabel;

  public DetectedDisplayRenderer() {
    super(new DetectedDisplayRendererView().getRootComponent());
    initComponents();
  }

  /** Initalises component models, event listeners, etc. */
  private void initComponents() {
    mainContentPanel = (JPanel) getComponent("mainContentPanel");
    nameLabel = getLabel("nameLabel");
    descriptionLabel = getLabel("descriptionLabel");
    setInitialState();
  }

  /** Initializes component content. */
  private void setInitialState() {}

  @Override
  public Component getListCellRendererComponent(
      JList<? extends HardwareTabUtils.DisplayInfo> list,
      HardwareTabUtils.DisplayInfo value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    setFont(list.getFont());
    setEnabled(list.isEnabled());
    setOpaque(true);

    if (isSelected) {
      setAllBackground(list.getSelectionBackground());
      setAllForeground(list.getSelectionForeground());
    } else {
      setAllBackground(list.getBackground());
      setAllForeground(list.getForeground());
    }

    if (cellHasFocus) {
      setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
    } else {
      setBorder(UIManager.getBorder("List.cellNoFocusBorder"));
    }

    setValue(value);

    return this;
  }

  public void setAllBackground(Color c) {
    super.setBackground(c);
    // JLabels are transparent, so there's no need to set the background in them
    mainContentPanel.setBackground(c);
  }

  public void setAllForeground(Color c) {
    super.setForeground(c);
    mainContentPanel.setForeground(c);
    nameLabel.setForeground(c);
    descriptionLabel.setForeground(c);
  }

  void setValue(HardwareTabUtils.DisplayInfo value) {
    if (value == null) {
      value = HardwareTabUtils.DisplayInfo.NO_SCREEN;
    }

    nameLabel.setText(value.idString());
    descriptionLabel.setText(
        value.detectedWidth()
            + "\u00D7"
            + value.detectedHeight()
            + " @ "
            + value.detectedX()
            + ","
            + value.detectedY());
  }
}
