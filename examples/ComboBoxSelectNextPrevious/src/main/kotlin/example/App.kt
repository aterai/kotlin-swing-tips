package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.UIManager.LookAndFeelInfo

fun makeUI(): Component {
  val model = DefaultComboBoxModel(UIManager.getInstalledLookAndFeels())
  val combo = object : LookAndFeelComboBox(model) {
    override fun updateUI() {
      super.updateUI()
      val name = getUI().javaClass.name
      if (name.contains("MetalComboBoxUI") || name.contains("MotifComboBoxUI")) {
        val im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        im.put(KeyStroke.getKeyStroke("DOWN"), "selectNext2")
        im.put(KeyStroke.getKeyStroke("UP"), "selectPrevious2")
      }
    }
  }
  val box = JPanel(GridLayout(2, 2, 5, 2))
  box.add(JLabel("MetalComboBoxUI default"))
  box.add(JLabel("BasicComboBoxUI default"))
  box.add(LookAndFeelComboBox(model))
  box.add(combo)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JComboBox(arrayOf("Item1", "Item2", "Item3")), BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class LookAndFeelComboBox(
  lnf: ComboBoxModel<LookAndFeelInfo>,
) : JComboBox<LookAndFeelInfo>(lnf) {
  private var listener: ItemListener? = null

  override fun updateUI() {
    removeItemListener(listener)
    setRenderer(null)
    super.updateUI()
    val r = getRenderer()
    setRenderer { list, value, index, isSelected, cellHasFocus ->
      val c = r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      if (c is JLabel && value != null) {
        c.text = value.name
      }
      c
    }
    listener = ItemListener { e ->
      val item = e.item
      if (e.stateChange == ItemEvent.SELECTED && item is LookAndFeelInfo) {
        setLookAndFeel(item.className)
      }
    }
    addItemListener(listener)
  }
}

private fun setLookAndFeel(lookAndFeelName: String) {
  EventQueue.invokeLater {
    val current = UIManager.getLookAndFeel().javaClass.name
    if (current != lookAndFeelName) {
      runCatching {
        UIManager.setLookAndFeel(lookAndFeelName)
        updateLookAndFeel()
      }.onFailure {
        it.printStackTrace()
      }
    }
  }
}

private fun updateLookAndFeel() {
  for (window in Window.getWindows()) {
    SwingUtilities.updateComponentTreeUI(window)
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
