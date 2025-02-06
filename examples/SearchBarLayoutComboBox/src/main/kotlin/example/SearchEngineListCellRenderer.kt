package example

import java.awt.Component
import javax.swing.*

class SearchEngineListCellRenderer<E> : ListCellRenderer<E> {
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JLabel && value is SearchEngine) {
      c.icon = value.favicon
      c.toolTipText = value.url
    }
    return c
  }
}
