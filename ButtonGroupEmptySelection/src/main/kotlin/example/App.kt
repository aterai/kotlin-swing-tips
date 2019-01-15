package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Optional
import java.util.stream.Stream
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {

    val bg = ToggleButtonGroup()
    val p = JPanel()
    // Stream.of("A", "B", "C").map(Function<String, JToggleButton> { JToggleButton(it) }).forEach({ r ->
    Stream.of("A", "B", "C").map(::JToggleButton).forEach({ r ->
      r.setActionCommand(r.getText())
      p.add(r)
      bg.add(r)
    })

    val label = JLabel()
    val button = JButton("check")
    button.addActionListener({
      val txt = Optional.ofNullable(bg.getSelection())
        .map({ b -> String.format("\"%s\" isSelected.", b.getActionCommand()) })
        .orElse("Please select one of the option above.")
      label.setText(txt)
    })

    val box = Box.createHorizontalBox()
    box.add(label)
    box.add(Box.createHorizontalGlue())
    box.add(button, BorderLayout.WEST)
    box.add(Box.createHorizontalStrut(5))

    add(p)
    add(box, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }
}

internal class ToggleButtonGroup : ButtonGroup() {
  private var prevModel: ButtonModel? = null
  private var isAdjusting: Boolean = false
  override fun setSelected(m: ButtonModel?, b: Boolean) {
    if (isAdjusting) {
      return
    }
    if (m == prevModel) {
      isAdjusting = true
      clearSelection()
      isAdjusting = false
    } else {
      super.setSelected(m, b)
    }
    prevModel = getSelection()
  }
}

fun main() {
  EventQueue.invokeLater({
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
  })
}
