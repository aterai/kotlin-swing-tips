package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(BorderLayout(5, 5)).also { panel ->
  val selectedLabelShift = "TabbedPane.selectedLabelShift"
  val labelShift = "TabbedPane.labelShift"

  val log = JTextArea()
  val d = UIManager.getLookAndFeelDefaults()
  log.append("$selectedLabelShift: ${d[selectedLabelShift]}\n")
  log.append("$labelShift: ${d[labelShift]}\n")

  val sls = UIManager.getLookAndFeelDefaults().getInt(selectedLabelShift)
  val slsModel = SpinnerNumberModel(sls, -5, 5, 1)
  slsModel.addChangeListener { e ->
    (e.source as? SpinnerNumberModel)?.also {
      UIManager.put(selectedLabelShift, it.number.toInt())
      SwingUtilities.updateComponentTreeUI(panel.topLevelAncestor)
    }
  }

  val ls = UIManager.getLookAndFeelDefaults().getInt(labelShift)
  val lsModel = SpinnerNumberModel(ls, -5, 5, 1)
  lsModel.addChangeListener { e ->
    (e.source as? SpinnerNumberModel)?.also {
      UIManager.put(labelShift, it.number.toInt())
      SwingUtilities.updateComponentTreeUI(panel.topLevelAncestor)
    }
  }

  val title1 = """UIManager.put("$selectedLabelShift", offset)"""
  val box1 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createTitledBorder(title1)
    it.add(JLabel("offset = "))
    it.add(JSpinner(slsModel))
    it.add(Box.createHorizontalGlue())
  }

  val title2 = """UIManager.put("$labelShift", offset)"""
  val box2 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createTitledBorder(title2)
    it.add(JLabel("offset = "))
    it.add(JSpinner(lsModel))
    it.add(Box.createHorizontalGlue())
  }

  val p = JPanel(GridLayout(2, 1)).also {
    it.add(box1)
    it.add(box2)
  }

  val tabbedPane = JTabbedPane().also {
    it.addTab("title 0", ColorIcon(Color.RED), JScrollPane(log))
    it.addTab("title 1", ColorIcon(Color.GREEN), JButton("button"))
    it.addTab("title 2", ColorIcon(Color.BLUE), JLabel("label"))
    it.addTab("title 3", JPanel())
    it.setTabComponentAt(3, JLabel("label", ColorIcon(Color.ORANGE), SwingConstants.LEFT))
  }

  panel.add(p, BorderLayout.NORTH)
  panel.add(tabbedPane)
  panel.preferredSize = Dimension(320, 240)
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
