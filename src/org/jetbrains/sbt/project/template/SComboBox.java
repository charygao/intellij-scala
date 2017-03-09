package org.jetbrains.sbt.project.template;

import scala.Function1;

import javax.swing.*;
import java.awt.*;

/**
 * @author Pavel Fatin
 */
public class SComboBox extends JComboBox {
  public SComboBox() {
  }

  public <T extends Object> SComboBox(T[] items) {
    //noinspection unchecked
    super(items);
  }

  public <T extends Object> void setItems(T[] items) {
    //noinspection unchecked
    super.setModel(new DefaultComboBoxModel(items));
  }

  public void setTextRenderer(final Function1<String, String> renderer) {
    //noinspection unchecked
    setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value != null && !(value instanceof String)) {
          throw new IllegalArgumentException("Not a String value: " + value);
        }
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(renderer.apply((String) value));
        return component;
      }
    });
  }
}
