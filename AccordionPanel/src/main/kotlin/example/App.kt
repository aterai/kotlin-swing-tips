package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val accordion = Box.createVerticalBox()
    accordion.setOpaque(true)
    accordion.setBackground(Color(0xB4_B4_FF))
    accordion.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5))
    makeExpansionPanelList().forEach {
      accordion.add(it)
      accordion.add(Box.createVerticalStrut(5))
    }
    accordion.add(Box.createVerticalGlue())

    val scroll = JScrollPane(accordion)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scroll.getVerticalScrollBar().setUnitIncrement(25)

    val split = JSplitPane()
    split.setResizeWeight(.5)
    split.setDividerSize(2)
    split.setLeftComponent(scroll)
    split.setRightComponent(JLabel("Dummy"))
    add(split)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeExpansionPanelList() = listOf(
    object : AbstractExpansionPanel("System Tasks") {
      override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
        listOf("aaaa", "aaaaaaa")
          .map { title -> JCheckBox(title) }
          .forEach { check ->
            check.setOpaque(false)
            p.add(check)
          }
      }
    },
    object : AbstractExpansionPanel("Other Places") {
      override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
        listOf("Desktop", "My Network Places", "My Documents", "Shared Documents")
          .map { title -> JLabel(title) }
          .forEach { label -> p.add(label) }
      }
    },
    object : AbstractExpansionPanel("Details") {
      override fun makePanel() = JPanel(GridLayout(0, 1)).also { p ->
        val bg = ButtonGroup()
        listOf("aaa", "bbb", "ccc", "ddd")
          .map { title -> JRadioButton(title) }
          .forEach { radio ->
            radio.setSelected(p.getComponentCount() == 0)
            radio.setOpaque(false)
            p.add(radio)
            bg.add(radio)
          }
      }
    }
  )
}

abstract class AbstractExpansionPanel(private val title: String) : JPanel(BorderLayout()) {
  private val label: JLabel
  private val panel: JPanel

  abstract fun makePanel(): JPanel

  init {
    label = object : JLabel("▼ $title") {
      private val bgc = Color(0xC8_C8_FF)
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as? Graphics2D ?: return
        // Insets ins = getInsets();
        g2.setPaint(GradientPaint(50f, 0f, Color.WHITE, getWidth().toFloat(), getHeight().toFloat(), bgc))
        g2.fillRect(0, 0, getWidth(), getHeight())
        g2.dispose()
        super.paintComponent(g)
      }
    }
    label.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        initPanel()
      }
    })
    label.setForeground(Color.BLUE)
    label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 2))
    add(label, BorderLayout.NORTH)

    panel = makePanel()
    panel.setVisible(false)
    panel.setOpaque(true)
    panel.setBackground(Color(0xF0_F0_FF))
    val outBorder = BorderFactory.createMatteBorder(0, 2, 2, 2, Color.WHITE)
    val inBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    val border = BorderFactory.createCompoundBorder(outBorder, inBorder)
    panel.setBorder(border)
    add(panel)
  }

  override fun getPreferredSize(): Dimension? = label.getPreferredSize()?.also {
    if (panel.isVisible()) {
      it.height += panel.getPreferredSize().height
    }
  }

  override fun getMaximumSize() = getPreferredSize()?.also {
    it.width = Short.MAX_VALUE.toInt()
  }

  protected fun initPanel() {
    panel.setVisible(!panel.isVisible())
    val mark = if (panel.isVisible()) "△" else "▼"
    label.setText("$mark $title")
    revalidate()
    // fireExpansionEvent()
    EventQueue.invokeLater { panel.scrollRectToVisible(panel.getBounds()) }
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
