package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout()) {
  init {
    val COPY_KEY = "copy"

    val combo1 = JComboBox<String>(makeModel(5))
    val copy = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val combo = e.getSource() as JComboBox<*>
        combo.getSelectedItem()?.let {
          val contents = StringSelection(it.toString())
          val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
          clipboard.setContents(contents, null)
          println(it)
        }
      }
    }
    val am = combo1.getActionMap()
    am.put(COPY_KEY, copy)
    val modifiers = InputEvent.CTRL_DOWN_MASK
    val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, modifiers)
    val im = combo1.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(keyStroke, COPY_KEY)
    val popup = JPopupMenu()
    popup.add(COPY_KEY).addActionListener { e ->
      val o = popup.getInvoker()
      val c = if (o is JComboBox<*>) o else SwingUtilities.getAncestorOfClass(JComboBox::class.java, o as Component)
      if (c is JComboBox<*>) {
        val a = c.getActionMap().get(COPY_KEY)
        a.actionPerformed(ActionEvent(c, e.getID(), e.getActionCommand()))
        // KeyEvent keyEvent = new KeyEvent(c, 0, 0, 0, 0, 'C');
        // SwingUtilities.notifyAction(a, keyStroke, keyEvent, c, modifiers);
      }
    }
    combo1.setComponentPopupMenu(popup)

    val combo2 = JComboBox<String>(makeModel(10))
    combo2.setEditable(true)
    val field = combo2.getEditor().getEditorComponent() as JTextField
    field.setComponentPopupMenu(TextFieldPopupMenu())

    val box = Box.createVerticalBox()
    box.add(makeTitledPanel("Default:", JComboBox<String>(makeModel(0))))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("Editable: false, JPopupMenu, Ctrl+C", combo1))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("Editable: true, JPopupMenu, Ctrl+C", combo2))
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(box, BorderLayout.NORTH)

    val textArea = JTextArea()
    textArea.setComponentPopupMenu(TextFieldPopupMenu())
    add(JScrollPane(textArea))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }

  private fun makeModel(start: Int): ComboBoxModel<String> {
    val model = DefaultComboBoxModel<String>()
    // IntStream.range(start, start + 5).forEach { i -> model.addElement("item: $i") }
    (start until start + 5).forEach { model.addElement("item: $it") }
    return model
  }
}

internal class TextFieldPopupMenu : JPopupMenu() {
  private val cutAction = DefaultEditorKit.CutAction()
  private val copyAction = DefaultEditorKit.CopyAction()
  private val pasteAction = DefaultEditorKit.PasteAction()
  private val deleteAction = object : AbstractAction("delete") {
    override fun actionPerformed(e: ActionEvent) {
      val c = getInvoker()
      if (c is JTextComponent) {
        c.replaceSelection(null)
      }
    }
  }

  init {
    add(cutAction)
    add(copyAction)
    add(pasteAction)
    add(deleteAction)
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.getSelectedText() != null
      cutAction.setEnabled(hasSelectedText)
      copyAction.setEnabled(hasSelectedText)
      deleteAction.setEnabled(hasSelectedText)
      super.show(c, x, y)
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
