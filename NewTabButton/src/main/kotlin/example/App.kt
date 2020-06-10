package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.Serializable
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  UIManager.put("example.TabButton", "TabViewButtonUI")
  UIManager.put("TabViewButtonUI", "example.OperaTabViewButtonUI")
  val tab3 = CardLayoutTabbedPane()
  tab3.border = BorderFactory.createTitledBorder("CardLayout+JRadioButton(opera like)")
  tab3.addTab("9999", JScrollPane(JTree()))
  tab3.addTab("11111111111111", JLabel("666666"))
  tab3.addTab("222222", JLabel("555555555"))
  tab3.addTab("333", JButton("4444"))
  return JPanel(BorderLayout()).also {
    it.add(tab3)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CardLayoutTabbedPane : JPanel(BorderLayout()) {
  private val cardLayout = CardLayout()
  private val tabPanel = JPanel(TabLayout())
  private val wrapPanel = JPanel(BorderLayout())
  private val contentsPanel = JPanel(cardLayout)
  private val bg = ButtonGroup()
  private val button = JButton(PlusIcon())

  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  private val icons = listOf(
    ImageIcon(javaClass.getResource("wi0009-16.png")),
    ImageIcon(javaClass.getResource("wi0054-16.png")),
    ImageIcon(javaClass.getResource("wi0062-16.png")),
    ImageIcon(javaClass.getResource("wi0063-16.png")),
    ImageIcon(javaClass.getResource("wi0064-16.png")),
    ImageIcon(javaClass.getResource("wi0096-16.png")),
    ImageIcon(javaClass.getResource("wi0111-16.png")),
    ImageIcon(javaClass.getResource("wi0122-16.png")),
    ImageIcon(javaClass.getResource("wi0124-16.png")),
    ImageIcon(javaClass.getResource("wi0126-16.png"))
  )

  init {
    val left = 0
    val right = 0
    tabPanel.border = BorderFactory.createMatteBorder(0, left, 0, right, Color(20, 30, 50))
    contentsPanel.border = BorderFactory.createEmptyBorder(4, left, 2, right)

    tabPanel.isOpaque = true
    tabPanel.background = Color(20, 30, 50)

    wrapPanel.isOpaque = true
    wrapPanel.background = Color(20, 30, 50)

    // contentsPanel.setOpaque(true);
    // contentsPanel.setBackground(new Color(20, 30, 50));

    wrapPanel.add(tabPanel)
    // TEST: wrapPanel.add(new JButton("a"), BorderLayout.WEST);

    // JPanel locPanel = new JPanel();
    // wrapPanel.add(new JButton("b"), BorderLayout.SOUTH);

    add(wrapPanel, BorderLayout.NORTH)
    add(contentsPanel)

    button.border = BorderFactory.createEmptyBorder()
    button.addActionListener(object : ActionListener {
      private var count = 0
      override fun actionPerformed(e: ActionEvent) {
        addTab("new tab:$count", JLabel("xxx:$count"))
        count++
      }
    })
  }

  private fun createTabComponent(title: String, comp: Component): Component {
    val tab = TabButton(title)
    tab.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        (e.component as? AbstractButton)?.isSelected = true
        cardLayout.show(contentsPanel, title)
      }
    })
    tab.icon = icons.random()
    tab.layout = BorderLayout()
    val close = object : JButton(CloseTabIcon(Color.GRAY)) {
      override fun getPreferredSize() = Dimension(12, 12)
    }
    close.addActionListener {
      tabPanel.remove(tab)
      contentsPanel.remove(comp)
      val isMoreThanOne = tabPanel.componentCount > 1
      if (isMoreThanOne) {
        tabPanel.revalidate()
        (tabPanel.getComponent(0) as? TabButton)?.isSelected = true
        cardLayout.first(contentsPanel)
      }
      tabPanel.revalidate()
    }
    close.border = BorderFactory.createEmptyBorder()
    close.isFocusPainted = false
    close.isContentAreaFilled = false
    close.pressedIcon = CloseTabIcon(Color.BLACK)
    close.rolloverIcon = CloseTabIcon(Color.ORANGE)

    val p = JPanel(BorderLayout())
    p.isOpaque = false
    p.add(close, BorderLayout.NORTH)
    tab.add(p, BorderLayout.EAST)
    bg.add(tab)
    tab.isSelected = true
    return tab
  }

  fun addTab(title: String, comp: Component) {
    tabPanel.remove(button)
    tabPanel.add(createTabComponent(title, comp))
    tabPanel.add(button)
    tabPanel.revalidate()
    contentsPanel.add(comp, title)
    cardLayout.show(contentsPanel, title)
  }
}

private class TabLayout : LayoutManager, Serializable {
  override fun addLayoutComponent(name: String, comp: Component) { /* not needed */
  }

  override fun removeLayoutComponent(comp: Component) { /* not needed */
  }

  override fun preferredLayoutSize(parent: Container): Dimension {
    synchronized(parent.treeLock) {
      val last = parent.componentCount - 1
      var w = 0
      var h = 0
      if (last >= 0) {
        val comp = parent.getComponent(last)
        val d = comp.preferredSize
        w = d.width
        h = d.height
      }
      val i = parent.insets
      return Dimension(i.left + i.right + w, i.top + i.bottom + h)
    }
  }

  override fun minimumLayoutSize(parent: Container): Dimension {
    synchronized(parent.treeLock) {
      return Dimension(100, 24)
    }
  }

  override fun layoutContainer(parent: Container) {
    synchronized(parent.treeLock) {
      val componentCount = parent.componentCount
      if (componentCount == 0) {
        return
      }
      // val numRows = 1
      // val ltr = parent.componentOrientation.isLeftToRight
      val insets = parent.insets
      val numCols = componentCount - 1
      val lastWidth = parent.getComponent(componentCount - 1).preferredSize.width
      val width = parent.width - insets.left - insets.right - lastWidth
      val h = parent.height - insets.top - insets.bottom
      val w = if (width > TAB_WIDTH * numCols) TAB_WIDTH else width / numCols
      var gap = width - w * numCols
      var x = insets.left
      val y = insets.top
      for (i in 0 until componentCount) {
        val cw = if (i == numCols) lastWidth else w + if (gap-- > 0) 1 else 0
        parent.getComponent(i).setBounds(x, y, cw, h)
        x += cw
      }
    }
  }

  override fun toString() = "$javaClass.getName(): JTabbedPane tab layout"

  companion object {
    private const val serialVersionUID = 1L
    private const val TAB_WIDTH = 100
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
