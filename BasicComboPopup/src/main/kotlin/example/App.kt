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
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout()) {
  init {
    val jtp = JTextPane()
    jtp.text = "Shift+Tab"
    val combo = JComboBox(
      arrayOf(
        "public", "protected", "private",
        "final", "transient", "super", "this", "return", "class"
      )
    )
    val popup: BasicComboPopup = EditorComboPopup(jtp, combo)
    val amc = popup.actionMap
    amc.put("myUp", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val i = combo.selectedIndex
        combo.selectedIndex = if (i == 0) combo.itemCount - 1 else i - 1
      }
    })
    amc.put("myDown", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val i = combo.selectedIndex
        combo.selectedIndex = if (i == combo.itemCount - 1) 0 else i + 1
      }
    })
    amc.put("myEnt", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        combo.getItemAt(combo.selectedIndex)?.also {
          popup.hide()
          TextEditorUtils.append(jtp, it)
        }
      }
    })
    val imc = popup.inputMap
    imc.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "myUp")
    imc.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "myDown")
    imc.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "myEnt")
    jtp.actionMap.put("myPop", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        try {
          val rect = jtp.modelToView(jtp.caretPosition)
          // Java 9: Rectangle rect = jtp.modelToView2D(jtp.getCaretPosition()).getBounds();
          popup.show(jtp, rect.x, rect.maxY.toInt())
          EventQueue.invokeLater {
            val c = popup.topLevelAncestor
            if (c is Window) {
              c.toFront()
            }
            popup.requestFocusInWindow()
          }
        } catch (ex: BadLocationException) { // should never happen
          val wrap: RuntimeException = StringIndexOutOfBoundsException(ex.offsetRequested())
          wrap.initCause(ex)
          throw wrap
        }
      }
    })
    jtp.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "myPop")
    add(JScrollPane(jtp))
    preferredSize = Dimension(320, 240)
  }
}

class EditorComboPopup(private val textArea: JTextComponent, cb: JComboBox<*>?) : BasicComboPopup(cb) {
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

  override fun isFocusable(): Boolean {
    return true
  }
}

object TextEditorUtils {
  fun append(editor: JTextComponent, str: String?) {
    try {
      val doc = editor.document
      doc.insertString(editor.caretPosition, str, null)
    } catch (ex: BadLocationException) { // should never happen
      val wrap: RuntimeException = StringIndexOutOfBoundsException(ex.offsetRequested())
      wrap.initCause(ex)
      throw wrap
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
