package ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public static final String SCREEN_LOGIN     = "login";
    public static final String SCREEN_DASHBOARD = "dashboard";
    public static final String SCREEN_STOCK     = "stock";
    public static final String SCREEN_SALES     = "sales";
    public static final String SCREEN_CUSTOMERS = "customers";
    public static final String SCREEN_ORDERS    = "orders";
    public static final String SCREEN_REPORTS   = "reports";
    public static final String SCREEN_REMINDERS = "reminders";
    public static final String SCREEN_PU_ORDERS = "Online Orders";
    public static final String SCREEN_USERS = "Users";
    public static final String SCREEN_DISCOUNT_PLANS = "Discount Plans" ;
    public static final String SCREEN_TEMPLATES = "Templates" ;




    public MainFrame() {
        super("IPOS-CA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1280, 720);
        setLocationRelativeTo(null);

        ScreenRouter router = new ScreenRouter(cards, root);

        root.add(new LoginPanel(router),     SCREEN_LOGIN);
        root.add(new DashboardPanel(router), SCREEN_DASHBOARD);
        root.add(new StockPanel(router),     SCREEN_STOCK);
        root.add(new SalesPanel(router),     SCREEN_SALES);
        root.add(new CustomerPanel(router),  SCREEN_CUSTOMERS);
        root.add(new OrdersPanel(router),    SCREEN_ORDERS);
        root.add(new ReportsPanel(router),          SCREEN_REPORTS);
        root.add(new PaymentRemindersPanel(router), SCREEN_REMINDERS);
        root.add(new PUOrdersPanel(router),         SCREEN_PU_ORDERS);
        root.add(new UserPanel(router),             SCREEN_USERS);
        root.add(new DiscountPlansPanel(router),    SCREEN_DISCOUNT_PLANS);
        root.add(new TemplatesPanel(router),        SCREEN_TEMPLATES);


        setContentPane(root);
        router.goTo(SCREEN_LOGIN);
    }
}