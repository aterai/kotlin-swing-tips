package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val jtp = JTextPane()
  jtp.text = "Shift+Tab: open EditorComboPopup\n"

  val combo = JComboBox(
    arrayOf(
      "public", "protected", "private",
      "final", "transient", "super", "this", "return", "class"
    )
  )

  val popup = EditorComboPopup(jtp, combo)
  val am = popup.actionMap
  am.put("myUp", object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val i = combo.selectedIndex
      combo.selectedIndex = if (i == 0) combo.itemCount - 1 else i - 1
    }
  })
  am.put("myDown", object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val i = combo.selectedIndex
      combo.selectedIndex = if (i == combo.itemCount - 1) 0 else i + 1
    }
  })
  am.put("myEnt", object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      combo.getItemAt(combo.selectedIndex)?.also {
        popup.hide()
        TextEditorUtils.append(jtp, it)
      }
    }
  })

  val im = popup.inputMap
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "myUp")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "myDown")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "myEnt")

  jtp.actionMap.put("myPop", object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      runCatching {
        val rect = jtp.modelToView(jtp.caretPosition)
        // Java 9: val rect = jtp.modelToView2D(jtp.caretPosition).bounds
        popup.show(jtp, rect.x, rect.maxY.toInt())
        EventQueue.invokeLater {
          (popup.topLevelAncestor as? Window)?.toFront()
          popup.requestFocusInWindow()
        }
      }.onFailure { // should never happen
        it.printStackTrace()
        UIManager.getLookAndFeel().provideErrorFeedback(jtp)
      }
    }
  })
  jtp.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "myPop")

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(jtp))
    it.preferredSize = Dimension(320, 240)
  }
}

private class EditorComboPopup(private val textArea: JTextComponent, cb: JComboBox<*>) : BasicComboPopup(cb) {
  @Transient
  private var listener: MouseListener? = null

  override fun installListListeners() {
    super.installListListeners()
    listener = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        hide()
        TextEditorUtils.append(textArea, comboBox.selectedItem?.toString() ?: "")
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

private object TextEditorUtils {
  fun append(editor: JTextComponent, str: String?) {
    runCatching {
      val doc = editor.document
      doc.insertString(editor.caretPosition, str, null)
    }.onFailure { // should never happen
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
