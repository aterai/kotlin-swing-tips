package example

import java.awt.*
import javax.swing.*
import kotlin.math.roundToInt

// https://stackoverflow.com/questions/35405672/use-width-and-max-width-to-wrap-text-in-joptionpane
private val textArea = object : JTextArea(1, 1) {
  override fun updateUI() {
    super.updateUI()
    lineWrap = true
    wrapStyleWord = true
    isEditable = false
    isOpaque = false
    font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  }

  override fun setText(t: String) {
    super.setText(t)
    columns = 50
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/text/JTextComponent.html#modelToView-int-
    // i.e. layout cannot be computed until the component has been sized.
    // The component does not have to be visible or painted.
    size = super.getPreferredSize() // looks like ugly hack...
    // println(super.getPreferredSize())
    rows = runCatching {
      val r = modelToView(t.length)
      // Java 9: val r = modelToView2D(t.length).bounds
      (r.maxY.toFloat() / rowHeight).roundToInt()
    }.getOrDefault(1)
    // println(super.getPreferredSize())
    val isOnlyOneColumn = rows == 1
    if (isOnlyOneColumn) {
      size = preferredSize
      columns = 1
    }
  }
}

fun makeUI(): Component {
  val scroll = JScrollPane(textArea)
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  val txt = "This is a long error message. 1, 22, 333, 4444, 55555. "
  val msgLong = txt.repeat(10)
  val longButton = JButton("JOptionPane: long")
  longButton.addActionListener {
    textArea.text = msgLong
    JOptionPane.showMessageDialog(scroll.rootPane, scroll, "Error", JOptionPane.ERROR_MESSAGE)
  }
  val msgShort = "This is a short error message."
  val shortButton = JButton("JOptionPane: short")
  shortButton.addActionListener {
    textArea.text = msgShort
    JOptionPane.showMessageDialog(scroll.rootPane, scroll, "Error", JOptionPane.ERROR_MESSAGE)
  }
  return JPanel().also {
    it.add(longButton)
    it.add(shortButton)
    it.preferredSize = Dimension(320, 240)
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
