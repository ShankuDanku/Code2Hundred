import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.*;

import java.io.*;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;

class RecentSearch {
    final public static String RS_endpoint =
            "https://api.twitter.com/2/tweets/search/recent?query=";// base url for twitter recent search endpoint
    public String RS_params;
    String JSON;
    StringBuilder RS_query;
    ArrayList<Dictionary<String, String>> leads = new ArrayList<>();
    String NextToken = null;

    RecentSearch(String query) {
        StringBuilder encoded_query = new StringBuilder();

        for (int i = 0; i < query.length(); i++) {
            switch (query.charAt(i)) {
                default -> encoded_query.append(query.charAt(i));
                case ' ' -> encoded_query.append("%20");
                case ':' -> encoded_query.append("%3A");
                case '#' -> encoded_query.append("%23");
            }
        }

        this.RS_query = encoded_query;

        String time = LocalDateTime.now().minus(48, ChronoUnit.HOURS)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        this.RS_params = "&start_time=" + time + "&max_results=100" +
                "&expansions=attachments.media_keys,author_id,referenced_tweets.id" +
                "&user.fields=username&tweet.fields=text";
    }

    RecentSearch(String query, int t) {
        StringBuilder encoded_query = new StringBuilder();

        for (int i = 0; i < query.length(); i++) {
            switch (query.charAt(i)) {
                default -> encoded_query.append(query.charAt(i));
                case ' ' -> encoded_query.append("%20");
                case ':' -> encoded_query.append("%3A");
                case '#' -> encoded_query.append("%23");
            }
        }

        this.RS_query = encoded_query;

        String time = LocalDateTime.now().minus(t, ChronoUnit.HOURS)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        this.RS_params = "&start_time=" + time + "&max_results=100" +
                "&expansions=attachments.media_keys,author_id,referenced_tweets.id" +
                "&user.fields=username&tweet.fields=text&media.fields=url,preview_image_url";
    }

    void APIcall() throws IOException {
        //String bearerToken = System.getenv("BT");
        String bearerToken = "AAAAAAAAAAAAAAAAAAAAAN68OwEAAAAAymuGqJujzz6gVs9AdNWlv9uK%2Fvw%3D46jUxbTlgenpXSX0a8qzaGZTTLHSaV6hGTsNLrV9BwiR1IHK1L";

        Document html = Jsoup.connect(RecentSearch.RS_endpoint + RS_query + this.RS_params)// building url
                .header("Authorization", "Bearer " + bearerToken)// passing bearer token as header
                .ignoreContentType(true)// jsoup cant handle json
                .get();
        Element json = html.selectFirst("body");// getting json in body tags
        this.JSON = json.text();
    }

    void APIcall(String NextToken) throws IOException {
        String bearerToken = System.getenv("BT");

        Document html = Jsoup.connect(RecentSearch.RS_endpoint + RS_query + this.RS_params + "&next_token=" + NextToken)// building url
                .header("Authorization", "Bearer " + bearerToken)// passing bearer token as header
                .ignoreContentType(true).get();// jsoup cant handle json
        Element json = html.selectFirst("body");// getting json in body tags
        this.JSON = json.text();
    }

