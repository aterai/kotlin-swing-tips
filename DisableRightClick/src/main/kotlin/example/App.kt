package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup

fun makeUI(): Component {
  val combo1 = makeComboBox(5)
  combo1.ui = if (combo1.ui is WindowsComboBoxUI) {
    object : WindowsComboBoxUI() {
      override fun createPopup() = BasicComboPopup2(comboBox)
    }
  } else {
    object : BasicComboBoxUI() {
      override fun createPopup() = BasicComboPopup2(comboBox)
    }
  }

  val combo2 = makeComboBox(20)
  combo2.ui = if (combo2.ui is WindowsComboBoxUI) {
    object : WindowsComboBoxUI() {
      override fun createPopup() = BasicComboPopup3(comboBox)
    }
  } else {
    object : BasicComboBoxUI() {
      override fun createPopup() = BasicComboPopup3(comboBox)
    }
  }

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("default:", makeComboBox(5)))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("default:", makeComboBox(20)))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("disable right click in drop-down list:", combo1))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("disable right click and scroll in drop-down list:", combo2))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private fun makeComboBox(size: Int): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  for (i in 0 until size) {
    model.addElement("No.$i")
  }
  return JComboBox(model)
}

private class BasicComboPopup2(combo: JComboBox<*>) : BasicComboPopup(combo) {
  @Transient private var handler2: MouseListener? = null

  override fun uninstallingUI() {
    super.uninstallingUI()
    handler2 = null
  }

  override fun createListMouseListener() = handler2 ?: Handler2()

  private inner class Handler2 : MouseAdapter() {
    override fun mouseReleased(e: MouseEvent) {
      if (e.source != list) {
        return
      }
      if (list.model.size > 0) {
        // <ins>
        if (!SwingUtilities.isLeftMouseButton(e) || !comboBox.isEnabled) {
          return
        }
        // </ins>
        // JList mouse listener
        if (comboBox.selectedIndex == list.selectedIndex) {
          comboBox.editor.item = list.selectedValue
        }
        comboBox.selectedIndex = list.selectedIndex
      }
      comboBox.isPopupVisible = false
      // workaround for cancelling an edited item (bug 4530953)
      if (comboBox.isEditable && comboBox.editor != null) {
        comboBox.configureEditor(comboBox.editor, comboBox.selectedItem)
      }
    }
  }
}

private class BasicComboPopup3(combo: JComboBox<*>) : BasicComboPopup(combo) {
  override fun createList(): JList<*> {
    return object : JList<Any>(comboBox.model) {
      override fun processMouseEvent(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e)) {
          return
        }
        var ev = e
        if (e.isControlDown) {
          // Fix for 4234053. Filter out the Control Key from the list.
          // ie., don't allow CTRL key deselection.
          ev = MouseEvent(
            e.component, e.id, e.getWhen(),
            // e.getModifiers() ^ InputEvent.CTRL_MASK,
            // Java 10: e.getModifiersEx() ^ Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
            e.modifiersEx xor InputEvent.CTRL_DOWN_MASK,
            e.x, e.y,
            e.xOnScreen, e.yOnScreen,
            e.clickCount,
            e.isPopupTrigger,
            MouseEvent.NOBUTTON,
          )
        }
        super.processMouseEvent(ev)
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
