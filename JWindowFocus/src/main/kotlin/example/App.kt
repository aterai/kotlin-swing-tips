package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

private val editor = makeEditor()
private val button1 = JButton("JPopupMenu")
private val button2 = JButton("JFrame#setUndecorated(true)")
private val button3 = JButton("JWindow()")
private val button4 = JButton("JWindow(owner)")

fun makeUI(): Component {
  button1.addActionListener { e ->
    (e.source as? JButton)?.also {
      resetEditor(editor, it)
      val popup = JPopupMenu()
      popup.border = BorderFactory.createEmptyBorder()
      popup.add(editor)
      popup.pack()
      val p = it.location
      p.y += it.height
      popup.show(it.parent, p.x, p.y)
      editor.requestFocusInWindow()
    }
  }

  button2.addActionListener { e ->
    (e.source as? JButton)?.also {
      resetEditor(editor, it)
      val window = JFrame()
      window.isUndecorated = true
      window.isAlwaysOnTop = true
      window.add(editor)
      window.pack()
      window.location = getWindowLocation(it)
      window.isVisible = true
      editor.requestFocusInWindow()
    }
  }

  button3.addActionListener { e ->
    (e.source as? JButton)?.also {
      resetEditor(editor, it)
      val window = JWindow()
      window.focusableWindowState = true
      window.isAlwaysOnTop = true
      window.add(editor)
      window.pack()
      window.location = getWindowLocation(it)
      window.isVisible = true
      editor.requestFocusInWindow()
    }
  }

  button4.addActionListener { e ->
    (e.source as? JButton)?.also {
      resetEditor(editor, it)
      val window = JWindow(SwingUtilities.getWindowAncestor(it))
      window.focusableWindowState = true
      window.isAlwaysOnTop = true
      window.add(editor)
      window.pack()
      window.location = getWindowLocation(it)
      window.isVisible = true
      editor.requestFocusInWindow()
    }
  }

  return JPanel(FlowLayout(FlowLayout.LEADING, 50, 50)).also {
    initWindowListener(it, editor)
    listOf(button1, button2, button3, button4).map(it::add)
    it.preferredSize = Dimension(320, 240)
  }
}

fun initWindowListener(c: Container, editor: JTextArea) {
  EventQueue.invokeLater {
    val window = SwingUtilities.getWindowAncestor(c)
    val wl = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        resetEditor(editor, null)
      }
    }
    window?.addMouseListener(wl)
    val cl = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        resetEditor(editor, null)
      }

      override fun componentMoved(e: ComponentEvent) {
        resetEditor(editor, null)
      }
    }
    window?.addComponentListener(cl)
  }
}

fun getWindowLocation(c: Component): Point {
  val p = c.location
  p.y += c.height
  SwingUtilities.convertPointToScreen(p, c.parent)
  return p
}

fun makeEditor(): JTextArea {
  val editor = JTextArea()
  editor.font = UIManager.getFont("TextField.font")
  editor.border = BorderFactory.createLineBorder(Color.GRAY)
  editor.lineWrap = true
  editor.componentPopupMenu = TextComponentPopupMenu()
  val dl = object : DocumentListener {
    private var prev = -1
    private fun update() {
      EventQueue.invokeLater {
        val h = editor.preferredSize.height
        if (prev != h) {
          val rect = editor.bounds
          rect.height = h
          editor.bounds = rect
          val p = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, editor)
          if (p is JPopupMenu) {
            p.pack()
            editor.requestFocusInWindow()
          } else {
            val w = SwingUtilities.getWindowAncestor(editor)
            w?.pack()
          }
        }
        prev = h
      }
    }

    override fun insertUpdate(e: DocumentEvent) {
      update()
    }

    override fun removeUpdate(e: DocumentEvent) {
      update()
    }

    override fun changedUpdate(e: DocumentEvent) {
      update()
    }
  }
  editor.document.addDocumentListener(dl)
  return editor
}

fun resetEditor(editor: JTextArea, b: JButton?) {
  val window = SwingUtilities.getWindowAncestor(editor)
  window?.dispose()
  if (b != null) {
    editor.text = b.text
    val d = editor.preferredSize
    editor.setBounds(0, 0, b.width, d.height)
  }
}

private class TextComponentPopupMenu : JPopupMenu() {
  init {
    add(DefaultEditorKit.CutAction())
    add(DefaultEditorKit.CopyAction())
    add(DefaultEditorKit.PasteAction())
    add("delete").addActionListener {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText != null
      for (menuElement in subElements) {
        val m = menuElement.component
        if (m is JMenuItem && m.action is DefaultEditorKit.PasteAction) {
          continue
        }
        m.isEnabled = hasSelectedText
      }
      super.show(c, x, y)
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
