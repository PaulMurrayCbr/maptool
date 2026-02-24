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

  JList<HardwareTabUtils.DisplayInfo> listComponent;

  public DetectedDisplayRenderer() {
    super(new DetectedDisplayRendererView().getRootComponent());
    initComponents();
  }

  /** Initalises component models, event listeners, etc. */
  private void initComponents() {
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
      setBackground(list.getSelectionBackground());
      setForeground(list.getSelectionForeground());
    } else {
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    if (cellHasFocus) {
      setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
    } else {
      setBorder(UIManager.getBorder("List.cellNoFocusBorder"));
    }

    setValue(value);

    return this;
  }

  void setValue(HardwareTabUtils.DisplayInfo value) {
    if (value == null) {
      value = HardwareTabUtils.DisplayInfo.NO_SCREEN;
    }
  }
}
