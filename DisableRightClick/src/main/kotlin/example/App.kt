package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.plaf.basic.ComboPopup

class MainPanel : JPanel(BorderLayout()) {
  init {
    val combo1 = makeComboBox(5)
    if (combo1.getUI() is WindowsComboBoxUI) {
      combo1.setUI(object : WindowsComboBoxUI() {
        protected override fun createPopup(): ComboPopup {
          return BasicComboPopup2(comboBox)
        }
      })
    } else {
      combo1.setUI(object : BasicComboBoxUI() {
        protected override fun createPopup(): ComboPopup {
          return BasicComboPopup2(comboBox)
        }
      })
    }

    val combo2 = makeComboBox(20)
    if (combo2.getUI() is WindowsComboBoxUI) {
      combo2.setUI(object : WindowsComboBoxUI() {
        protected override fun createPopup(): ComboPopup {
          return BasicComboPopup3(comboBox)
        }
      })
    } else {
      combo2.setUI(object : BasicComboBoxUI() {
        protected override fun createPopup(): ComboPopup {
          return BasicComboPopup3(comboBox)
        }
      })
    }

    val box = Box.createVerticalBox()
    box.add(makeTitledPanel("default:", makeComboBox(5)))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("default:", makeComboBox(20)))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("disable right click in drop-down list:", combo1))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("disable right click and scroll in drop-down list:", combo2))
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(box, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }

  private fun makeComboBox(size: Int): JComboBox<String> {
    val model = DefaultComboBoxModel<String>()
    for (i in 0 until size) {
      model.addElement("No.$i")
    }
    return JComboBox<String>(model)
  }
}

internal class BasicComboPopup2(combo: JComboBox<*>) : BasicComboPopup(combo) {
  @Transient
  private var handler2: MouseListener? = null

  override fun uninstallingUI() {
    super.uninstallingUI()
    handler2 = null
  }

  protected override fun createListMouseListener() = handler2 ?: Handler2()

  private inner class Handler2 : MouseAdapter() {
    override fun mouseReleased(e: MouseEvent) {
      if (e.getSource() != list) {
        return
      }
      if (list.getModel().getSize() > 0) {
        // <ins>
        if (!SwingUtilities.isLeftMouseButton(e) || !comboBox.isEnabled()) {
          return
        }
        // </ins>
        // JList mouse listener
        if (comboBox.getSelectedIndex() == list.getSelectedIndex()) {
          comboBox.getEditor().setItem(list.getSelectedValue())
        }
        comboBox.setSelectedIndex(list.getSelectedIndex())
      }
      comboBox.setPopupVisible(false)
      // workaround for cancelling an edited item (bug 4530953)
      if (comboBox.isEditable() && comboBox.getEditor() != null) {
        comboBox.configureEditor(comboBox.getEditor(), comboBox.getSelectedItem())
      }
    }
  }
}

internal class BasicComboPopup3(combo: JComboBox<*>) : BasicComboPopup(combo) {
  protected override fun createList(): JList<*> {
    return object : JList<Any>(comboBox.getModel()) {
      protected override fun processMouseEvent(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e)) {
          return
        }
        var ev = e
        if (e.isControlDown()) {
          // Fix for 4234053. Filter out the Control Key from the list.
          // ie., don't allow CTRL key deselection.
          ev = MouseEvent(
            e.getComponent(), e.getID(), e.getWhen(),
            // e.getModifiers() ^ InputEvent.CTRL_MASK,
            // Java 10: e.getModifiersEx() ^ Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
            e.getModifiersEx() xor InputEvent.CTRL_DOWN_MASK,
            e.getX(), e.getY(),
            e.getXOnScreen(), e.getYOnScreen(),
            e.getClickCount(),
            e.isPopupTrigger(),
            MouseEvent.NOBUTTON
          )
        }
        super.processMouseEvent(ev)
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
