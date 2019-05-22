package example

// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout()) {
  init {
    val popup1 = makePopupMenu()

    val textField1 = JTextField("Default setComponentPopupMenu")
    textField1.setComponentPopupMenu(popup1)
    textField1.setName("textField1")

    val popup2 = TextComponentPopupMenu()
    val textField2 = JTextField("Override JPopupMenu#show(...)")
    textField2.setComponentPopupMenu(popup2)
    textField2.setName("textField2")

    val combo3 = JComboBox<String>(arrayOf("JPopupMenu does not open???", "111", "222"))
    combo3.setEditable(true)
    // NOT work: combo3.setComponentPopupMenu(popup2);
    val textField3 = combo3.getEditor().getEditorComponent() as JTextField
    textField3.setComponentPopupMenu(popup2)
    textField3.setName("textField3")
    // TEST: textField3.putClientProperty("doNotCancelPopup", null);

    val combo4 = JComboBox<String>(arrayOf("addMouseListener", "111", "222"))
    combo4.setEditable(true)
    val textField4 = combo4.getEditor().getEditorComponent() as JTextField
    textField4.setComponentPopupMenu(popup2)
    textField4.setName("textField4")
    textField4.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        println("Close all JPopupMenu(excludes dropdown list of own JComboBox)")
        for (m in MenuSelectionManager.defaultManager().getSelectedPath()) {
          if (combo4.isPopupVisible()) { // m instanceof ComboPopup
            continue
          } else if (m is JPopupMenu) {
            m.setVisible(false)
          }
        }
      }
    })

    val box = Box.createVerticalBox()
    listOf<Component>(textField1, textField2, combo3, combo4).forEach {
      box.add(it)
      box.add(Box.createVerticalStrut(5))
    }

    val textArea = JTextArea("dummy")
    textArea.setComponentPopupMenu(popup2)

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(box, BorderLayout.NORTH)
    add(JScrollPane(textArea))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makePopupMenu(): JPopupMenu {
    val cutAction = DefaultEditorKit.CutAction()
    val copyAction = DefaultEditorKit.CopyAction()
    val pasteAction = DefaultEditorKit.PasteAction()

    val popup1 = JPopupMenu()
    popup1.add(cutAction)
    popup1.add(copyAction)
    popup1.add(pasteAction)
    popup1.addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuCanceled(e: PopupMenuEvent) { /* not needed */
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) { /* not needed */
      }

      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        val pop = e.getSource() as JPopupMenu
        val tc = pop.getInvoker() as JTextComponent
        println("${tc.javaClass.getName()}: ${tc.getName()}")
        // TEST:
        // tc.requestFocusInWindow();
        // tc.selectAll();
        val hasSelectedText = tc.getSelectedText() != null
        cutAction.setEnabled(hasSelectedText)
        copyAction.setEnabled(hasSelectedText)
      }
    })
    return popup1
  }
}

class TextComponentPopupMenu : JPopupMenu() {
  private val cutAction = DefaultEditorKit.CutAction()
  private val copyAction = DefaultEditorKit.CopyAction()
  private val pasteAction = DefaultEditorKit.PasteAction()

  init {
    add(cutAction)
    add(copyAction)
    add(pasteAction)
  }

  override fun show(c: Component, x: Int, y: Int) {
    println("${c.javaClass.getName()}: ${c.getName()}")
    val tc = c as? JTextComponent ?: return
    tc.requestFocusInWindow()
    var hasSelectedText = tc.getSelectedText() != null
    if (tc is JTextField && !tc.isFocusOwner() && !hasSelectedText) {
      tc.selectAll()
      hasSelectedText = true
    }
    cutAction.setEnabled(hasSelectedText)
    copyAction.setEnabled(hasSelectedText)
    super.show(tc, x, y)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
