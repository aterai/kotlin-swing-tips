package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicComboPopup

class AuxiliaryWindowsComboBoxUI : WindowsComboBoxUI() {
  override fun createPopup() = BasicComboPopup2(comboBox)

  override fun configureEditor() {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  override fun unconfigureEditor() {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  override fun removeEditor() {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  override fun addEditor() {
    removeEditor()
    comboBox.editor?.editorComponent?.also {
      configureEditor()
      comboBox.add(it)
      if (comboBox.isFocusOwner) {
        it.requestFocusInWindow()
      }
    }
  }

  override fun update(g: Graphics, c: JComponent) {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  override fun paint(g: Graphics, c: JComponent) {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  override fun paintCurrentValue(g: Graphics, bounds: Rectangle, hasFocus: Boolean) {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  override fun paintCurrentValueBackground(g: Graphics, bounds: Rectangle, hasFocus: Boolean) {
    /* Override all UI-specific methods your UI classes inherit. */
  }

  companion object {
    @JvmStatic
    fun createUI(c: JComponent?) = AuxiliaryWindowsComboBoxUI()
  }
}

class BasicComboPopup2(combo: JComboBox<*>) : BasicComboPopup(combo) {
  private var handler2: MouseListener? = null

  override fun uninstallingUI() {
    super.uninstallingUI()
    handler2 = null
  }

  override fun createListMouseListener(): MouseListener {
    val h = handler2 ?: Handler2()
    handler2 = h
    return h
  }

  private inner class Handler2 : MouseAdapter() {
    override fun mouseReleased(e: MouseEvent) {
      if (e.source != list) {
        return
      }
      if (list.model.size > 0) {
        if (!SwingUtilities.isLeftMouseButton(e) || !comboBox.isEnabled) {
          return
        }
        if (comboBox.selectedIndex == list.selectedIndex) {
          comboBox.editor.item = list.selectedValue
        }
        comboBox.selectedIndex = list.selectedIndex
      }
      comboBox.isPopupVisible = false
      if (comboBox.isEditable && comboBox.editor != null) {
        comboBox.configureEditor(comboBox.editor, comboBox.selectedItem)
      }
    }
  }
}
