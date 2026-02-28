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
import javax.swing.*;

/**
 * The DetectedDisplayListRenderer class implements a custom renderer for displaying
 * {@link HardwareTabUtils.DisplayInfo} objects within a {@link JList}.
 *
 * This class customizes the appearance and layout of the list cell by defining how each
 * {@link HardwareTabUtils.DisplayInfo} object should be represented in terms of text and style.
 *
 * It uses a {@link JPanel} as the root component for rendering the cell, along with sub-components
 * like {@link JLabel} for displaying the display name and description. The list cell's background
 * and foreground colors adapt based on the selection and focus state of the component.
 */

public class DetectedDisplayListRenderer implements ListCellRenderer<HardwareTabUtils.DisplayInfo> {

  // injected by the intellij form framework

  private JPanel mainPanel;
  private JLabel nameLabel;
  private JLabel descriptionLabel;

  public JComponent getRootComponent() {
    return mainPanel;
  }

  @Override
  public Component getListCellRendererComponent(
      JList<? extends HardwareTabUtils.DisplayInfo> list,
      HardwareTabUtils.DisplayInfo value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    mainPanel.setFont(list.getFont());
    mainPanel.setEnabled(list.isEnabled());
    mainPanel.setOpaque(true);

    if (isSelected) {
      setAllBackground(list.getSelectionBackground());
      setAllForeground(list.getSelectionForeground());
    } else {
      setAllBackground(list.getBackground());
      setAllForeground(list.getForeground());
    }

    if (cellHasFocus) {
      mainPanel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
    } else {
      mainPanel.setBorder(UIManager.getBorder("List.cellNoFocusBorder"));
    }

    setValue(value);

    return mainPanel;
  }

  public void setValue(HardwareTabUtils.DisplayInfo value) {
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

  private void setAllBackground(Color c) {
    mainPanel.setBackground(c);
    // JLabels are transparent, so there's no need to set the background in them
    mainPanel.setBackground(c);
  }

  private void setAllForeground(Color c) {
    mainPanel.setForeground(c);
    nameLabel.setForeground(c);
    descriptionLabel.setForeground(c);
  }
}