    void JSONparse() {
        JSONObject main = new JSONObject(this.JSON);
        if (main.getJSONObject("meta").getInt("result_count") != 0) {
            JSONArray tweets = main.getJSONObject("includes").getJSONArray("tweets");
            JSONArray handles = main.getJSONObject("includes").getJSONArray("users");
            JSONArray incomplete_tweets = main.getJSONArray("data");

            for (int i = 0; i < incomplete_tweets.length(); i++) {
                Dictionary<String, String> lead = new Hashtable<>();
                try {
                    if ("retweeted".equals(incomplete_tweets.getJSONObject(i).getJSONArray("referenced_tweets")
                            .getJSONObject(0).getString("type"))) {
                        for (int c = 0; c < tweets.length(); c++) {
                            if (incomplete_tweets.getJSONObject(i).getJSONArray("referenced_tweets")
                                    .getJSONObject(0).getString("id").equals(tweets.getJSONObject(c)
                                            .getString("id"))) {
                                lead.put("text", tweets.getJSONObject(c).getString("text"));
                                break;
                            }
                        }
                        String text = incomplete_tweets.getJSONObject(i).getString("text");
                        StringBuilder username = new StringBuilder();
                        for (int c = 4; c < text.length(); c++) {
                            if (text.charAt(c) == ':')
                                break;
                            else
                                username.append(text.charAt(c));
                        }
                        lead.put("handle", username.toString());
                        lead.put("link", "https://twitter.com/" + username +
                                "/status/" + incomplete_tweets.getJSONObject(i).getJSONArray("referenced_tweets")
                                .getJSONObject(0).getString("id"));
                    } else {
                        lead.put("text", incomplete_tweets.getJSONObject(i).getString("text"));
                        for (int c = 0; c < handles.length(); c++) {
                            if (incomplete_tweets.getJSONObject(i).getString("author_id").equals(handles.getJSONObject(c)
                                    .getString("id"))) {
                                lead.put("handle", handles.getJSONObject(c).getString("username"));
                                lead.put("link", "https://twitter.com/" + handles.getJSONObject(c).getString("username") +
                                        "/status/" + incomplete_tweets.getJSONObject(i).getString("id"));
                                break;
                            }
                        }

                    }
                } catch (Exception e) {
                    lead.put("text", incomplete_tweets.getJSONObject(i).getString("text"));
                    for (int c = 0; c < handles.length(); c++) {
                        if (incomplete_tweets.getJSONObject(i).getString("author_id").equals(handles.getJSONObject(c)
                                .getString("id"))) {
                            lead.put("handle", handles.getJSONObject(c).getString("username"));
                            lead.put("link", "https://twitter.com/" + handles.getJSONObject(c).getString("username") +
                                    "/status/" + incomplete_tweets.getJSONObject(i).getString("id"));
                            break;
                        }
                    }
                }
                boolean add = true;

                for (Dictionary<String, String> dictionary : leads) {
                    if (dictionary.get("text").equals(lead.get("text"))) {
                        add = false;
                        break;
                    }
                }
                if (add)
                    leads.add(lead);
            }
            try {
                NextToken = main.getJSONObject("meta").getString("next_token");
            } catch (Exception e) {
                NextToken = null;
            }
        }

    }

}

 public class GUI {
    static Color MainBackground = Color.BLACK;
    static Color Accent = new Color(29, 161, 242);
    static Color Divider = Color.darkGray;
    static Color SearchBackground = new Color(34, 48, 60);

    ImageIcon icon = new ImageIcon("icon.png");
    ImageIcon bennet = new ImageIcon("bennet.png");
    Image bennetPhoto = bennet.getImage();
    ImageIcon newbennet = new ImageIcon(bennetPhoto.getScaledInstance(100, 100, Image.SCALE_SMOOTH));
    Image photo = icon.getImage();
    ImageIcon NewIcon = new ImageIcon(photo.getScaledInstance(100, 100, Image.SCALE_SMOOTH));

    static Font DefaultFont = new Font("Segoe UI Light", Font.PLAIN, 15);

    static Image AccentCapsule = new ImageIcon("capsule.png").getImage();
    static Image WhiteCapsule = new ImageIcon("white.png").getImage();
    static Image DarkCapsule = new ImageIcon("dark.png").getImage();
    static Image SearchCapsule = new ImageIcon("SearchCapsule.png").getImage();
    static Image HomeIcon = new ImageIcon("HomeIcon.png").getImage();
    static Image DBicon = new ImageIcon("DataBaseIcon.png").getImage();
    static Image DIcon = new ImageIcon("RupeeIcon.png").getImage();

    JCheckBox beds;
    JCheckBox vaxx;
    JCheckBox o2;
    JCheckBox remdesivir;
    JCheckBox ventilator;
    JCheckBox plasma;
    JCheckBox icu;
    JCheckBox amb;
    JCheckBox verified;
    JCheckBox meals;
    JCheckBox Exclude;

    JFrame window;
    JPanel numbers;
    JTextField location;
    JTextField resource;
    JPanel checkboxes;
    JPanel output;
    JPanel MenuBar;
    JPanel info;
    JScrollPane MainScroll;
    JPanel MainOutput;
    JPanel fil;
    JPanel popup;

    JPanel Donations;
    JPanel Database;
    CardLayout MainCard = new CardLayout();
    JPanel MainStuff;

    static class TwitterCheckBox extends JCheckBox implements MouseListener {
        TwitterCheckBox(String text) {
            super(text);
            super.setOpaque(false);
            super.setForeground(Color.WHITE);
            super.setFont(new Font("Segoe UI Light", Font.PLAIN, 15));
            super.setIcon(new ImageIcon("Deselected small.png"));
            super.setSelectedIcon(new ImageIcon("selected small.png"));
            super.addMouseListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    static class TwitterMenuLabel extends JLabel implements MouseListener {
        String Card;
        JPanel CardPanel;
        CardLayout Layout;
        static int sX = 130;
        static int sY = 40;
        JLabel holder;

        TwitterMenuLabel(String text, String CardName, JPanel panel, CardLayout manager, Image img) {
            super(new ImageIcon(DarkCapsule.getScaledInstance(sX, sY, Image.SCALE_SMOOTH)));
            super.setLayout(new BorderLayout());
            holder = new JLabel(text, JLabel.CENTER);
            holder.setVerticalTextPosition(JLabel.NORTH);
            holder.setIcon(new ImageIcon(img.getScaledInstance(27, 27, Image.SCALE_SMOOTH)));
            holder.setIconTextGap(5);
            holder.setForeground(Accent);
            holder.setFont(new Font("Segoe UI Light", Font.PLAIN, 18));
            holder.addMouseListener(this);
            super.add(holder);
            Card = CardName;
            CardPanel = panel;
            Layout = manager;
        }


        @Override
        public void mouseClicked(MouseEvent e) {
            Layout.show(CardPanel, Card);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            holder.setForeground(Accent);
            super.setIcon(new ImageIcon(WhiteCapsule.getScaledInstance(sX, sY, Image.SCALE_SMOOTH)));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            holder.setForeground(Accent);
            super.setIcon(new ImageIcon(DarkCapsule.getScaledInstance(sX, sY, Image.SCALE_SMOOTH)));
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.setCursor(new Cursor(Cursor.HAND_CURSOR));
            holder.setForeground(Color.WHITE);
            super.setIcon(new ImageIcon(AccentCapsule.getScaledInstance(sX, sY, Image.SCALE_SMOOTH)));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            holder.setForeground(Accent);
            super.setIcon(new ImageIcon(DarkCapsule.getScaledInstance(sX, sY, Image.SCALE_SMOOTH)));
        }
    }

    static class TwitterCredits extends JLabel {
        TwitterCredits(String Text) {
            super(Text, JLabel.CENTER);
            super.setOpaque(false);
            super.setForeground(Accent);
            super.setFont(new Font("Segoe UI Light", Font.PLAIN, 15));
        }
    }

    static class TwitterScrollbarUI extends BasicScrollBarUI {
        Color c;

        TwitterScrollbarUI(Color clr) {
            super();
            c = clr;
        }

        @Override
        protected void configureScrollBarColors() {
            super.thumbColor = c;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton jbutton = new JButton();
            jbutton.setPreferredSize(new Dimension(0, 0));
            jbutton.setMinimumSize(new Dimension(0, 0));
            jbutton.setMaximumSize(new Dimension(0, 0));
            return jbutton;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = this.thumbColor;
            g2.setPaint(color);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);
            g2.dispose();
        }
    }

    static class TwitterHyperLinkPanel extends JPanel implements MouseListener {
        String URL;
        Color color = Accent;
        Boolean Clicked = false;
        JLabel Label;

        TwitterHyperLinkPanel(String Text, String Address) {
            super(new FlowLayout(FlowLayout.LEFT, 10, 0));
            super.setOpaque(false);
            Label = new JLabel(Text, JLabel.CENTER);
            Label.setForeground(Color.WHITE);
            Label.setFont(DefaultFont);
            Label.setOpaque(false);
            Label.setBorder(new EmptyBorder(10, 0, 10, 0));
            Label.addMouseListener(this);
            super.add(Label);
            URL = Address;

        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Rectangle r = super.getComponent(0).getBounds();
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setPaint(color);
            g2d.fillRoundRect(r.x - 10, r.y, r.width + 20, r.height, r.height - 1, r.height - 1);
            g2d.dispose();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Clicked = true;
            color = SearchBackground;
            Label.setForeground(Accent);
            super.repaint();
            try {
                Desktop.getDesktop().browse(URI.create(URL));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {
            Label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            color = Color.WHITE;
            Label.setForeground(Accent);
            super.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (!Clicked) {
                color = Accent;
                Label.setForeground(Color.WHITE);
                super.repaint();
            }
            if (Clicked) {
                color = SearchBackground;
                Label.setForeground(Accent);
                super.repaint();
            }
        }
    }

    static class TwitterTextAreaPanel extends JPanel {
        JTextArea textarea;
        Color BG = MainBackground;

        TwitterTextAreaPanel(String Text) {
            super();
            super.setOpaque(false);
            super.setLayout(new BorderLayout());
            textarea = new JTextArea(Text);
            textarea.setEditable(false);
            textarea.setForeground(Color.white);
            textarea.setFont(DefaultFont);
            textarea.setOpaque(false);
            //textarea.setBackground(MainBackground);
            textarea.setLineWrap(true);
            textarea.setWrapStyleWord(true);
            super.setBorder(new EmptyBorder(10, 10, 10, 10));
            super.add(textarea);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2d = (Graphics2D) g.create();
            Rectangle r = super.getBounds();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setPaint(Accent);
            g2d.drawRoundRect(r.x, 0, r.width - 1, r.height - 1, 50, 50);
            g2d.setPaint(BG);
            g2d.fillRoundRect(2, 2, r.width - 4, r.height - 4, 50, 50);
            super.paintComponent(g);
            g2d.dispose();

        }

        void setBG(Color c) {
            BG = c;
        }
    }

    GUI() throws FileNotFoundException {
        window = new JFrame("Leads Locator for COVID Resources from Twitter");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH);
        Dimension Size = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
        window.setSize(Size);
        window.setIconImage(icon.getImage());
        window.getContentPane().setBackground(MainBackground);

        numbers = new JPanel();
        BoxLayout box = new BoxLayout(numbers, BoxLayout.Y_AXIS);
        numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
        numbers.setBackground(MainBackground);

        JScrollPane MainNumber = new JScrollPane(numbers);
        MainNumber.getVerticalScrollBar().setUI(new TwitterScrollbarUI(Accent));
        MainNumber.getVerticalScrollBar().setBackground(MainBackground);
        MainNumber.setBorder(BorderFactory.createEmptyBorder());
        MainNumber.setPreferredSize(new Dimension(135, 0));

        output = new JPanel(new GridLayout(0, 2, 10, 30));
        output.setBackground(MainBackground);
        output.setBorder(new EmptyBorder(0, 0, 0, 10));
        MainScroll = new JScrollPane(output);
        TwitterScrollbarUI UI = new TwitterScrollbarUI(Accent);
        MainScroll.getVerticalScrollBar().setUI(UI);
        MainScroll.getVerticalScrollBar().setBackground(MainBackground);
        MainScroll.setOpaque(false);
        MainScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));

        MainOutput = new JPanel(new BorderLayout());
        MainOutput.add(BorderLayout.CENTER, MainScroll);
        MainOutput.add(BorderLayout.EAST, MainNumber);
        MainOutput.setOpaque(false);

        MainStuff = new JPanel(MainCard);
        MainStuff.setOpaque(false);
        MainStuff.add("1", MainOutput);
        MainStuff.add("2", DonationPage());

        window.getContentPane().add(BorderLayout.WEST, SearchEngine());
        window.getContentPane().add(BorderLayout.CENTER, MainStuff);
        window.getContentPane().add(BorderLayout.NORTH, MenuBar());
        popup = new JPanel();
        popup.setBackground(MainBackground.darker());
        popup.setPreferredSize(new Dimension(25, 25));
        popup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                window.getContentPane().add(BorderLayout.SOUTH, info);
                window.getContentPane().remove(popup);
                window.getContentPane().repaint();
                window.getContentPane().revalidate();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                popup.setBackground(MainBackground.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                popup.setBackground(MainBackground.darker());
            }
        });
        window.getContentPane().add(BorderLayout.SOUTH, popup);


        window.setVisible(true);
    }

    JScrollPane DonationPage() throws FileNotFoundException {
        Donations = new JPanel(new GridLayout(0, 1, 0, 50));
        Donations.setOpaque(true);
        Donations.setBackground(MainBackground);
        Donations.setBorder(new EmptyBorder(10, 10, 10, 0));
        JScrollPane MainDonation = new JScrollPane(Donations);
        MainDonation.getVerticalScrollBar().setUI(new TwitterScrollbarUI(Accent));
        MainDonation.getVerticalScrollBar().setBackground(MainBackground);
        MainDonation.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, Divider.darker()));
        JSONArray Resources = DBMS.getDonations();
        for (int i = 0; i < Resources.length(); i++) {
            JPanel info = new JPanel(new BorderLayout(0, 10));
            JTextArea text = new JTextArea(Resources.getJSONObject(i)
                    .getString("text"));
            text.setBackground(SearchBackground);
            text.setFont(DefaultFont);
            text.setForeground(Accent);
            info.setOpaque(false);
            info.add(BorderLayout.NORTH, new TwitterHyperLinkPanel(Resources.getJSONObject(i)
                    .getString("name"), Resources.getJSONObject(i).getString("link")));
            info.add(text);
            Donations.add(info);
        }
        return MainDonation;
    }

    JTextField SearchBar(String prompt) {
        JTextField tf = new JTextField(prompt);
        tf.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.WHITE));
        tf.setOpaque(false);
        tf.setFont(DefaultFont);
        tf.setCaretColor(Color.WHITE);
        tf.setForeground(Color.GRAY.darker());
        tf.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tf.setText("");
                tf.setForeground(Color.WHITE);
            }
        });
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (tf.getText().equals("")) {
                    tf.setText(prompt);
                    tf.setForeground(Color.GRAY.darker());
                }
            }
        });
        tf.addActionListener(e -> GUIbackend());
        return tf;
    }

    JLabel SearchBG(JTextField Bar) {
        int BarX = 250;
        int BarY = 50;
        JLabel LocationBar = new JLabel(new ImageIcon(SearchCapsule.getScaledInstance(BarX, BarY, Image.SCALE_SMOOTH)));
        LocationBar.setLayout(new BorderLayout());
        JPanel Filler = new JPanel();
        JPanel Filler2 = new JPanel();
        JPanel Filler3 = new JPanel();
        Filler3.setOpaque(false);
        Filler3.setPreferredSize(new Dimension(20, 0));
        Filler2.setPreferredSize(new Dimension(20, 0));
        Filler.setOpaque(false);
        Filler2.setOpaque(false);
        LocationBar.add(Bar);
        LocationBar.add(BorderLayout.SOUTH, Filler);
        LocationBar.add(BorderLayout.EAST, Filler3);
        LocationBar.add(BorderLayout.WEST, Filler2);
        return LocationBar;
    }

    JPanel SearchEngine() {
        JPanel SearchEngine = new JPanel(new BorderLayout(0, 75));
        SearchEngine.setBackground(MainBackground);
        SearchEngine.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Divider.darker()));

        JPanel ActualSearchEngine = new JPanel(new BorderLayout());
        ActualSearchEngine.setOpaque(false);
        ActualSearchEngine.setBorder(new EmptyBorder(0, 5, 0, 5));

        checkboxes = new JPanel(new GridLayout(8, 2, 10, 20));

        beds = new TwitterCheckBox("Beds");
        remdesivir = new TwitterCheckBox("Remdesivir");
        ventilator = new TwitterCheckBox("Ventilators");
        plasma = new TwitterCheckBox("Plasma");
        icu = new TwitterCheckBox("ICU");
        meals = new TwitterCheckBox("Meals");
        vaxx = new TwitterCheckBox("Vaccines");
        o2 = new TwitterCheckBox("Oxygen");
        verified = new TwitterCheckBox("Verified Only");
        amb = new TwitterCheckBox("Ambulance");
        Exclude = new TwitterCheckBox("Filter tweets");

        checkboxes.add(beds);
        checkboxes.add(o2);
        checkboxes.add(ventilator);
        checkboxes.add(plasma);
        checkboxes.add(icu);
        checkboxes.add(meals);
        checkboxes.add(vaxx);
        checkboxes.add(remdesivir);
        checkboxes.add(verified);
        checkboxes.add(amb);
        checkboxes.add(Exclude);
        checkboxes.setOpaque(false);

        JPanel SearchBars = new JPanel(new GridLayout(0, 1, 0, 50));
        SearchBars.setOpaque(false);

        resource = SearchBar("Other Resource(s)");
        location = SearchBar("Enter City/Location");

        int ImgSizeX = 180;
        int ImgSizeY = 60;
        JLabel SearchButton = new JLabel(new ImageIcon(AccentCapsule.getScaledInstance(ImgSizeX, ImgSizeY, Image.SCALE_SMOOTH)));
        SearchButton.setLayout(new BorderLayout());
        SearchButton.setBorder(new EmptyBorder(0, 0, 25, 0));
        JLabel Search = new JLabel("Search", JLabel.CENTER);
        Search.setFont(new Font("Segoe UI Black", Font.PLAIN, 15));
        Search.setForeground(Color.WHITE);
        Search.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Search.setCursor(new Cursor(Cursor.HAND_CURSOR));
                Search.setForeground(Accent);
                SearchButton.setIcon(new ImageIcon(WhiteCapsule.getScaledInstance(ImgSizeX, ImgSizeY, Image.SCALE_SMOOTH)));

            }

            @Override
            public void mouseExited(MouseEvent e) {
                Search.setForeground(Color.WHITE);
                SearchButton.setIcon(new ImageIcon(AccentCapsule.getScaledInstance(ImgSizeX, ImgSizeY, Image.SCALE_SMOOTH)));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                GUIbackend();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Search.setForeground(Accent);
                SearchButton.setIcon(new ImageIcon(DarkCapsule.getScaledInstance(ImgSizeX, ImgSizeY, Image.SCALE_SMOOTH)));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Search.setForeground(Color.WHITE);
                SearchButton.setIcon(new ImageIcon(AccentCapsule.getScaledInstance(ImgSizeX, ImgSizeY, Image.SCALE_SMOOTH)));

            }
        });

        SearchButton.add(Search);

        SearchBars.add(SearchBG(resource));
        SearchBars.add(SearchBG(location));


        ActualSearchEngine.add(BorderLayout.CENTER, checkboxes);
        ActualSearchEngine.add(BorderLayout.SOUTH, SearchBars);

        SearchEngine.add(ActualSearchEngine);
        SearchEngine.add(BorderLayout.SOUTH, SearchButton);
        return SearchEngine;
    }

    private JPanel MenuBar() {
        MenuBar = new JPanel(new FlowLayout(FlowLayout.LEADING, 25, 0));
        MenuBar.setBackground(MainBackground);
        MenuBar.setBorder(new EmptyBorder(10, 0, 10, 0));

        TwitterMenuLabel Home = new TwitterMenuLabel("Home", "1", MainStuff, MainCard, HomeIcon);
        JLabel Don = new TwitterMenuLabel("Donations", "2", MainStuff, MainCard, DIcon);

        MenuBar.add(Home);
        MenuBar.add(Don);

        fil = new JPanel(new BorderLayout());
        fil.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Divider.darker()));
        fil.add(MenuBar);
        return fil;
    }

    {
        info = new JPanel(new FlowLayout(FlowLayout.CENTER));
        info.setBackground(MainBackground.darker());
        info.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Divider.darker()));
        info.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                window.getContentPane().add(BorderLayout.SOUTH, popup);
                popup.setBackground(MainBackground.darker());
                window.getContentPane().remove(info);
                window.getContentPane().repaint();
                window.getContentPane().revalidate();
            }
        });

        JPanel C2H = new JPanel(new BorderLayout());
        C2H.setOpaque(false);
        JLabel C2hText = new JLabel("By Team Code2Hundred");
        C2hText.setFont(DefaultFont);
        C2hText.setForeground(Accent);
        C2H.setBorder(new EmptyBorder(0, 0, 0, 100));
        C2H.add(BorderLayout.CENTER, new JLabel(NewIcon));
        C2H.add(BorderLayout.NORTH, C2hText);

        JPanel creators = new JPanel(new GridLayout(0, 1));
        creators.setOpaque(false);
        TwitterCredits header = new TwitterCredits("Creators: ");
        TwitterCredits d = new TwitterCredits("Dhriti Kapoor");
        TwitterCredits m = new TwitterCredits("Moiz Anwar");
        TwitterCredits a = new TwitterCredits("Ritwik Arthur William");
        TwitterCredits r = new TwitterCredits("Riya Jain");
        TwitterCredits s = new TwitterCredits("Sashank Durbha");

        creators.setBorder(new EmptyBorder(0, 0, 0, 100));
        creators.add(header);
        creators.add(d);
        creators.add(m);
        creators.add(a);
        creators.add(r);
        creators.add(s);

        JPanel Bennett = new JPanel(new BorderLayout());
        Bennett.setOpaque(false);
        JLabel stud = new JLabel("Powered By:", JLabel.CENTER);
        stud.setFont(DefaultFont);
        stud.setForeground(Accent);
        Bennett.add(BorderLayout.NORTH, stud);
        Bennett.add(BorderLayout.CENTER, new JLabel(newbennet));

        info.add(C2H);
        info.add(creators);
        info.add(Bennett);
    }// credits

    void GUIbackend() {
        String Manual = "https://twitter.com/search?q=";
        StringBuilder query = new StringBuilder();
        StringBuilder or = new StringBuilder();
        ArrayList<String> OR = new ArrayList<>();
        String[] L = new String[0];
        if (!location.getText().equals("Enter City/Location") && !location.getText().equals(""))
            L = location.getText().split(",");
        if (L.length == 1)
            query.append("(").append(L[0]).append(") ");
        if (L.length > 1) {
            query.append("( ");
            query.append(L[0]);
            for (int x = 1; x < L.length; x++) {
                query.append(" OR ").append(L[x]);
            }
            query.append(" ) ");
        }
        if (Exclude.isSelected())
            query.append("-needed -urgent -need -bot ");
        if (!resource.getText().equals("Other Resource(s)") && !resource.getText().equals("")) {
            String[] R = resource.getText().split(",");
            Collections.addAll(OR, R);
        }
        if (beds.isSelected())
            OR.add("Beds OR Bed");
        if (plasma.isSelected())
            OR.add("plasma");
        if (vaxx.isSelected())
            OR.add("vaccine OR vaccines");
        if (o2.isSelected())
            OR.add("oxygen");
        if (meals.isSelected())
            OR.add("meal OR meals OR food");
        if (ventilator.isSelected())
            OR.add("ventilator OR ventilators");
        if (amb.isSelected())
            OR.add("ambulance OR ambulances");
        if (icu.isSelected())
            OR.add("ICU");
        if (remdesivir.isSelected())
            OR.add("remdesivir OR remidesivir OR remdesivir");
        if (verified.isSelected())
            query.append("(verified OR #verified) ");
        if (OR.size() == 1)
            query.append("(").append(OR.get(0)).append(")");
        if (OR.size() > 1) {
            or.append("(");
            or.append(OR.get(0));
            for (int x = 1; x < OR.size(); x++) {
                or.append(" OR ").append(OR.get(x));
            }
            or.append(")");
        }
        query.append(or);
        query = new StringBuilder(query.toString().strip());
        System.out.println(query);
        if (!query.toString().equals("")) {
            RecentSearch crll = new RecentSearch(query.toString());
            try {
                crll.APIcall();
                crll.JSONparse();
            } catch (IOException ioException) {
                //ioException.printStackTrace();
            } catch (Exception e) {
                System.out.println(e);
            }
            int i = 0;
            while (crll.NextToken != null && i < 4) {
                i++;
                try {
                    crll.APIcall(crll.NextToken);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                crll.JSONparse();
            }
            //System.out.println(crll.JSON);
            Manual+=crll.RS_query+"&f=live";
            TwitterHyperLinkPanel btn = new TwitterHyperLinkPanel("Search Manually",Manual);
            try{
                MainOutput.remove(2);
            }
            catch (Exception ignored){

            }
            MainOutput.add(BorderLayout.NORTH,btn);
            MainOutput.revalidate();MainOutput.repaint();
            ArrayList<Dictionary<String, String>> leads = crll.leads;
            output.removeAll();
            numbers.removeAll();
            JLabel numbertitle = new JLabel("Number(s)", JLabel.CENTER);
            numbertitle.setFont(DefaultFont);
            numbertitle.setForeground(Accent);
            //numbers.add(numbertitle);
            MainCard.show(MainStuff, "1");
            int pos = 0;
            if (leads != null) {
                output.setLayout(new GridLayout(0, 2, 10, 10));
                for (Dictionary<String, String> lead : leads) {
                    JPanel out = new JPanel(new BorderLayout(0, 10));
                    out.setBackground(MainBackground);
                    String text = lead.get("text");
                    TwitterTextAreaPanel t = new TwitterTextAreaPanel(text);
                    TwitterHyperLinkPanel tt = new TwitterHyperLinkPanel("@" + lead.get("handle"), lead.get("link"));
                    out.add(BorderLayout.NORTH, tt);

                    JPanel head = new JPanel();
                    head.setOpaque(false);
                    text = text.replaceAll("\\+91", "");
                    text = text.replaceAll("-", "");
                    text = text.replace(" ", "");
                    ArrayList<String> NumberList = new ArrayList<>();
                    StringBuilder num = new StringBuilder();
                    boolean digit = false;
                    for (int c = 0; c < text.length(); c++) {
                        if (Character.isDigit(text.charAt(c))) {
                            num.append(text.charAt(c));
                            digit = true;
                        }
                        if ((!Character.isDigit(text.charAt(c)) || c == text.length() - 1) && digit) {
                            digit = false;
                            if (num.length() == 10 || num.length() == 11) {
                                NumberList.add(num.toString());
                            }
                            num = new StringBuilder();

                        }
                    }
                    for (String s : NumberList) {
                        JLabel numberr = new JLabel(s, JLabel.CENTER);
                        JLabel Numberbg = new JLabel(new ImageIcon(DarkCapsule.getScaledInstance(100, 40, Image.SCALE_SMOOTH)));
                        Numberbg.setLayout(new BorderLayout());
                        numberr.setFont(DefaultFont);
                        numberr.setForeground(Accent);
                        Numberbg.add(numberr);
                        head.add(Numberbg);
                        JLabel numberbg = new JLabel(new ImageIcon(DarkCapsule.getScaledInstance(100, 40, Image.SCALE_SMOOTH)));
                        numberbg.setBorder(new EmptyBorder(10, 10, 0, 0));
                        numberbg.setLayout(new BorderLayout());
                        JLabel number = new JLabel(s, JLabel.CENTER);
                        number.setForeground(Accent);
                        number.setBackground(MainBackground);
                        number.setOpaque(false);
                        number.setFont(DefaultFont);
                        int finalPos = pos;
                        number.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                number.setForeground(Color.WHITE);
                                numberbg.setIcon(new ImageIcon(AccentCapsule.getScaledInstance(100, 40, Image.SCALE_SMOOTH)));
                                numberbg.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                t.setBG(SearchBackground);
                                t.repaint();
                                tt.Label.setForeground(Accent);
                                tt.setColor(Color.WHITE);
                                tt.repaint();
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                                number.setForeground(Accent);
                                numberbg.setIcon(new ImageIcon(DarkCapsule.getScaledInstance(100, 40, Image.SCALE_SMOOTH)));
                                t.setBG(MainBackground);
                                t.repaint();
                                tt.Label.setForeground(Color.WHITE);
                                tt.setColor(Accent);
                                tt.repaint();
                            }

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                MainScroll.getVerticalScrollBar().setValue((finalPos / 2)
                                        * ((MainScroll.getVerticalScrollBar().getMaximum()) / (leads.size() / 2 + (leads.size() % 2))));
                                t.setBG(SearchBackground);
                                t.repaint();
                                tt.Label.setForeground(Accent);
                                tt.setColor(Color.WHITE);
                                tt.repaint();
                            }
                        });
                        numberbg.add(number);
                        numbers.add(numberbg);
                    }
                    JPanel fillerpanel = new JPanel();
                    fillerpanel.setPreferredSize(new Dimension(100, 40));
                    fillerpanel.setOpaque(false);
                    if (NumberList.isEmpty()) {
                        head.add(fillerpanel);
                    }
                    t.add(BorderLayout.SOUTH, head);
                    out.add(BorderLayout.CENTER, t);

                    output.add(out);
                    pos++;
                }
            }
            assert leads != null;
            if (leads.isEmpty()) {
                JLabel na = new JLabel("N/A", JLabel.CENTER);
                na.setForeground(Accent);
                output.setLayout(new BorderLayout());
                output.add(BorderLayout.CENTER, na);
            }
        }
        numbers.revalidate();
        numbers.repaint();
        output.revalidate();
        output.repaint();
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("sun.java2d.uiScale.enabled", "false");
        new GUI();

    }

}

class DBMS {
    static String DatabaseLocation = "DataBase.txt";
    static String DonationLocation = "DonationLink.json";

    static ArrayList<String> read() throws FileNotFoundException {
        File DB = new File(DatabaseLocation);
        ArrayList<String> data = new ArrayList<>();
        Scanner line = new Scanner(DB);
        while (line.hasNextLine()) {
            data.add(line.nextLine());
        }
        line.close();
        return data;
    }

    static void update(String text) throws IOException {
        FileWriter writer = new FileWriter("DataBase.txt", true);
        writer.write(text);
        writer.close();
    }

    static JSONArray getDonations() throws FileNotFoundException {
        File DL = new File(DonationLocation);
        StringBuilder text = new StringBuilder();
        Scanner line = new Scanner(DL);
        while (line.hasNextLine()) {
            text.append(line.nextLine());
        }
        return new JSONArray(text.toString());
    }

    public static void main(String[] args) throws IOException {
        System.out.println(DBMS.getDonations());
    }

}

