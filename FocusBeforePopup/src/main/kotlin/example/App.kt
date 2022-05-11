package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val popup1 = makePopupMenu()

  val textField1 = JTextField("Default setComponentPopupMenu")
  textField1.componentPopupMenu = popup1
  textField1.name = "textField1"

  val popup2 = TextComponentPopupMenu()
  val textField2 = JTextField("Override JPopupMenu#show(...)")
  textField2.componentPopupMenu = popup2
  textField2.name = "textField2"

  val combo3 = JComboBox(arrayOf("JPopupMenu does not open???", "111", "222"))
  combo3.isEditable = true
  // NOT work: combo3.setComponentPopupMenu(popup2)
  val textField3 = combo3.editor.editorComponent
  (textField3 as? JComponent)?.componentPopupMenu = popup2
  textField3.name = "textField3"
  // TEST: textField3.putClientProperty("doNotCancelPopup", null)

  val combo4 = JComboBox(arrayOf("addMouseListener", "111", "222"))
  combo4.isEditable = true
  val textField4 = combo4.editor.editorComponent
  (textField4 as? JComponent)?.componentPopupMenu = popup2
  textField4.name = "textField4"
  val ml4 = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      println("Close all JPopupMenu(excludes dropdown list of own JComboBox)")
      for (m in MenuSelectionManager.defaultManager().selectedPath) {
        if (combo4.isPopupVisible) {
          continue
        } else if (m is JPopupMenu) {
          m.isVisible = false
        }
      }
    }
  }
  textField4.addMouseListener(ml4)

  val box = Box.createVerticalBox()
  listOf<Component>(textField1, textField2, combo3, combo4).forEach {
    box.add(it)
    box.add(Box.createVerticalStrut(5))
  }

  val textArea = JTextArea("JTextArea")
  textArea.componentPopupMenu = popup2

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopupMenu(): JPopupMenu {
  val cutAction = DefaultEditorKit.CutAction()
  val copyAction = DefaultEditorKit.CopyAction()
  val pasteAction = DefaultEditorKit.PasteAction()

  val popup1 = JPopupMenu()
  popup1.add(cutAction)
  popup1.add(copyAction)
  popup1.add(pasteAction)
  val pml1 = object : PopupMenuListener {
    override fun popupMenuCanceled(e: PopupMenuEvent) {
      /* not needed */
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      /* not needed */
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      val pop = e.source as? JPopupMenu ?: return
      (pop.invoker as? JTextComponent)?.also {
        println("${it.javaClass.name}: ${it.name}")
        // TEST:
        // it.requestFocusInWindow()
        // it.selectAll()
        val hasSelectedText = it.selectedText != null
        cutAction.isEnabled = hasSelectedText
        copyAction.isEnabled = hasSelectedText
      }
    }
  }
  popup1.addPopupMenuListener(pml1)
  return popup1
}

private class TextComponentPopupMenu : JPopupMenu() {
  private val cutAction = DefaultEditorKit.CutAction()
  private val copyAction = DefaultEditorKit.CopyAction()
  private val pasteAction = DefaultEditorKit.PasteAction()

  init {
    add(cutAction)
    add(copyAction)
    add(pasteAction)
  }

  override fun show(c: Component, x: Int, y: Int) {
    println("${c.javaClass.name}: ${c.name}")
    val tc = c as? JTextComponent ?: return
    tc.requestFocusInWindow()
    var hasSelectedText = tc.selectedText != null
    if (!tc.isFocusOwner && !hasSelectedText) {
      tc.selectAll()
      hasSelectedText = true
    }
    cutAction.isEnabled = hasSelectedText
    copyAction.isEnabled = hasSelectedText
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
