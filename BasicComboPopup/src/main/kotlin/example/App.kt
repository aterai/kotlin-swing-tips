package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val textPane = JTextPane()
  textPane.text = "Shift+Tab: open EditorComboPopup\n"

  val model = arrayOf(
    "public",
    "protected",
    "private",
    "final",
    "transient",
    "super",
    "this",
    "return",
    "class",
  )
  val combo = JComboBox<Any>(model)

  val popup = object : BasicComboPopup(combo) {
    private var listener: MouseListener? = null

    override fun installListListeners() {
      super.installListListeners()
      listener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          hide()
          TextEditorUtils.append(textPane, comboBox.selectedItem?.toString() ?: "")
        }
      }
      list?.addMouseListener(listener)
    }

    override fun uninstallingUI() {
      if (listener != null) {
        list.removeMouseListener(listener)
        listener = null
      }
      super.uninstallingUI()
    }

    override fun isFocusable() = true
  }
  val am = popup.actionMap
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val i = combo.selectedIndex
      val size = combo.itemCount
      combo.selectedIndex = (i - 1 + size) % size
    }
  }
  am.put("loopUp", a1)

  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val i = combo.selectedIndex
      val size = combo.itemCount
      combo.selectedIndex = (i + 1) % size
    }
  }
  am.put("loopDown", a2)

  val a3 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      combo.getItemAt(combo.selectedIndex)?.also {
        popup.hide()
        TextEditorUtils.append(textPane, it.toString())
      }
    }
  }
  am.put("insert", a3)

  val im = popup.inputMap
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "loopUp")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "loopDown")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insert")

  val a4 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      runCatching {
        val rect = textPane.modelToView(textPane.caretPosition)
        // Java 9: val rect = jtp.modelToView2D(jtp.caretPosition).bounds
        popup.show(textPane, rect.x, rect.maxY.toInt())
        EventQueue.invokeLater {
          (popup.topLevelAncestor as? Window)?.toFront()
          popup.requestFocusInWindow()
        }
      }.onFailure {
        it.printStackTrace()
        UIManager.getLookAndFeel().provideErrorFeedback(textPane)
      }
    }
  }
  textPane.actionMap.put("popupInsert", a4)
  val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
  textPane.inputMap.put(keyStroke, "popupInsert")

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private object TextEditorUtils {
  fun append(
    editor: JTextComponent,
    str: String?,
  ) {
    runCatching {
      val doc = editor.document
      doc.insertString(editor.caretPosition, str, null)
    }.onFailure {
      it.printStackTrace()
      UIManager.getLookAndFeel().provideErrorFeedback(editor)
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
