package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class SearchEngineListCellRenderer<E : SearchEngine> : ListCellRenderer<E> {
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out E>,
    value: E?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    val l = c as? JLabel ?: return c
    value?.also {
      l.setIcon(it.favicon)
      l.setToolTipText(it.url)
    }
    return l
  }
}
